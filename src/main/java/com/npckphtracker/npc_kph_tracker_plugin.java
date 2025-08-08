package com.npckphtracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@PluginDescriptor(
    name = "NPC KPH Tracker",
    description = "Tracks kills per hour for all NPCs",
    tags = {"combat", "slayer", "tracking", "kph", "boss"}
)
public class NpcKphTrackerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private NpcKphTrackerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private NpcKphTrackerOverlay overlay;

    @Inject
    private ItemManager itemManager;

    private final Map<String, NpcTrackingData> npcTrackingMap = new ConcurrentHashMap<>();
    private String currentTrackedNpc = null;
    private boolean isTracking = false;
    private SlayerTaskData currentSlayerTask = null;
    private int previousInventoryValue = 0;
    private boolean trackingInventoryValue = false;

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        // Initialize inventory tracking
        if (client.getLocalPlayer() != null)
        {
            previousInventoryValue = calculateInventoryValue();
            trackingInventoryValue = true;
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
    {
        // Track inventory changes to calculate GP gains
        if (itemContainerChanged.getContainerId() == InventoryID.INVENTORY.getId() && trackingInventoryValue)
        {
            int currentInventoryValue = calculateInventoryValue();
            int gpGain = currentInventoryValue - previousInventoryValue;
            
            // Only track positive gains above a threshold to avoid noise
            if (gpGain > 0 && isTracking && currentTrackedNpc != null)
            {
                NpcTrackingData data = npcTrackingMap.get(currentTrackedNpc);
                if (data != null)
                {
                    data.addGpGain(gpGain);
                }
            }
            
            previousInventoryValue = currentInventoryValue;
        }
    }

    private int calculateInventoryValue()
    {
        if (client.getLocalPlayer() == null)
        {
            return 0;
        }

        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory == null)
        {
            return 0;
        }

        int totalValue = 0;
        for (Item item : inventory.getItems())
        {
            if (item.getId() > 0)
            {
                int itemPrice = itemManager.getItemPrice(item.getId());
                totalValue += itemPrice * item.getQuantity();
            }
        }
        
        return totalValue;
    }
    public void onVarbitChanged(VarbitChanged varbitChanged)
    {
        // Check for slayer task changes
        if (varbitChanged.getVarbits().containsKey(VarPlayer.SLAYER_TASK_SIZE) ||
            varbitChanged.getVarbits().containsKey(VarPlayer.SLAYER_TASK_CREATURE))
        {
            updateSlayerTask();
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath actorDeath)
    {
        if (actorDeath.getActor() instanceof NPC)
        {
            NPC npc = (NPC) actorDeath.getActor();
            
            // Skip if NPC is null or has no name
            if (npc.getName() == null)
            {
                return;
            }

            // Track all NPCs regardless of type

            // Check if this kill is for current slayer task
            if (currentSlayerTask != null && isSlayerTaskNpc(npc.getName()))
            {
                currentSlayerTask.decrementRemaining();
            }

            // Check if player was in combat with this NPC
            if (wasPlayerInCombatWith(npc))
            {
                trackNpcKill(npc.getName());
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        // Clean up old data periodically
        cleanupOldData();
    }

    private void trackNpcKill(String npcName)
    {
        Instant now = Instant.now();
        
        NpcTrackingData data = npcTrackingMap.computeIfAbsent(npcName, k -> new NpcTrackingData());
        data.addKill(now);
        
        // Set as current tracked NPC if auto-tracking is enabled
        if (config.autoTrackLastKilled())
        {
            currentTrackedNpc = npcName;
            isTracking = true;
        }
    }

    private void updateSlayerTask()
    {
        int taskSize = client.getVarpValue(VarPlayer.SLAYER_TASK_SIZE);
        int taskCreature = client.getVarpValue(VarPlayer.SLAYER_TASK_CREATURE);
        
        if (taskSize > 0 && taskCreature > 0)
        {
            String taskName = getSlayerTaskName(taskCreature);
            if (taskName != null)
            {
                if (currentSlayerTask == null || !currentSlayerTask.getTaskName().equals(taskName))
                {
                    // New task detected
                    currentSlayerTask = new SlayerTaskData(taskName, taskSize, taskSize);
                    
                    // Auto-track slayer task if enabled
                    if (config.autoTrackSlayerTask())
                    {
                        currentTrackedNpc = taskName;
                        isTracking = true;
                    }
                }
                else
                {
                    // Update existing task
                    currentSlayerTask.setRemaining(taskSize);
                }
            }
        }
        else
        {
            // No active task
            currentSlayerTask = null;
        }
    }

    private String getSlayerTaskName(int taskCreature)
    {
        // Map creature IDs to names - this is a simplified version
        // In practice, you'd want a more comprehensive mapping
        switch (taskCreature)
        {
            case 1: return "Crawling Hands";
            case 2: return "Cave bugs";
            case 3: return "Cave crawlers";
            case 4: return "Banshees";
            case 5: return "Cave slimes";
            case 6: return "Rock slugs";
            case 7: return "Desert lizards";
            case 8: return "Cockatrices";
            case 9: return "Pyrefiends";
            case 10: return "Mogres";
            case 11: return "Harpie bug swarms";
            case 12: return "Wall beasts";
            case 13: return "Killerwatts";
            case 14: return "Molanisks";
            case 15: return "Basilisks";
            case 16: return "Sea snakes";
            case 17: return "Turoth";
            case 18: return "Fever spiders";
            case 19: return "Infernal mages";
            case 20: return "Brine rats";
            case 21: return "Bloodvelds";
            case 22: return "Jellies";
            case 23: return "Spiritual rangers";
            case 24: return "Spiritual warriors";
            case 25: return "Dust devils";
            case 26: return "Aberrant spectres";
            case 27: return "Spiritual mages";
            case 28: return "Kurasks";
            case 29: return "Skeletal wyverns";
            case 30: return "Gargoyles";
            case 31: return "Nechryaels";
            case 32: return "Abyssal demons";
            case 33: return "Cave krakens";
            case 34: return "Dark beasts";
            case 35: return "Smoke devils";
            case 36: return "Drakes";
            case 37: return "Wyrms";
            case 38: return "Hydras";
            // Add more mappings as needed
            default: return "Unknown Task";
        }
    }

    private boolean isSlayerTaskNpc(String npcName)
    {
        if (currentSlayerTask == null || npcName == null)
        {
            return false;
        }
        
        String taskName = currentSlayerTask.getTaskName().toLowerCase();
        String npcLower = npcName.toLowerCase();
        
        // Simple name matching - could be improved with more sophisticated logic
        return npcLower.contains(taskName.toLowerCase()) || 
               taskName.contains(npcLower) ||
               isAlternativeTaskName(taskName, npcLower);
    }

    private boolean isAlternativeTaskName(String taskName, String npcName)
    {
        // Handle alternative names for slayer tasks
        Map<String, String[]> alternatives = Map.of(
            "bloodvelds", new String[]{"bloodveld", "mutated bloodveld"},
            "gargoyles", new String[]{"gargoyle", "grotesque guardians"},
            "abyssal demons", new String[]{"abyssal demon", "greater abyssal demon"},
            "dust devils", new String[]{"dust devil", "choke devil"},
            "nechryaels", new String[]{"nechryael", "greater nechryael"},
            "cave krakens", new String[]{"cave kraken", "kraken"},
            "smoke devils", new String[]{"smoke devil", "thermonuclear smoke devil"},
            "drakes", new String[]{"drake"},
            "wyrms", new String[]{"wyrm"},
            "hydras", new String[]{"hydra", "alchemical hydra"}
        );

        String[] alts = alternatives.get(taskName.toLowerCase());
        if (alts != null)
        {
            for (String alt : alts)
            {
                if (npcName.contains(alt))
                {
                    return true;
                }
            }
        }
        
        return false;
    }

    private boolean wasPlayerInCombatWith(NPC npc)
    {
        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return false;
        }

        // Check if the NPC was interacting with the player
        Actor npcInteracting = npc.getInteracting();
        if (npcInteracting == player)
        {
            return true;
        }

        // Check if player was interacting with the NPC
        Actor playerInteracting = player.getInteracting();
        return playerInteracting == npc;
    }

    private void cleanupOldData()
    {
        if (client.getTickCount() % 100 != 0) // Only run every 100 ticks
        {
            return;
        }

        Instant cutoff = Instant.now().minus(config.dataRetentionHours(), ChronoUnit.HOURS);
        
        npcTrackingMap.values().forEach(data -> data.removeOldKills(cutoff));
        npcTrackingMap.entrySet().removeIf(entry -> entry.getValue().getKillCount() == 0);
    }

    public void resetTracking()
    {
        if (currentTrackedNpc != null)
        {
            NpcTrackingData data = npcTrackingMap.get(currentTrackedNpc);
            if (data != null)
            {
                data.reset();
            }
        }
    }

    public void resetAllTracking()
    {
        npcTrackingMap.clear();
        currentTrackedNpc = null;
        isTracking = false;
    }

    public void setTrackedNpc(String npcName)
    {
        currentTrackedNpc = npcName;
        isTracking = npcName != null;
    }

    public String getCurrentTrackedNpc()
    {
        return currentTrackedNpc;
    }

    public boolean isTracking()
    {
        return isTracking && currentTrackedNpc != null;
    }

    public NpcTrackingData getTrackingData(String npcName)
    {
        return npcTrackingMap.get(npcName);
    }

    public SlayerTaskData getCurrentSlayerTask()
    {
        return currentSlayerTask;
    }

    public String getEstimatedTimeRemaining()
    {
        if (currentSlayerTask == null || !isTracking())
        {
            return null;
        }

        NpcTrackingData data = getTrackingData(currentTrackedNpc);
        if (data == null)
        {
            return null;
        }

        double kph = config.useRecentKphForEstimate() ? 
            data.getRecentKillsPerHour(config.recentTimeMinutes()) : 
            data.getKillsPerHour();

        if (kph <= 0)
        {
            return null;
        }

        int remaining = currentSlayerTask.getRemaining();
        double hoursRemaining = remaining / kph;
        
        return formatEstimatedTime(hoursRemaining);
    }

    private String formatEstimatedTime(double hours)
    {
        if (hours < 1)
        {
            int minutes = (int) Math.round(hours * 60);
            return minutes + "m";
        }
        else
        {
            int wholeHours = (int) hours;
            int minutes = (int) Math.round((hours - wholeHours) * 60);
            return wholeHours + "h " + minutes + "m";
        }
    }

    public Map<String, NpcTrackingData> getAllTrackingData()
    {
        return new HashMap<>(npcTrackingMap);
    }

    @Provides
    NpcKphTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NpcKphTrackerConfig.class);
    }
}

// Slayer task data class
class SlayerTaskData
{
    private final String taskName;
    private final int originalAmount;
    private int remaining;

    public SlayerTaskData(String taskName, int originalAmount, int remaining)
    {
        this.taskName = taskName;
        this.originalAmount = originalAmount;
        this.remaining = remaining;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public int getOriginalAmount()
    {
        return originalAmount;
    }

    public int getRemaining()
    {
        return remaining;
    }

    public void setRemaining(int remaining)
    {
        this.remaining = remaining;
    }

    public void decrementRemaining()
    {
        if (remaining > 0)
        {
            remaining--;
        }
    }

    public int getCompleted()
    {
        return originalAmount - remaining;
    }

    public double getProgressPercentage()
    {
        if (originalAmount == 0)
        {
            return 0.0;
        }
        return ((double) getCompleted() / originalAmount) * 100.0;
    }
}

// Supporting classes

class NpcTrackingData
{
    private final Map<Instant, Integer> killTimestamps = new ConcurrentHashMap<>();
    private int totalKills = 0;
    private long totalGpGained = 0;
    private Instant firstKill;
    private Instant lastKill;

    public void addKill(Instant timestamp)
    {
        killTimestamps.merge(timestamp.truncatedTo(ChronoUnit.MINUTES), 1, Integer::sum);
        totalKills++;
        
        if (firstKill == null || timestamp.isBefore(firstKill))
        {
            firstKill = timestamp;
        }
        
        if (lastKill == null || timestamp.isAfter(lastKill))
        {
            lastKill = timestamp;
        }
    }

    public void addGpGain(int gpAmount)
    {
        totalGpGained += gpAmount;
    }

    public void removeOldKills(Instant cutoff)
    {
        killTimestamps.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));
        recalculateTotals();
    }

    private void recalculateTotals()
    {
        totalKills = killTimestamps.values().stream().mapToInt(Integer::intValue).sum();
        
        if (killTimestamps.isEmpty())
        {
            firstKill = null;
            lastKill = null;
        }
        else
        {
            firstKill = killTimestamps.keySet().stream().min(Instant::compareTo).orElse(null);
            lastKill = killTimestamps.keySet().stream().max(Instant::compareTo).orElse(null);
        }
    }

    public double getKillsPerHour()
    {
        if (totalKills == 0 || firstKill == null || lastKill == null)
        {
            return 0.0;
        }

        long minutes = ChronoUnit.MINUTES.between(firstKill, lastKill);
        if (minutes == 0)
        {
            return 0.0;
        }

        return (totalKills / (minutes / 60.0));
    }

    public double getRecentKillsPerHour(int minutes)
    {
        Instant cutoff = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        int recentKills = killTimestamps.entrySet().stream()
            .filter(entry -> entry.getKey().isAfter(cutoff))
            .mapToInt(Map.Entry::getValue)
            .sum();

        if (recentKills == 0)
        {
            return 0.0;
        }

        return (recentKills / (minutes / 60.0));
    }

    public double getGpPerHour()
    {
        if (totalGpGained == 0 || firstKill == null || lastKill == null)
        {
            return 0.0;
        }

        long minutes = ChronoUnit.MINUTES.between(firstKill, lastKill);
        if (minutes == 0)
        {
            return 0.0;
        }

        return (totalGpGained / (minutes / 60.0));
    }

    public double getAverageGpPerKill()
    {
        if (totalKills == 0)
        {
            return 0.0;
        }
        
        return (double) totalGpGained / totalKills;
    }

    public long getTotalGpGained()
    {
        return totalGpGained;
    }

    public int getKillCount()
    {
        return totalKills;
    }

    public Instant getFirstKill()
    {
        return firstKill;
    }

    public Instant getLastKill()
    {
        return lastKill;
    }

    public void reset()
    {
        killTimestamps.clear();
        totalKills = 0;
        totalGpGained = 0;
        firstKill = null;
        lastKill = null;
    }
}