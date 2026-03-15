# KPH Tracker Plugin — Setup Guide

## What It Does

Displays a live overlay showing:
- The NPC you're currently killing
- Kills per hour (rolling 1-hour window)
- Total kills this session
- Session elapsed time

It auto-detects the NPC you're attacking, so no manual setup needed.

---

## Prerequisites

Install these before you start:

1. **Java JDK 11** — https://adoptium.net (download Temurin 11)
2. **IntelliJ IDEA Community** (free) — https://www.jetbrains.com/idea/download
3. **Git** — https://git-scm.com

---

## Step 1: Clone the RuneLite External Plugin Template

The easiest way to develop external plugins is using the official starter template.

```bash
git clone https://github.com/runelite/example-plugin.git kph-tracker
cd kph-tracker
```

This gives you a working plugin project you can build on top of.

---

## Step 2: Replace the Source Files

Delete everything inside `src/main/java/` and replace it with the three files provided:

```
src/main/java/com/kphtracker/
    KphTrackerPlugin.java
    KphTrackerConfig.java
    KphTrackerOverlay.java
```

Also replace `build.gradle` with the one provided.

---

## Step 3: Open in IntelliJ

1. Open IntelliJ IDEA
2. Choose **File > Open** and select the `kph-tracker` folder
3. IntelliJ will auto-detect the Gradle project — click **Trust Project** if prompted
4. Wait for Gradle to sync and download dependencies (takes 1-2 minutes first time)

---

## Step 4: Run the Plugin in RuneLite

RuneLite provides a development launcher that lets you test plugins live.

1. In IntelliJ, open the **Gradle** panel (right side of screen)
2. Navigate to: `Tasks > runelite > runClient`
   — OR just search for `runClient` in the Gradle tasks
3. Double-click `runClient` — this launches RuneLite with your plugin loaded
4. Log in to OSRS normally
5. Go to the plugin list (puzzle piece icon), search **KPH Tracker**, and enable it

> **Note:** The first run will download RuneLite's dependencies (~200MB). This is normal.

---

## Step 5: Using the Plugin In-Game

- Start attacking any NPC — the overlay will auto-detect it
- KPH calculates after your first kill and updates in real time
- Right-click the overlay to move it anywhere on screen

### Config Options (Plugin Settings Panel)

| Setting | Default | Description |
|---|---|---|
| Auto Detect NPC | On | Tracks whichever NPC you last attacked |
| NPC Name (Manual) | blank | Lock tracking to a specific NPC name |
| Reset on NPC Change | Off | Resets counter when you switch targets |
| Show Session Time | On | Shows how long you've been grinding |
| Show Total Kills | On | Shows total kills this session |

---

## File Structure Reference

```
kph-tracker/
├── build.gradle
└── src/
    └── main/
        └── java/
            └── com/kphtracker/
                ├── KphTrackerPlugin.java   ← main logic, kill detection
                ├── KphTrackerConfig.java   ← settings/options
                ├── KphTrackerOverlay.java  ← on-screen HUD display
                └── KphTrackerPanel.java    ← side panel with reset button
```

## Side Panel

Click the yellow **K** icon in RuneLite's left sidebar to open the panel. It shows the same stats as the overlay (NPC, KPH, total kills, session time) and includes a **Reset Session** button that clears the kill counter and restarts the session timer.

The panel refreshes automatically every second. You can have both the overlay and the panel open at the same time.

---

## How Kill Detection Works

The plugin listens for two game events:

**`onInteractingChanged`** — fires when you click an NPC to attack it. Records which NPC you're targeting for auto-detect.

**`onNpcDespawned`** — fires when an NPC disappears. If `isDead()` returns true and the NPC name matches what you're tracking, it counts as a kill.

KPH is calculated as a rolling window: kills in the last hour ÷ elapsed seconds × 3600. This gives an accurate real-time rate rather than just counting from session start.

---

## Common Issues

**Plugin doesn't appear in RuneLite:**
Make sure the `@PluginDescriptor` annotation is in your main plugin class and you ran `runClient` via Gradle, not just the regular RuneLite launcher.

**KPH shows 0:**
Wait until at least one kill registers. KPH can't calculate until the first kill sets a baseline time.

**NPC name shows "—":**
You haven't attacked anything yet this session, or Auto Detect is off and no NPC name is configured.

**Build fails with "cannot find symbol":**
Gradle sync may not have completed. Try **File > Sync Project with Gradle Files** in IntelliJ.
