package com.kphtracker;

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
    private static final Color COLOR_GOLD  = new Color(255, 215, 0);
    private static final Color COLOR_MUTED = new Color(150, 150, 150);

    private final KphTrackerPlugin plugin;
    private final KphTrackerConfig config;
    private final PanelComponent   panelComponent = new PanelComponent();

    @Inject
    private KphTrackerOverlay(KphTrackerPlugin plugin, KphTrackerConfig config)
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

        String npcName  = plugin.getTrackedNpcName();
        double kph      = plugin.getKillsPerHour();
        Color  kphColor = plugin.getKphColor();

        if (config.compactOverlay())
        {
            renderCompact(npcName, kph, kphColor);
        }
        else
        {
            renderFull(npcName, kph, kphColor);
        }

        panelComponent.setPreferredSize(new Dimension(config.compactOverlay() ? 140 : 170, 0));
        return panelComponent.render(graphics);
    }

    private void renderCompact(String npcName, double kph, Color kphColor)
    {
        String label = (npcName != null ? npcName : "-")
            + "  |  "
            + (kph > 0 ? String.format("%.1f", kph) : "-")
            + " kph";

        panelComponent.getChildren().add(TitleComponent.builder()
            .text(label)
            .color(kph > 0 ? kphColor : COLOR_MUTED)
            .build());
    }

    private void renderFull(String npcName, double kph, Color kphColor)
    {
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("KPH Tracker")
            .color(Color.YELLOW)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("NPC")
            .right(npcName != null ? npcName : "-")
            .rightColor(npcName != null ? Color.WHITE : COLOR_MUTED)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("KPH")
            .right(kph > 0 ? String.format("%.1f", kph) : "-")
            .rightColor(kph > 0 ? kphColor : COLOR_MUTED)
            .build());

        if (config.showPeakOnOverlay())
        {
            double peak = plugin.getPeakKph();
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Peak")
                .right(peak > 0 ? String.format("%.1f", peak) : "-")
                .rightColor(COLOR_GOLD)
                .build());
        }

        if (config.showTotalKills())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Kills")
                .right(String.valueOf(plugin.getTotalKills()))
                .build());
        }

        if (config.showSessionTime() && plugin.getSessionStart() != null)
        {
            Duration elapsed = Duration.between(plugin.getSessionStart(), Instant.now());
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Time")
                .right(KphTrackerPlugin.formatDuration(elapsed))
                .build());
        }
    }
}
