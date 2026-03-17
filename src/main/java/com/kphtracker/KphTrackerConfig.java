package com.kphtracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("kphtracker")
public interface KphTrackerConfig extends Config
{
    // -------------------------------------------------------------------------
    // Sections
    // -------------------------------------------------------------------------

    @ConfigSection(
        name = "Tracking",
        description = "What to track and how",
        position = 0
    )
    String trackingSection = "tracking";

    @ConfigSection(
        name = "Overlay",
        description = "On-screen HUD options",
        position = 1
    )
    String overlaySection = "overlay";

    @ConfigSection(
        name = "Panel",
        description = "Side panel options",
        position = 2
    )
    String panelSection = "panel";

    @ConfigSection(
        name = "Notifications",
        description = "Alert settings",
        position = 3
    )
    String notifSection = "notif";

    // -------------------------------------------------------------------------
    // Tracking
    // -------------------------------------------------------------------------

    @ConfigItem(
        keyName = "autoDetect",
        name = "Auto Detect NPC",
        description = "Automatically track whichever NPC you last attacked.",
        section = trackingSection,
        position = 0
    )
    default boolean autoDetect()
    {
        return true;
    }

    @ConfigItem(
        keyName = "npcName",
        name = "NPC Name (Manual)",
        description = "Name of the NPC to track when Auto Detect is off. Leave blank to count all NPC kills.",
        section = trackingSection,
        position = 1
    )
    default String npcName()
    {
        return "";
    }

    @ConfigItem(
        keyName = "resetOnNpcChange",
        name = "Reset on NPC Change",
        description = "Reset the kill counter when you switch to a different NPC.",
        section = trackingSection,
        position = 2
    )
    default boolean resetOnNpcChange()
    {
        return false;
    }

    // -------------------------------------------------------------------------
    // Overlay
    // -------------------------------------------------------------------------

    @ConfigItem(
        keyName = "compactOverlay",
        name = "Compact Overlay",
        description = "Show a minimal overlay with just NPC name and KPH.",
        section = overlaySection,
        position = 0
    )
    default boolean compactOverlay()
    {
        return false;
    }

    @ConfigItem(
        keyName = "showTotalKills",
        name = "Show Total Kills",
        description = "Display total session kills on the overlay.",
        section = overlaySection,
        position = 1
    )
    default boolean showTotalKills()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showSessionTime",
        name = "Show Session Time",
        description = "Display elapsed session time on the overlay.",
        section = overlaySection,
        position = 2
    )
    default boolean showSessionTime()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showPeakOnOverlay",
        name = "Show Peak KPH",
        description = "Display session-best KPH on the overlay.",
        section = overlaySection,
        position = 3
    )
    default boolean showPeakOnOverlay()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Panel
    // -------------------------------------------------------------------------

    @ConfigItem(
        keyName = "showKillLog",
        name = "Show Kill Log",
        description = "Show a timestamped list of recent kills in the side panel.",
        section = panelSection,
        position = 0
    )
    default boolean showKillLog()
    {
        return true;
    }

    @ConfigItem(
        keyName = "killLogSize",
        name = "Kill Log Size",
        description = "Number of recent kills to display in the panel (5–20).",
        section = panelSection,
        position = 1
    )
    @Range(min = 5, max = 20)
    default int killLogSize()
    {
        return 10;
    }

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------

    @ConfigItem(
        keyName = "notifyOnPb",
        name = "Notify on Personal Best",
        description = "Send a desktop notification when you set a new session KPH record.",
        section = notifSection,
        position = 0
    )
    default boolean notifyOnPb()
    {
        return true;
    }
}
