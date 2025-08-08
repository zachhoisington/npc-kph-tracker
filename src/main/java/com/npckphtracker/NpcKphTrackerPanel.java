package com.npckphtracker;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Map;

public class NpcKphTrackerPanel extends PluginPanel
{
    private static final DecimalFormat KPH_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat GP_FORMAT = new DecimalFormat("#,###");

    private final NpcKphTrackerPlugin plugin;
    private final NpcKphTrackerConfig config;

    private JPanel contentPanel;
    private JComboBox<String> npcSelector;
    private JLabel statusLabel;
    private JLabel currentNpcLabel;
    private JLabel totalKillsLabel;
    private JLabel totalKphLabel;
    private JLabel recentKphLabel;
    private JLabel sessionTimeLabel;
    private JLabel slayerTaskLabel;
    private JLabel taskProgressLabel;
    private JLabel timeEstimateLabel;
    private JLabel totalGpLabel;
    private JLabel avgGpPerKillLabel;
    private JLabel gpPerHourLabel;

    public NpcKphTrackerPanel(NpcKphTrackerPlugin plugin, NpcKphTrackerConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        init();
    }

    private void init()
    {
        JLabel title = new JLabel("NPC KPH Tracker");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(16f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel selectorPanel = new JPanel(new BorderLayout());
        selectorPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        selectorPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel selectorLabel = new JLabel("Track NPC:");
        selectorLabel.setForeground(Color.WHITE);

        npcSelector = new JComboBox<>();
        npcSelector.setPreferredSize(new Dimension(0, 25));
        npcSelector.addActionListener(new NpcSelectorListener());

        selectorPanel.add(selectorLabel, BorderLayout.NORTH);
        selectorPanel.add(npcSelector, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton resetButton = new JButton("Reset Current");
        resetButton.addActionListener(e -> {
            plugin.resetTracking();
            updatePanel();
        });

        JButton resetAllButton = new JButton("Reset All");
        resetAllButton.addActionListener(e -> {
            plugin.resetAllTracking();
            updatePanel();
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(resetAllButton);

        controlPanel.add(selectorPanel, BorderLayout.NORTH);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel infoPanel = createInfoPanel();

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentPanel.add(controlPanel, BorderLayout.NORTH);
        contentPanel.add(infoPanel, BorderLayout.CENTER);

        add(title, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        updatePanel();
    }

    private JPanel createInfoPanel()
    {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        infoPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        statusLabel = new JLabel("Status: Not tracking");
        statusLabel.setForeground(Color.ORANGE);
        infoPanel.add(statusLabel);

        infoPanel.add(Box.createVerticalStrut(10));

        currentNpcLabel = new JLabel("Current NPC: None");
        currentNpcLabel.setForeground(Color.WHITE);
        infoPanel.add(currentNpcLabel);

        totalKillsLabel = new JLabel("Total Kills: 0");
        totalKillsLabel.setForeground(Color.WHITE);
        infoPanel.add(totalKillsLabel);

        totalKphLabel = new JLabel("Total KPH: 0");
        totalKphLabel.setForeground(Color.WHITE);
        infoPanel.add(totalKphLabel);

        recentKphLabel = new JLabel("Recent KPH: 0");
        recentKphLabel.setForeground(Color.WHITE);
        infoPanel.add(recentKphLabel);

        sessionTimeLabel = new JLabel("Session Time: 0m");
        sessionTimeLabel.setForeground(Color.WHITE);
        infoPanel.add(sessionTimeLabel);

        infoPanel.add(Box.createVerticalStrut(10));

        slayerTaskLabel = new JLabel("Slayer Task: None");
        slayerTaskLabel.setForeground(Color.MAGENTA);
        infoPanel.add(slayerTaskLabel);

        taskProgressLabel = new JLabel("Progress: N/A");
        taskProgressLabel.setForeground(Color.CYAN);
        infoPanel.add(taskProgressLabel);

        timeEstimateLabel = new JLabel("Est. Time: N/A");
        timeEstimateLabel.setForeground(Color.GREEN);
        infoPanel.add(timeEstimateLabel);

        infoPanel.add(Box.createVerticalStrut(10));

        totalGpLabel = new JLabel("Total GP: 0");
        totalGpLabel.setForeground(Color.YELLOW);
        infoPanel.add(totalGpLabel);

        avgGpPerKillLabel = new JLabel("Avg GP/Kill: 0");
        avgGpPerKillLabel.setForeground(Color.WHITE);
        infoPanel.add(avgGpPerKillLabel);

        gpPerHourLabel = new JLabel("GP/Hour: 0");
        gpPerHourLabel.setForeground(Color.WHITE);
        infoPanel.add(gpPerHourLabel);

        return infoPanel;
    }

    public void updatePanel()
    {
        SwingUtilities.invokeLater(() -> {
            updateNpcSelector();
            updateLabels();
        });
    }

    private void updateNpcSelector()
    {
        String currentSelection = (String) npcSelector.getSelectedItem();
        npcSelector.removeAllItems();
        npcSelector.addItem("-- Select NPC --");

        Map<String, NpcTrackingData> allData = plugin.getAllTrackingData();
        for (String npcName : allData.keySet())
        {
            npcSelector.addItem(npcName);
        }

        if (currentSelection != null)
        {
            npcSelector.setSelectedItem(currentSelection);
        }
        else if (plugin.getCurrentTrackedNpc() != null)
        {
            npcSelector.setSelectedItem(plugin.getCurrentTrackedNpc());
        }
    }

    private void updateLabels()
    {
        String trackedNpc = plugin.getCurrentTrackedNpc();

        if (plugin.isTracking() && trackedNpc != null)
        {
            statusLabel.setText("Status: Tracking");
            statusLabel.setForeground(Color.GREEN);
            currentNpcLabel.setText("Current NPC: " + trackedNpc);

            NpcTrackingData data = plugin.getTrackingData(trackedNpc);
            if (data != null)
            {
                totalKillsLabel.setText("Total Kills: " + data.getKillCount());
                totalKphLabel.setText("Total KPH: " + (data.getKillsPerHour() > 0 ? KPH_FORMAT.format(data.getKillsPerHour()) : "0"));
                recentKphLabel.setText("Recent KPH (" + config.recentTimeMinutes() + "m): " +
                        (data.getRecentKillsPerHour(config.recentTimeMinutes()) > 0 ? KPH_FORMAT.format(data.getRecentKillsPerHour(config.recentTimeMinutes())) : "0"));

                if (data.getFirstKill() != null)
                {
                    long sessionMinutes = java.time.temporal.ChronoUnit.MINUTES.between(
                            data.getFirstKill(),
                            java.time.Instant.now()
                    );
                    sessionTimeLabel.setText("Session Time: " + formatTime(sessionMinutes));
                }
                else
                {
                    sessionTimeLabel.setText("Session Time: 0m");
                }

                updateSlayerInfo(trackedNpc, data);
                updateGpInfo(data);
            }
        }
        else
        {
            statusLabel.setText("Status: Not tracking");
            statusLabel.setForeground(Color.ORANGE);
            currentNpcLabel.setText("Current NPC: None");
            totalKillsLabel.setText("Total Kills: 0");
            totalKphLabel.setText("Total KPH: 0");
            recentKphLabel.setText("Recent KPH: 0");
            sessionTimeLabel.setText("Session Time: 0m");
            slayerTaskLabel.setText("Slayer Task: None");
            taskProgressLabel.setText("Progress: N/A");
            timeEstimateLabel.setText("Est. Time: N/A");
            totalGpLabel.setText("Total GP: 0");
            avgGpPerKillLabel.setText("Avg GP/Kill: 0");
            gpPerHourLabel.setText("GP/Hour: 0");
        }
    }

    private void updateGpInfo(NpcTrackingData data)
    {
        totalGpLabel.setText("Total GP: " + formatGp(data.getTotalGpGained()));
        avgGpPerKillLabel.setText("Avg GP/Kill: " + formatGp((long) data.getAverageGpPerKill()));
        gpPerHourLabel.setText("GP/Hour: " + formatGp((long) data.getGpPerHour()));
    }

    private void updateSlayerInfo(String trackedNpc, NpcTrackingData data)
    {
        SlayerTaskData slayerTask = plugin.getCurrentSlayerTask();

        if (slayerTask != null && slayerTask.getTaskName().equalsIgnoreCase(trackedNpc))
        {
            slayerTaskLabel.setText("Slayer Task: " + slayerTask.getTaskName());
            taskProgressLabel.setText(String.format("Progress: %d/%d (%.1f%%)",
                    slayerTask.getCompleted(),
                    slayerTask.getOriginalAmount(),
                    slayerTask.getProgressPercentage()));

            String timeEstimate = plugin.getEstimatedTimeRemaining();
            timeEstimateLabel.setText("Est. Time: " + (timeEstimate != null ? timeEstimate : "Calculating..."));
        }
        else if (slayerTask != null)
        {
            slayerTaskLabel.setText("Slayer Task: " + slayerTask.getTaskName() + " (not tracking)");
            taskProgressLabel.setText(String.format("Progress: %d/%d",
                    slayerTask.getCompleted(),
                    slayerTask.getOriginalAmount()));
            timeEstimateLabel.setText("Est. Time: N/A");
        }
        else
        {
            slayerTaskLabel.setText("Slayer Task: None");
            taskProgressLabel.setText("Progress: N/A");
            timeEstimateLabel.setText("Est. Time: N/A");
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

    private String formatGp(long gp)
    {
        if (gp >= 1_000_000)
        {
            return String.format("%.1fM", gp / 1_000_000.0);
        }
        else if (gp >= 1_000)
        {
            return String.format("%.1fK", gp / 1_000.0);
        }
        else
        {
            return GP_FORMAT.format(gp);
        }
    }

    private class NpcSelectorListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            String selected = (String) npcSelector.getSelectedItem();
            if (selected != null && !selected.equals("-- Select NPC --"))
            {
                plugin.setTrackedNpc(selected);
                updateLabels();
            }
            else if (selected != null && selected.equals("-- Select NPC --"))
            {
                plugin.setTrackedNpc(null);
                updateLabels();
            }
        }
    }
}