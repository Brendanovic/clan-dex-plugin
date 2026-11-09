package com.clandex.ui;

import com.clandex.Catch;
import com.clandex.DexInfo;
import com.clandex.stats.StatFormula;
import com.clandex.stats.StatFormulas;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Party tab: up to 6 of the player's catches, each as a card.
 *
 * <pre>
 *  [ big  ]  Name #001             Lv 5   x
 *  [sprite]  [==========] HP 21/21
 *            Atk 12  Def 10  Spe 14  Spc 11
 *            Move one        Move two
 *  ( Empty slot )                              <- until all 6 are filled
 * </pre>
 *
 * HP is always full for now (there are no battles yet). Stats come from the formula the bot's file
 * names ({@link StatFormulas}).
 */
class PartyPanel extends JPanel
{
	private static final Color HP_GOOD = new Color(0x4CBB5E);
	private static final Color HP_TRACK = new Color(0x1C1C1C);

	private final SpriteLoader sprites;
	private final Consumer<String> onRemove;
	private final JPanel cards = new JPanel();

	/** {@code onRemove} gets the catch id when the player removes a catch from the party. */
	PartyPanel(SpriteLoader sprites, Consumer<String> onRemove)
	{
		super(new BorderLayout());
		this.sprites = sprites;
		this.onRemove = onRemove;
		setOpaque(false);
		cards.setOpaque(false);
		cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
		add(cards, BorderLayout.NORTH);
		setParty(Collections.emptyList(), null);
	}

	/** Shows these party members (already in party order) using the latest sync's info. */
	void setParty(List<Catch> members, DexInfo info)
	{
		cards.removeAll();
		for (Catch c : members)
		{
			cards.add(Layout.fullWidth(card(c, info)));
			cards.add(Layout.vgap(4));
		}
		for (int i = members.size(); i < PartyStore.MAX_SIZE; i++)
		{
			cards.add(Layout.fullWidth(emptySlot(i == members.size(), info == null ? "entry" : info.entry())));
			cards.add(Layout.vgap(4));
		}
		revalidate();
		repaint();
	}

	private JPanel card(Catch c, DexInfo info)
	{
		String species = info != null && c.getIndex() < info.getNames().size() ? info.getNames().get(c.getIndex()) : "?";
		String title = species + (c.getNumber() != null ? String.format(" #%03d", c.getNumber()) : "");
		int level = c.getLevel() != null ? c.getLevel() : 1;

		JLabel picture = new JLabel(Sprites.PLACEHOLDER_LARGE);
		picture.setVerticalAlignment(SwingConstants.TOP);
		String template = info == null ? null : (c.isShiny() && info.getShinySpriteTemplate() != null ? info.getShinySpriteTemplate() : info.getSpriteTemplate());
		if (template != null)
		{
			sprites.load(Links.entry(template, c.getIndex()), image -> picture.setIcon(Sprites.render(Sprites.trim(image), Theme.SPRITE_LARGE, true)));
		}
		else
		{
			picture.setIcon(null);
		}

		JLabel remove = Layout.label("x", Theme.SMALL, Theme.TEXT_FAINT);
		remove.setToolTipText("Remove from party");
		remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		remove.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				onRemove.accept(c.getId());
			}
		});

		JPanel top = new JPanel(new BorderLayout(6, 0));
		top.setOpaque(false);
		top.add(Layout.label(title, Theme.BODY, c.isShiny() ? Theme.SHINY : Theme.TEXT), BorderLayout.CENTER);
		JPanel right = new JPanel(new BorderLayout(8, 0));
		right.setOpaque(false);
		right.add(Layout.label("Lv " + level, Theme.SMALL, Theme.ACCENT), BorderLayout.WEST);
		right.add(remove, BorderLayout.EAST);
		top.add(right, BorderLayout.EAST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.add(Layout.fullWidth(top));

		int[] base = info == null ? null : info.baseStats(c.getIndex());
		int[] ivs = c.getIvs();
		if (base != null && ivs != null)
		{
			StatFormula formula = StatFormulas.forName(info.getStatFormula());
			int[] stats = formula.compute(base, ivs, level);
			text.add(Layout.vgap(3));
			text.add(Layout.fullWidth(hpBar(stats[0], stats[0])));
			text.add(Layout.vgap(3));
			text.add(Layout.label(String.format("Atk %d  Def %d  Spe %d  Spc %d", stats[1], stats[2], stats[3], stats[4]), Theme.SMALL, Theme.TEXT_MUTED));
		}

		if (!c.getMoves().isEmpty())
		{
			JPanel moves = new JPanel(new GridLayout(0, 2, 6, 0));
			moves.setOpaque(false);
			for (String move : c.getMoves())
			{
				moves.add(Layout.label(move, Theme.SMALL, Theme.TEXT));
			}
			text.add(Layout.vgap(3));
			text.add(Layout.fullWidth(moves));
		}

		JPanel card = new JPanel(new BorderLayout(8, 0));
		card.setBackground(Theme.ROW_CAUGHT);
		card.setBorder(BorderFactory.createEmptyBorder(6, 6, 8, 8));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.add(picture, BorderLayout.WEST);
		card.add(text, BorderLayout.CENTER);
		return card;
	}

	/** "HP 21/21" with a bar: green while healthy (always full until battles exist). */
	private static JPanel hpBar(int current, int max)
	{
		JComponent bar = new JComponent()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				g.setColor(HP_TRACK);
				g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
				g.setColor(HP_GOOD);
				g.fillRoundRect(0, 0, max == 0 ? 0 : getWidth() * current / max, getHeight(), 4, 4);
			}
		};
		bar.setPreferredSize(new Dimension(0, 6));

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(bar, BorderLayout.CENTER);
		row.add(Layout.label("HP " + current + "/" + max, Theme.SMALL, Theme.TEXT_MUTED), BorderLayout.EAST);
		JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.add(row, BorderLayout.CENTER);
		holder.setPreferredSize(new Dimension(0, 14));
		return holder;
	}

	/** An empty party slot; the first one says how to fill it, in the bot's word for an entry. */
	private static JPanel emptySlot(boolean showHint, String entry)
	{
		String article = !entry.isEmpty() && "aeiouAEIOU".indexOf(entry.charAt(0)) >= 0 ? "an " : "a ";
		JLabel label = Layout.label(showHint ? "Empty slot - open " + article + entry + " in the Dex and press + Party" : "Empty slot",
			Theme.SMALL, Theme.TEXT_FAINT);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		JPanel slot = new JPanel(new BorderLayout());
		slot.setBackground(Theme.ROW_MISSING);
		slot.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
		slot.add(label, BorderLayout.CENTER);
		slot.setAlignmentX(Component.LEFT_ALIGNMENT);
		return slot;
	}
}
