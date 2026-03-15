package com.kphtracker;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;

public class KphTrackerOverlay extends Overlay
{
    private final KphTrackerPlugin plugin;
    private final KphTrackerConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private KphTrackerOverlay(Client client, KphTrackerPlugin plugin, KphTrackerConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        // Title bar
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("KPH Tracker")
            .color(Color.YELLOW)
            .build());

        // NPC being tracked
        String npcName = plugin.getTrackedNpcName();
        panelComponent.getChildren().add(LineComponent.builder()
            .left("NPC")
            .right(npcName != null ? npcName : "—")
            .rightColor(npcName != null ? Color.WHITE : Color.GRAY)
            .build());

        // Kills per hour
        double kph = plugin.getKillsPerHour();
        panelComponent.getChildren().add(LineComponent.builder()
            .left("KPH")
            .right(kph > 0 ? String.format("%.1f", kph) : "—")
            .rightColor(kph > 0 ? Color.GREEN : Color.GRAY)
            .build());

        // Total kills this session
        if (config.showTotalKills())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Kills")
                .right(String.valueOf(plugin.getTotalKills()))
                .build());
        }

        // Session elapsed time
        if (config.showSessionTime() && plugin.getSessionStart() != null)
        {
            Duration elapsed = Duration.between(plugin.getSessionStart(), Instant.now());
            String timeStr = formatDuration(elapsed);
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Time")
                .right(timeStr)
                .build());
        }

        panelComponent.setPreferredSize(new Dimension(160, 0));
        return panelComponent.render(graphics);
    }

    /**
     * Formats a Duration as h:mm:ss or m:ss depending on length.
     */
    private String formatDuration(Duration d)
    {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();

        if (hours > 0)
        {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        else
        {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}
