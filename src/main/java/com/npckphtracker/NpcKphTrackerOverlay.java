package com.npckphtracker;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;

public class NpcKphTrackerOverlay extends OverlayPanel
{
    private static final DecimalFormat KPH_FORMAT = new DecimalFormat("#.#");
    private static final DecimalFormat GP_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat GP_DECIMAL_FORMAT = new DecimalFormat("#,###.#");
    private static final Color PANEL_BACKGROUND_COLOR = new Color(16, 20, 25, 200);
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = Color.LIGHT_GRAY;
    private static final Color HIGHLIGHT_COLOR = Color.YELLOW;

    private final Client client;
    private final NpcKphTrackerPlugin plugin;
    private final NpcKphTrackerConfig config;

    @Inject
    private NpcKphTrackerOverlay(Client client, NpcKphTrackerPlugin plugin, NpcKphTrackerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(mapConfigPosition());
        setResizable(false);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay() || !plugin.isTracking())
        {
            return null;
        }

        String trackedNpc = plugin.getCurrentTrackedNpc();
        if (trackedNpc == null)
        {
            return null;
        }

        NpcTrackingData data = plugin.getTrackingData(trackedNpc);
        if (data == null || data.getKillCount() == 0)
        {
            return null;
        }

        // Update overlay position based on config
        setPosition(mapConfigPosition());

        panelComponent.setBackgroundColor(PANEL_BACKGROUND_COLOR);
        panelComponent.setBorder(new Rectangle(1, 1, 1, 1));

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("NPC KPH Tracker")
                .color(TITLE_COLOR)
                .build());
// Slayer task information
        SlayerTaskData slayerTask = plugin.getCurrentSlayerTask();
        boolean isSlayerTask = slayerTask != null && slayerTask.getTaskName().equalsIgnoreCase(trackedNpc);
        // Current NPC being tracked
        String displayName = trackedNpc;
        if (isSlayerTask)
        {
            displayName = trackedNpc + " (Slayer)";
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Tracking:")
                .right(displayName)
                .leftColor(TEXT_COLOR)
                .rightColor(isSlayerTask ? Color.MAGENTA : HIGHLIGHT_COLOR)
                .build());



        if (config.showSlayerInfo() && isSlayerTask)
        {
            // Task progress
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Task Progress:")
                    .right(slayerTask.getCompleted() + "/" + slayerTask.getOriginalAmount())
                    .leftColor(TEXT_COLOR)
                    .rightColor(Color.CYAN)
                    .build());

            // Remaining
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Remaining:")
                    .right(String.valueOf(slayerTask.getRemaining()))
                    .leftColor(TEXT_COLOR)
                    .rightColor(Color.ORANGE)
                    .build());

            // Progress percentage
            double progress = slayerTask.getProgressPercentage();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Progress:")
                    .right(String.format("%.1f%%", progress))
                    .leftColor(TEXT_COLOR)
                    .rightColor(getProgressColor(progress))
                    .build());
        }

        // Kill count
        if (config.showKillCount())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Kills:")
                    .right(String.valueOf(data.getKillCount()))
                    .leftColor(TEXT_COLOR)
                    .rightColor(Color.WHITE)
                    .build());
        }

        // Total KPH
        if (config.showTotalKph())
        {
            double totalKph = data.getKillsPerHour();
            String kphText = totalKph > 0 ? KPH_FORMAT.format(totalKph) : "0";

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Total KPH:")
                    .right(kphText)
                    .leftColor(TEXT_COLOR)
                    .rightColor(getKphColor(totalKph))
                    .build());
        }

        // Recent KPH
        if (config.showRecentKph())
        {
            double recentKph = data.getRecentKillsPerHour(config.recentTimeMinutes());
            String recentKphText = recentKph > 0 ? KPH_FORMAT.format(recentKph) : "0";

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Recent KPH (" + config.recentTimeMinutes() + "m):")
                    .right(recentKphText)
                    .leftColor(TEXT_COLOR)
                    .rightColor(getKphColor(recentKph))
                    .build());
        }

        // Time estimate for slayer task
        if (config.showTimeEstimate() && isSlayerTask)
        {
            String timeEstimate = plugin.getEstimatedTimeRemaining();
            if (timeEstimate != null)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Est. Time:")
                        .right(timeEstimate)
                        .leftColor(TEXT_COLOR)
                        .rightColor(Color.GREEN)
                        .build());
            }
        }

        // GP Tracking
        if (config.showGpTracking())
        {
            // Total GP gained
            long totalGp = data.getTotalGpGained();
            if (totalGp > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Total GP:")
                        .right(formatGp(totalGp))
                        .leftColor(TEXT_COLOR)
                        .rightColor(Color.YELLOW)
                        .build());
            }

            // Average GP per kill
            if (config.showAvgGpPerKill())
            {
                double avgGpPerKill = data.getAverageGpPerKill();
                if (avgGpPerKill > 0)
                {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Avg GP/Kill:")
                            .right(formatGp((long)avgGpPerKill))
                            .leftColor(TEXT_COLOR)
                            .rightColor(Color.GREEN)
                            .build());
                }
            }

            // GP per hour
            if (config.showGpPerHour())
            {
                double gpPerHour = data.getGpPerHour();
                if (gpPerHour > 0)
                {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("GP/Hour:")
                            .right(formatGp((long)gpPerHour))
                            .leftColor(TEXT_COLOR)
                            .rightColor(getGpPerHourColor(gpPerHour))
                            .build());
                }
            }
        }

        // Session time
        if (data.getFirstKill() != null)
        {
            long sessionMinutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    data.getFirstKill(),
                    java.time.Instant.now()
            );
            String sessionTime = formatTime(sessionMinutes);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Session:")
                    .right(sessionTime)
                    .leftColor(TEXT_COLOR)
                    .rightColor(Color.CYAN)
                    .build());
        }

        return super.render(graphics);
    }

    private net.runelite.client.ui.overlay.OverlayPosition mapConfigPosition()
    {
        switch (config.overlayPosition())
        {
            case TOP_LEFT:     return OverlayPosition.TOP_LEFT;
            case TOP_RIGHT:    return OverlayPosition.TOP_RIGHT;
            case BOTTOM_LEFT:  return OverlayPosition.BOTTOM_LEFT;
            case BOTTOM_RIGHT: return OverlayPosition.BOTTOM_RIGHT;
            default:           return OverlayPosition.TOP_LEFT;
        }
    }

    private Color getKphColor(double kph)
    {
        if (kph >= 100)
        {
            return Color.GREEN;
        }
        else if (kph >= 50)
        {
            return Color.YELLOW;
        }
        else if (kph > 0)
        {
            return Color.ORANGE;
        }
        else
        {
            return Color.RED;
        }
    }

    private Color getProgressColor(double progress)
    {
        if (progress >= 75)
        {
            return Color.GREEN;
        }
        else if (progress >= 50)
        {
            return Color.YELLOW;
        }
        else if (progress >= 25)
        {
            return Color.ORANGE;
        }
        else
        {
            return Color.RED;
        }
    }

    private Color getGpPerHourColor(double gpPerHour)
    {
        if (gpPerHour >= 2000000) // 2M+ GP/hr
        {
            return Color.GREEN;
        }
        else if (gpPerHour >= 1000000) // 1M+ GP/hr
        {
            return Color.YELLOW;
        }
        else if (gpPerHour >= 500000) // 500K+ GP/hr
        {
            return Color.ORANGE;
        }
        else if (gpPerHour > 0)
        {
            return Color.WHITE;
        }
        else
        {
            return Color.RED;
        }
    }

    private String formatGp(long gp)
    {
        if (gp >= 1000000)
        {
            return GP_DECIMAL_FORMAT.format(gp / 1000000.0) + "M";
        }
        else if (gp >= 1000)
        {
            return GP_DECIMAL_FORMAT.format(gp / 1000.0) + "K";
        }
        else
        {
            return GP_FORMAT.format(gp);
        }
    }

    private String formatTime(long minutes)
    {
        if (minutes < 60)
        {
            return minutes + "m";
        }
        else
        {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + "h " + remainingMinutes + "m";
        }
    }
}
