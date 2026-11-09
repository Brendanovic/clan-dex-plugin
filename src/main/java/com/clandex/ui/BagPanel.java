package com.clandex.ui;

import com.clandex.DexInfo;
import com.clandex.PlayerDex;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Bag tab: every item the player holds, with its picture (if the bot links one) and count.
 *
 * <pre>
 *  [pic] Basic Orb        Unlimited    <- always first: every player has unlimited Basic Orbs
 *  [pic] Great Orb              x2
 *  [pic] Level Treat              x1
 * </pre>
 */
class BagPanel extends JPanel
{
	private final SpriteLoader sprites;
	private final JPanel list = new JPanel(new GridLayout(0, 1, 0, 2));
	private final JLabel message = Layout.label("Sync to see your bag", Theme.SMALL, Theme.TEXT_FAINT);

	BagPanel(SpriteLoader sprites)
	{
		super(new BorderLayout());
		this.sprites = sprites;
		setOpaque(false);

		list.setOpaque(false);
		message.setHorizontalAlignment(SwingConstants.CENTER);
		message.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

		add(list, BorderLayout.NORTH);
		add(message, BorderLayout.CENTER);
	}

	/** Shows the player's bag from the latest sync, starting with their unlimited Basic Orbs. */
	void setBag(DexInfo info, PlayerDex player)
	{
		list.removeAll();
		list.add(itemRow(info, DexInfo.UNLIMITED_BALL, "Basic Orb", "Unlimited"));
		Map<String, Integer> bag = player.getBag();
		for (Map.Entry<String, Integer> item : bag.entrySet())
		{
			list.add(itemRow(info, item.getKey(), info.itemName(item.getKey()), "x" + item.getValue()));
		}
		message.setText("No other items yet - you sometimes find one after a catch");
		message.setVisible(bag.isEmpty());
		revalidate();
		repaint();
	}

	private JPanel itemRow(DexInfo info, String id, String displayName, String amountText)
	{
		JLabel name = Layout.label(displayName, Theme.BODY, Theme.TEXT);
		name.setIconTextGap(8);
		JLabel amount = Layout.label(amountText, Theme.SMALL, Theme.ACCENT);

		String template = info.getItemSpriteTemplate();
		if (template != null)
		{
			name.setIcon(Sprites.ITEM_PLACEHOLDER); // holds the space so the text doesn't jump
			sprites.load(Links.item(template, id), image ->
				name.setIcon(Sprites.render(Sprites.trim(image), Theme.ITEM_SPRITE, true)));
		}

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(Theme.ROW_CAUGHT);
		row.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 10));
		row.setPreferredSize(new Dimension(0, Theme.ITEM_SPRITE + 8));
		row.add(name, BorderLayout.CENTER);
		row.add(amount, BorderLayout.EAST);
		return row;
	}
}
