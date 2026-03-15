package com.kphtracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@PluginDescriptor(
    name = "KPH Tracker",
    description = "Tracks kills per hour for any NPC you fight",
    tags = {"kills", "kph", "tracker", "npc", "combat"}
)
public class KphTrackerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private KphTrackerConfig config;

    @Inject
    private KphTrackerOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private KphTrackerPanel panel;

    private NavigationButton navButton;

    // Rolling window of kill timestamps (used to calculate KPH)
    private final Deque<Instant> killTimestamps = new ArrayDeque<>();

    private int totalKills = 0;
    private String trackedNpcName = null;
    private Instant sessionStart = null;

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        sessionStart = Instant.now();

        // Register the side panel in RuneLite's left toolbar
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
        reset();
        trackedNpcName = null;
        log.debug("KPH Tracker stopped");
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * Fires whenever the local player changes their attack target.
     * Used to auto-detect which NPC the player is fighting.
     */
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
        String npcName = npc.getName();
        if (npcName == null)
        {
            return;
        }

        if (config.autoDetect() && !npcName.equals(trackedNpcName))
        {
            if (config.resetOnNpcChange())
            {
                reset();
            }
            trackedNpcName = npcName;
            log.debug("Now tracking NPC: {}", trackedNpcName);
        }
    }

    /**
     * Fires when an NPC despawns. Records a kill if it died and matches
     * the NPC we're tracking.
     */
    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();

        if (!npc.isDead())
        {
            return;
        }

        String npcName = npc.getName();
        if (npcName == null)
        {
            return;
        }

        boolean shouldCount = false;

        if (config.autoDetect())
        {
            shouldCount = npcName.equals(trackedNpcName);
        }
        else
        {
            String configName = config.npcName().trim();
            if (configName.isEmpty() || npcName.equalsIgnoreCase(configName))
            {
                shouldCount = true;
                trackedNpcName = npcName;
            }
        }

        if (shouldCount)
        {
            recordKill();
        }
    }

    // -------------------------------------------------------------------------
    // Kill tracking logic
    // -------------------------------------------------------------------------

    /**
     * Stamps a kill and trims the rolling window to the last 60 minutes.
     */
    private void recordKill()
    {
        Instant now = Instant.now();
        killTimestamps.addLast(now);
        totalKills++;

        Instant cutoff = now.minusSeconds(3600);
        while (!killTimestamps.isEmpty() && killTimestamps.peekFirst().isBefore(cutoff))
        {
            killTimestamps.pollFirst();
        }

        log.debug("Kill recorded. Total: {} | Window: {}", totalKills, killTimestamps.size());
    }

    /**
     * Resets kill counter and session timer. Preserves the tracked NPC name.
     * Called from both the panel reset button and internally on NPC switch.
     */
    public void reset()
    {
        killTimestamps.clear();
        totalKills = 0;
        sessionStart = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Getters (used by overlay and panel)
    // -------------------------------------------------------------------------

    /**
     * Calculates kills per hour using a rolling 1-hour window.
     */
    public double getKillsPerHour()
    {
        if (killTimestamps.isEmpty() || sessionStart == null)
        {
            return 0.0;
        }

        Instant firstKill = killTimestamps.peekFirst();
        double elapsedSeconds = java.time.Duration.between(firstKill, Instant.now()).getSeconds();

        if (elapsedSeconds < 1)
        {
            return 0.0;
        }

        return (killTimestamps.size() / elapsedSeconds) * 3600.0;
    }

    public int getTotalKills()
    {
        return totalKills;
    }

    public String getTrackedNpcName()
    {
        return trackedNpcName;
    }

    public Instant getSessionStart()
    {
        return sessionStart;
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    /**
     * Generates a simple 16x16 "K" icon for the sidebar nav button.
     * No external image file needed.
     */
    private BufferedImage buildIcon()
    {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 200, 0));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("K", 2, 13);
        g.dispose();
        return img;
    }

    @Provides
    KphTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(KphTrackerConfig.class);
    }
}
