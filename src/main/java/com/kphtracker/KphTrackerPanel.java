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
import java.util.ArrayList;
import java.util.List;

public class KphTrackerPanel extends PluginPanel
{
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final Color COLOR_GOLD    = new Color(255, 215, 0);
    private static final Color COLOR_GREEN   = new Color(100, 220, 100);
    private static final Color COLOR_MUTED   = new Color(130, 130, 130);
    private static final Color COLOR_SECTION = new Color(40, 40, 40);
    private static final Color COLOR_RESET   = new Color(180, 50, 50);
    private static final Color COLOR_RESET_H = new Color(220, 70, 70);
    private static final Color COLOR_COPY    = new Color(50, 100, 180);
    private static final Color COLOR_COPY_H  = new Color(70, 130, 220);

    // -------------------------------------------------------------------------
    // Live stat labels
    // -------------------------------------------------------------------------

    private final JLabel npcValueLabel  = makeValueLabel("—");
    private final JLabel kphValueLabel  = makeValueLabel("—");
    private final JLabel peakValueLabel = makeValueLabel("—");
    private final JLabel killValueLabel = makeValueLabel("0");
    private final JLabel timeValueLabel = makeValueLabel("0:00");

    /** KPH progress bar — fills toward session peak. */
    private final JProgressBar kphBar;

    /** Container rebuilt every refresh when kill log is enabled. */
    private final JPanel killLogContainer;

    private final KphTrackerPlugin plugin;
    private final KphTrackerConfig config;
    private final Timer             updateTimer;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Inject
    KphTrackerPanel(KphTrackerPlugin plugin, KphTrackerConfig config)
    {
        super(false);
        this.plugin = plugin;
        this.config = config;

        kphBar = buildKphBar();
        killLogContainer = new JPanel();
        killLogContainer.setLayout(new BoxLayout(killLogContainer, BoxLayout.Y_AXIS));
        killLogContainer.setBackground(COLOR_SECTION);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Scrollable center area
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.add(buildStatsSection());
        content.add(Box.createVerticalStrut(8));
        content.add(buildKillLogSection());
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(),  BorderLayout.NORTH);
        add(scroll,         BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        updateTimer = new Timer(1000, e -> SwingUtilities.invokeLater(this::refresh));
        updateTimer.start();
    }

    // -------------------------------------------------------------------------
    // Section builders
    // -------------------------------------------------------------------------

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel icon = new JLabel("K");
        icon.setFont(new Font("SansSerif", Font.BOLD, 20));
        icon.setForeground(COLOR_GOLD);

        JLabel title = new JLabel("KPH Tracker");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Kill rate monitor");
        sub.setFont(FontManager.getRunescapeSmallFont());
        sub.setForeground(COLOR_MUTED);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 1));
        text.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        text.add(title);
        text.add(sub);

        header.add(icon, BorderLayout.WEST);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildStatsSection()
    {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(ColorScheme.DARK_GRAY_COLOR);
        section.setBorder(new EmptyBorder(10, 10, 0, 10));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(COLOR_SECTION);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(3, 0, 3, 8);
        lc.gridx = 0;

        GridBagConstraints rc = new GridBagConstraints();
        rc.anchor = GridBagConstraints.EAST;
        rc.insets = new Insets(3, 0, 3, 0);
        rc.gridx = 1;
        rc.weightx = 1.0;
        rc.fill = GridBagConstraints.HORIZONTAL;

        // Row: NPC
        addCardRow(card, "NPC",        npcValueLabel,  lc, rc, 0);
        addDivider(card, 1);
        // Row: KPH
        addCardRow(card, "Kills / hr", kphValueLabel,  lc, rc, 2);
        // KPH progress bar (spans full width)
        GridBagConstraints barC = new GridBagConstraints();
        barC.gridx = 0; barC.gridy = 3; barC.gridwidth = 2;
        barC.fill = GridBagConstraints.HORIZONTAL;
        barC.insets = new Insets(2, 0, 4, 0);
        card.add(kphBar, barC);
        addDivider(card, 4);
        // Row: Peak
        addCardRow(card, "Peak KPH",   peakValueLabel, lc, rc, 5);
        addDivider(card, 6);
        // Row: Total kills
        addCardRow(card, "Total kills", killValueLabel, lc, rc, 7);
        addDivider(card, 8);
        // Row: Session time
        addCardRow(card, "Session",    timeValueLabel, lc, rc, 9);

        section.add(card, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildKillLogSection()
    {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(ColorScheme.DARK_GRAY_COLOR);
        section.setBorder(new EmptyBorder(8, 10, 0, 10));

        // Section title
        JLabel title = new JLabel("Recent Kills");
        title.setFont(FontManager.getRunescapeSmallFont());
        title.setForeground(COLOR_MUTED);
        title.setBorder(new EmptyBorder(0, 0, 4, 0));

        section.add(title,            BorderLayout.NORTH);
        section.add(killLogContainer, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildFooter()
    {
        JButton resetBtn = makeButton("Reset Session", COLOR_RESET, COLOR_RESET_H);
        resetBtn.addActionListener(e ->
        {
            plugin.reset();
            refresh();
        });

        JButton copyBtn = makeButton("Copy Stats", COLOR_COPY, COLOR_COPY_H);
        copyBtn.addActionListener(e ->
        {
            plugin.copyStatsToClipboard();
            // Brief visual feedback
            String orig = copyBtn.getText();
            copyBtn.setText("Copied!");
            copyBtn.setEnabled(false);
            new Timer(1200, ev ->
            {
                copyBtn.setText(orig);
                copyBtn.setEnabled(true);
                ((Timer) ev.getSource()).stop();
            }).start();
        });

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 6, 0));
        btnRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        btnRow.add(resetBtn);
        btnRow.add(copyBtn);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        footer.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(10, 10, 10, 10)
        ));
        footer.add(btnRow, BorderLayout.CENTER);
        return footer;
    }

    // -------------------------------------------------------------------------
    // Refresh  (called on EDT every second)
    // -------------------------------------------------------------------------

    void refresh()
    {
        // Update peak before reading it
        plugin.checkAndUpdatePeak();

        String npc  = plugin.getTrackedNpcName();
        double kph  = plugin.getKillsPerHour();
        double peak = plugin.getPeakKph();
        Color  col  = plugin.getKphColor();

        // NPC
        npcValueLabel.setText(npc != null ? npc : "—");
        npcValueLabel.setForeground(npc != null ? Color.WHITE : COLOR_MUTED);

        // KPH
        kphValueLabel.setText(kph > 0 ? String.format("%.1f", kph) : "—");
        kphValueLabel.setForeground(kph > 0 ? col : COLOR_MUTED);

        // KPH progress bar
        if (peak > 0 && kph > 0)
        {
            int pct = (int) Math.min(100, (kph / peak) * 100);
            kphBar.setValue(pct);
            kphBar.setForeground(col);
            kphBar.setVisible(true);
        }
        else
        {
            kphBar.setValue(0);
            kphBar.setVisible(false);
        }

        // Peak
        peakValueLabel.setText(peak > 0 ? String.format("%.1f", peak) : "—");
        peakValueLabel.setForeground(peak > 0 ? COLOR_GOLD : COLOR_MUTED);

        // Kills
        killValueLabel.setText(String.valueOf(plugin.getTotalKills()));

        // Session time
        Instant start = plugin.getSessionStart();
        timeValueLabel.setText(
            start != null
                ? KphTrackerPlugin.formatDuration(Duration.between(start, Instant.now()))
                : "—"
        );

        // Kill log
        refreshKillLog();
    }

    private void refreshKillLog()
    {
        killLogContainer.removeAll();

        if (!config.showKillLog())
        {
            killLogContainer.setVisible(false);
            killLogContainer.revalidate();
            return;
        }

        killLogContainer.setVisible(true);

        List<KillLogEntry> entries = new ArrayList<>(plugin.getKillLog());
        int limit = Math.min(entries.size(), config.killLogSize());

        if (limit == 0)
        {
            JLabel empty = new JLabel("No kills yet this session");
            empty.setFont(FontManager.getRunescapeSmallFont());
            empty.setForeground(COLOR_MUTED);
            empty.setBorder(new EmptyBorder(6, 8, 6, 8));
            killLogContainer.add(empty);
        }
        else
        {
            int killNumber = plugin.getTotalKills();
            for (int i = 0; i < limit; i++)
            {
                KillLogEntry entry = entries.get(i);
                int num = killNumber - i;
                killLogContainer.add(buildKillLogRow(num, entry, i));
            }
        }

        killLogContainer.revalidate();
        killLogContainer.repaint();
    }

    private JPanel buildKillLogRow(int killNum, KillLogEntry entry, int index)
    {
        Duration ago    = Duration.between(entry.time, Instant.now());
        String   agoStr = formatAgo(ago);
        String   name   = entry.npcName != null ? entry.npcName : "Unknown";

        JLabel numLabel = new JLabel(String.format("#%d", killNum));
        numLabel.setFont(FontManager.getRunescapeSmallFont());
        numLabel.setForeground(COLOR_MUTED);
        numLabel.setPreferredSize(new Dimension(38, 16));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(Color.WHITE);

        JLabel agoLabel = new JLabel(agoStr);
        agoLabel.setFont(FontManager.getRunescapeSmallFont());
        agoLabel.setForeground(COLOR_MUTED);
        agoLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(index % 2 == 0 ? COLOR_SECTION : new Color(48, 48, 48));
        row.setBorder(new EmptyBorder(4, 8, 4, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        row.add(numLabel,  BorderLayout.WEST);
        row.add(nameLabel, BorderLayout.CENTER);
        row.add(agoLabel,  BorderLayout.EAST);
        return row;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static JLabel makeValueLabel(String text)
    {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FontManager.getRunescapeSmallFont());
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        return lbl;
    }

    private static JProgressBar buildKphBar()
    {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(false);
        bar.setForeground(COLOR_GREEN);
        bar.setBackground(new Color(60, 60, 60));
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(0, 4));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        bar.setVisible(false);
        return bar;
    }

    private static JButton makeButton(String text, Color base, Color hover)
    {
        JButton btn = new JButton(text);
        btn.setFont(FontManager.getRunescapeSmallFont());
        btn.setBackground(base);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 30));
        btn.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(base);  }
        });
        return btn;
    }

    private void addCardRow(JPanel card, String label, JLabel value,
                            GridBagConstraints lc, GridBagConstraints rc, int row)
    {
        JLabel lbl = new JLabel(label);
        lbl.setFont(FontManager.getRunescapeSmallFont());
        lbl.setForeground(COLOR_MUTED);
        lc.gridy = row;
        rc.gridy = row;
        card.add(lbl,   lc);
        card.add(value, rc);
    }

    private void addDivider(JPanel card, int row)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 0, 1, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 60, 60));
        card.add(sep, c);
    }

    private String formatAgo(Duration d)
    {
        long totalSecs = d.getSeconds();
        if (totalSecs < 60)  return totalSecs + "s ago";
        if (totalSecs < 3600)
        {
            long m = totalSecs / 60, s = totalSecs % 60;
            return String.format("%dm %02ds ago", m, s);
        }
        return "> 1hr ago";
    }

    void shutdown()
    {
        updateTimer.stop();
    }
}
