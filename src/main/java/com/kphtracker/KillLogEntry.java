package com.kphtracker;

import java.time.Instant;

/**
 * Immutable record of a single NPC kill.
 */
class KillLogEntry
{
    final String npcName;
    final Instant time;

    KillLogEntry(String npcName, Instant time)
    {
        this.npcName = npcName;
        this.time = time;
    }
}
