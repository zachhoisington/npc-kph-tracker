package com.example;

import com.npckphtracker.NpcKphTrackerPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NpcKphTrackerTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(NpcKphTrackerPlugin.class);
        RuneLite.main(args);
    }
}