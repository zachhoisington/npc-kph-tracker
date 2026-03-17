package com.kphtracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@PluginDescriptor(
    name = "KPH Tracker",
    description = "Tracks kills per hour for any NPC you fight",
    tags = {"kills", "kph", "tracker", "npc", "combat"}
)
public class KphTrackerPlugin extends Plugin
{
    // -------------------------------------------------------------------------
    // Injected dependencies
    // -------------------------------------------------------------------------

    @Inject private Client client;
    @Inject private KphTrackerConfig config;
    @Inject private KphTrackerOverlay overlay;
    @Inject private KphTrackerPanel panel;
    @Inject private OverlayManager overlayManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private Notifier notifier;

    private NavigationButton navButton;

    // -------------------------------------------------------------------------
    // State
    // All fields read/written across the client thread and Swing EDT must be
    // volatile or backed by thread-safe collections.
    // -------------------------------------------------------------------------

    /** Rolling 1-hour window of kill timestamps for KPH calculation. */
    private final ConcurrentLinkedDeque<Instant> killTimestamps = new ConcurrentLinkedDeque<>();

    /** Recent kills, newest first, capped at MAX_KILL_LOG_SIZE. */
    private final ConcurrentLinkedDeque<KillLogEntry> killLog = new ConcurrentLinkedDeque<>();

    private static final int MAX_KILL_LOG_SIZE = 25;

    private volatile int     totalKills     = 0;
    private volatile String  trackedNpcName = null;
    private volatile Instant sessionStart   = null;
    private volatile double  peakKph        = 0.0;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        sessionStart = Instant.now();

        navButton = NavigationButton.builder()
            .tooltip("KPH Tracker")
            .icon(buildIcon())
            .priority(7)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
        log.debug("KPH Tracker started");
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navButton);
        panel.shutdown();
        hardReset();
        log.debug("KPH Tracker stopped");
    }

    // -------------------------------------------------------------------------
    // Game event handlers  (run on client thread)
    // -------------------------------------------------------------------------

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (event.getSource() != client.getLocalPlayer())
        {
            return;
        }

        Actor target = event.getTarget();
        if (!(target instanceof NPC))
        {
            return;
        }

        NPC npc = (NPC) target;
        String name = npc.getName();
        if (name == null || name.equals(trackedNpcName))
        {
            return;
        }

        if (config.autoDetect())
        {
            if (config.resetOnNpcChange())
            {
                reset();
            }
            trackedNpcName = name;
            log.debug("Now tracking: {}", name);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();

        if (!npc.isDead())
        {
            return;
        }

        String name = npc.getName();
        if (name == null)
        {
            return;
        }

        boolean count = false;

        if (config.autoDetect())
        {
            count = name.equals(trackedNpcName);
        }
        else
        {
            String cfgName = config.npcName().trim();
            if (cfgName.isEmpty() || name.equalsIgnoreCase(cfgName))
            {
                count = true;
                trackedNpcName = name;
            }
        }

        if (count)
        {
            recordKill(name);
        }
    }

    // -------------------------------------------------------------------------
    // Kill tracking  (client thread)
    // -------------------------------------------------------------------------

    private void recordKill(String npcName)
    {
        Instant now = Instant.now();

        // Rolling 1-hour window
        killTimestamps.addLast(now);
        Instant cutoff = now.minusSeconds(3600);
        // Safe removal: pollFirst returns null if empty, and we put back if it was not old
        Instant head;
        while ((head = killTimestamps.peekFirst()) != null && head.isBefore(cutoff))
        {
            // peekFirst + pollFirst on ConcurrentLinkedDeque: another thread could remove
            // the head between these two calls, but pollFirst returning null is safe here
            // because we only care about evicting old entries, not about exact count.
            Instant removed = killTimestamps.pollFirst();
            if (removed == null)
            {
                break; // deque was emptied by another thread
            }
        }

        // Kill log (newest first), capped
        killLog.addFirst(new KillLogEntry(npcName, now));
        while (killLog.size() > MAX_KILL_LOG_SIZE)
        {
            killLog.pollLast();
        }

        totalKills++;
        log.debug("Kill recorded — total: {}  window: {}", totalKills, killTimestamps.size());
    }

    // -------------------------------------------------------------------------
    // Public API  (called from panel / overlay — may run on EDT)
    // -------------------------------------------------------------------------

    /**
     * Soft reset: clears kills, log, and peak but keeps the tracked NPC name.
     * Called by the Reset button.
     */
    public void reset()
    {
        killTimestamps.clear();
        killLog.clear();
        totalKills = 0;
        peakKph    = 0.0;
        sessionStart = Instant.now();
    }

    private void hardReset()
    {
        reset();
        trackedNpcName = null;
    }

    /**
     * Checks whether current KPH is a new session record and notifies if so.
     * Call periodically (e.g., from the panel refresh timer).
     * Safe to call from the EDT — only touches volatile fields.
     */
    public void checkAndUpdatePeak()
    {
        double kph = getKillsPerHour();
        if (kph <= 0)
        {
            return;
        }

        if (kph > peakKph)
        {
            // Notify only when the improvement is meaningful (5 KPH threshold)
            // and there was a previous baseline (avoids triggering on the very first kill)
            boolean notify = config.notifyOnPb()
                && peakKph > 10.0
                && kph > peakKph + 5.0;
            peakKph = kph;
            if (notify)
            {
                String name = trackedNpcName != null ? trackedNpcName : "NPC";
                notifier.notify(String.format("New KPH record at %s: %.1f kills/hr!", name, kph));
            }
        }
    }

    /**
     * Copies a formatted stats snapshot to the system clipboard.
     */
    public void copyStatsToClipboard()
    {
        String npc  = trackedNpcName != null ? trackedNpcName : "Unknown";
        String time = sessionStart != null
            ? formatDuration(Duration.between(sessionStart, Instant.now()))
            : "—";

        String text = String.format(
            "NPC: %s%nKPH: %.1f%nPeak KPH: %.1f%nTotal Kills: %d%nSession: %s",
            npc, getKillsPerHour(), peakKph, totalKills, time
        );

        try
        {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
            log.debug("Stats copied to clipboard");
        }
        catch (Exception e)
        {
            log.warn("Clipboard copy failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // Calculations
    // -------------------------------------------------------------------------

    /**
     * Kills per hour over a rolling 1-hour window.
     * Thread-safe: only reads from ConcurrentLinkedDeque.
     */
    public double getKillsPerHour()
    {
        Instant first = killTimestamps.peekFirst();
        if (first == null)
        {
            return 0.0;
        }

        double elapsedMs = Duration.between(first, Instant.now()).toMillis();
        if (elapsedMs < 1000.0)
        {
            return 0.0;
        }

        return (killTimestamps.size() / (elapsedMs / 1000.0)) * 3600.0;
    }

    /**
     * Returns a color representing how current KPH compares to the session peak.
     *   >= 90% of peak  → green
     *   >= 60% of peak  → yellow
     *   <  60% of peak  → orange
     *   no data         → gray
     */
    public Color getKphColor()
    {
        double kph = getKillsPerHour();
        if (kph <= 0)        return Color.GRAY;
        if (peakKph <= 0)    return new Color(100, 220, 100);
        double ratio = kph / peakKph;
        if (ratio >= 0.9)    return new Color(100, 220, 100);
        if (ratio >= 0.6)    return new Color(255, 200, 0);
        return new Color(255, 130, 0);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int                                  getTotalKills()     { return totalKills; }
    public String                               getTrackedNpcName() { return trackedNpcName; }
    public Instant                              getSessionStart()   { return sessionStart; }
    public double                               getPeakKph()        { return peakKph; }
    public ConcurrentLinkedDeque<KillLogEntry>  getKillLog()        { return killLog; }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public static String formatDuration(Duration d)
    {
        long h = d.toHours(), m = d.toMinutesPart(), s = d.toSecondsPart();
        return h > 0
            ? String.format("%d:%02d:%02d", h, m, s)
            : String.format("%d:%02d", m, s);
    }

    private BufferedImage buildIcon()
    {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 200, 0));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString("K", 2, 13);
        }
        finally
        {
            g.dispose();
        }
        return img;
    }

    @Provides
    KphTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(KphTrackerConfig.class);
    }
}
