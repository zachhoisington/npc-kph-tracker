package com.kphtracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("kphtracker")
public interface KphTrackerConfig extends Config
{
    @ConfigItem(
        keyName = "autoDetect",
        name = "Auto Detect NPC",
        description = "Automatically track the NPC you're currently attacking. Disable to specify an NPC name manually.",
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
        position = 1
    )
    default String npcName()
    {
        return "";
    }

    @ConfigItem(
        keyName = "resetOnNpcChange",
        name = "Reset on NPC Change",
        description = "Reset the kill counter when you start attacking a different NPC.",
        position = 2
    )
    default boolean resetOnNpcChange()
    {
        return false;
    }

    @ConfigItem(
        keyName = "showSessionTime",
        name = "Show Session Time",
        description = "Display how long the current session has been running on the overlay.",
        position = 3
    )
    default boolean showSessionTime()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showTotalKills",
        name = "Show Total Kills",
        description = "Display total kills this session on the overlay.",
        position = 4
    )
    default boolean showTotalKills()
    {
        return true;
    }
}
