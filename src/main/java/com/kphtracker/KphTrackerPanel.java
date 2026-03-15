package com.kphtracker;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class KphTrackerPanel extends PluginPanel
{
    private final KphTrackerPlugin plugin;

    // Stat display labels
    private final JLabel npcValueLabel  = new JLabel("—");
    private final JLabel kphValueLabel  = new JLabel("—");
    private final JLabel killValueLabel = new JLabel("0");
    private final JLabel timeValueLabel = new JLabel("0:00");

    // Refresh the panel every second
    private final Timer updateTimer;

    KphTrackerPanel(KphTrackerPlugin plugin)
    {
        super(false); // false = no auto scroll pane wrapper
        this.plugin = plugin;

        setLayout(new BorderLayout(0, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildStatsCard(), BorderLayout.CENTER);
        add(buildFooter(),    BorderLayout.SOUTH);

        updateTimer = new Timer(1000, e -> SwingUtilities.invokeLater(this::refresh));
        updateTimer.start();
    }

    // -------------------------------------------------------------------------
    // Layout builders
    // -------------------------------------------------------------------------

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel title = new JLabel("KPH Tracker");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.YELLOW);

        JLabel subtitle = new JLabel("Kill rate monitor");
        subtitle.setFont(FontManager.getRunescapeSmallFont());
        subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        text.add(title);
        text.add(subtitle);

        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildStatsCard()
    {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        GridBagConstraints left = new GridBagConstraints();
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(4, 0, 4, 8);
        left.gridx = 0;

        GridBagConstraints right = new GridBagConstraints();
        right.anchor = GridBagConstraints.EAST;
        right.insets = new Insets(4, 0, 4, 0);
        right.gridx = 1;
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;

        String[] labelTexts = {"NPC", "Kills / hr", "Total kills", "Session"};
        JLabel[] valueLabels = {npcValueLabel, kphValueLabel, killValueLabel, timeValueLabel};

        for (int i = 0; i < labelTexts.length; i++)
        {
            // Divider between rows (skip first)
            if (i > 0)
            {
                GridBagConstraints divC = new GridBagConstraints();
                divC.gridx = 0;
                divC.gridy = (i * 2) - 1;
                divC.gridwidth = 2;
                divC.fill = GridBagConstraints.HORIZONTAL;
                divC.insets = new Insets(0, 0, 0, 0);
                JSeparator sep = new JSeparator();
                sep.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
                card.add(sep, divC);
            }

            JLabel lbl = new JLabel(labelTexts[i]);
            lbl.setFont(FontManager.getRunescapeSmallFont());
            lbl.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

            JLabel val = valueLabels[i];
            val.setFont(FontManager.getRunescapeSmallFont());
            val.setForeground(Color.WHITE);
            val.setHorizontalAlignment(SwingConstants.RIGHT);

            left.gridy = i * 2;
            right.gridy = i * 2;
            card.add(lbl, left);
            card.add(val, right);
        }

        // Wrap in a panel with a visible border
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrapper.setBorder(new EmptyBorder(10, 10, 0, 10));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter()
    {
        JButton resetBtn = new JButton("Reset Session");
        resetBtn.setFont(FontManager.getRunescapeSmallFont());
        resetBtn.setBackground(new Color(180, 50, 50));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFocusPainted(false);
        resetBtn.setBorderPainted(false);
        resetBtn.setOpaque(true);
        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetBtn.setPreferredSize(new Dimension(0, 32));

        // Hover effect
        resetBtn.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e)
            {
                resetBtn.setBackground(new Color(220, 70, 70));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e)
            {
                resetBtn.setBackground(new Color(180, 50, 50));
            }
        });

        resetBtn.addActionListener(e ->
        {
            plugin.reset();
            refresh();
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        footer.setBorder(new EmptyBorder(10, 10, 10, 10));
        footer.add(resetBtn, BorderLayout.CENTER);
        return footer;
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    void refresh()
    {
        // NPC name
        String npcName = plugin.getTrackedNpcName();
        npcValueLabel.setText(npcName != null ? npcName : "—");

        // KPH
        double kph = plugin.getKillsPerHour();
        if (kph > 0)
        {
            kphValueLabel.setText(String.format("%.1f", kph));
            kphValueLabel.setForeground(new Color(100, 220, 100));
        }
        else
        {
            kphValueLabel.setText("—");
            kphValueLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        }

        // Total kills
        killValueLabel.setText(String.valueOf(plugin.getTotalKills()));

        // Session time
        Instant start = plugin.getSessionStart();
        if (start != null)
        {
            timeValueLabel.setText(formatDuration(Duration.between(start, Instant.now())));
        }
    }

    void shutdown()
    {
        updateTimer.stop();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String formatDuration(Duration d)
    {
        long hours   = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();

        if (hours > 0)
        {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }
}
