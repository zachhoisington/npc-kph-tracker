package com.npckphtracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("npcKphTracker")
public interface NpcKphTrackerConfig extends Config
{
    @ConfigItem(
        keyName = "showOverlay",
        name = "Show Overlay",
        description = "Display the KPH tracking overlay"
    )
    default boolean showOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "autoTrackSlayerTask",
        name = "Auto-track Slayer Task",
        description = "Automatically start tracking your current slayer task"
    )
    default boolean autoTrackSlayerTask()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showSlayerInfo",
        name = "Show Slayer Info",
        description = "Display slayer task information in overlay"
    )
    default boolean showSlayerInfo()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showTimeEstimate",
        name = "Show Time Estimate",
        description = "Display estimated time to complete slayer task"
    )
    default boolean showTimeEstimate()
    {
        return true;
    }

    @ConfigItem(
        keyName = "autoTrackLastKilled",
        name = "Auto-track Last Killed",
        description = "Automatically start tracking the last NPC type you killed (when no slayer task)"
    )
    default boolean autoTrackLastKilled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "dataRetentionHours",
        name = "Data Retention (Hours)",
        description = "How long to keep kill data (hours)"
    )
    @Range(min = 1, max = 168)
    default int dataRetentionHours()
    {
        return 24;
    }

    @ConfigItem(
        keyName = "showTotalKph",
        name = "Show Total KPH",
        description = "Display overall kills per hour since tracking started"
    )
    default boolean showTotalKph()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showRecentKph",
        name = "Show Recent KPH",
        description = "Display recent kills per hour"
    )
    default boolean showRecentKph()
    {
        return true;
    }

    @ConfigItem(
        keyName = "recentTimeMinutes",
        name = "Recent Time Window (Minutes)",
        description = "Time window for recent KPH calculation"
    )
    @Range(min = 5, max = 120)
    default int recentTimeMinutes()
    {
        return 15;
    }

    @ConfigItem(
        keyName = "showKillCount",
        name = "Show Kill Count",
        description = "Display total kill count"
    )
    default boolean showKillCount()
    {
        return true;
    }

    @ConfigItem(
        keyName = "overlayPosition",
        name = "Overlay Position",
        description = "Position of the overlay on screen"
    )
    default OverlayPosition overlayPosition()
    {
        return OverlayPosition.TOP_LEFT;
    }

    enum OverlayPosition
    {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}