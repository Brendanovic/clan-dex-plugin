package com.clandex.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.components.ProgressBar;

/**
 * The stats card at the top of the panel:
 * <pre>
 *  Fake Test Player
 *  37 / 151                        24%
 *  [=========-----------------------]
 *  52 total catches
 * </pre>
 */
class DexSummary extends JPanel
{
	private final JLabel player = Layout.label("Not synced yet", Theme.SMALL, Theme.TEXT_MUTED);
	private final JLabel caughtCount = Layout.label("0", Theme.BIG_NUMBER, Theme.TEXT);
	private final JLabel outOf = Layout.label(" / 0", Theme.BODY, Theme.TEXT_MUTED);
	private final JLabel percent = Layout.label("0%", Theme.TITLE, Theme.ACCENT);
	private final JLabel totalCatches = Layout.label(" ", Theme.SMALL, Theme.TEXT_MUTED);
	private final ProgressBar progress = new ProgressBar();

	DexSummary()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Theme.CARD);
		setBorder(BorderFactory.createEmptyBorder(Theme.GAP, 10, Theme.GAP + 2, 10));

		add(Layout.fullWidth(spread(player, new JLabel())));
		add(Layout.vgap(2));

		JPanel count = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		count.setOpaque(false);
		count.add(caughtCount);
		count.add(outOf);
		add(Layout.fullWidth(spread(count, percent)));
		add(Layout.vgap(4));

		// no labels on the bar itself: the numbers above already say it
		progress.setForeground(Theme.PROGRESS);
		progress.setBackground(Theme.PROGRESS_TRACK);
		progress.setPreferredSize(new Dimension(0, 8));
		add(Layout.fullWidth(progress));
		add(Layout.vgap(6));

		add(Layout.fullWidth(totalCatches));
	}

	void setPlayer(String name)
	{
		player.setText(name);
	}

	/** Refreshes every number on the card from a player's catches out of {@code entries} in the dex. */
	void setCaught(Map<Integer, Integer> caught, int entries)
	{
		int unique = caught.size();
		int total = caught.values().stream().mapToInt(Integer::intValue).sum();

		caughtCount.setText(String.valueOf(unique));
		outOf.setText(" / " + entries);
		percent.setText(entries == 0 ? "0%" : Math.round(unique * 100f / entries) + "%");
		progress.setMaximumValue(Math.max(1, entries));
		progress.setValue(unique);
		totalCatches.setText(total == 0 ? " " : total + (total == 1 ? " total catch" : " total catches"));
	}

	/** A transparent row with one component pinned left and one pinned right. */
	private static JPanel spread(Component left, Component right)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.add(left, BorderLayout.WEST);
		row.add(right, BorderLayout.EAST);
		return row;
	}
}
