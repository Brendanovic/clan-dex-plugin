package com.clandex.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * One entry in the list.
 *
 * <pre>
 *  [sprite] #025  Name                   x3      <- always visible
 *  [ big  ]  Name                                <- detail card, slides open on click
 *  [sprite]  Caught 3 times
 *            Your most caught
 * </pre>
 *
 * Caught entries are bright, with their sprite in full; missing ones are dimmed, with a dark
 * silhouette. If the clan's bot sends no sprites, rows show names only, with no gap.
 * Right-click offers "Copy name".
 */
class DexRow extends JPanel
{
	private static final int FRAME_MS = 15;

	private final int index;
	private final String name;
	private final String searchText;

	private final JPanel header = new JPanel(new BorderLayout(6, 0));
	private final JLabel number = new JLabel();
	private final JLabel nameLabel = new JLabel();
	private final JLabel countLabel = new JLabel();

	private final JPanel detail = new JPanel(new BorderLayout(10, 0));
	private final JLabel bigSprite = new JLabel();
	private final JLabel detailName = new JLabel();
	private final JLabel detailCount = new JLabel();
	private final JLabel detailNote = new JLabel();

	/** Animates the detail card open and closed; 0 = closed, 1 = open. */
	private final Timer slider = new Timer(FRAME_MS, e -> slideStep());
	private float openness;
	private boolean expanded;

	private int count;
	private boolean hovered;

	/** Whether this row shows a picture: false when the bot sends no sprites. */
	private boolean spritesOn;

	/** The sprite prepared at both sizes, caught and not caught; null until it has downloaded. */
	private ImageIcon spriteCaught;
	private ImageIcon spriteMissing;
	private ImageIcon spriteCaughtLarge;
	private ImageIcon spriteMissingLarge;

	DexRow(int index, String name)
	{
		super(null); // children are placed in doLayout so the detail card can be clipped while sliding
		this.index = index;
		this.name = name;
		this.searchText = (name + " " + number(index)).toLowerCase();

		buildHeader();
		buildDetail();
		add(header);
		add(detail);

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new RowMouse());
		setCount(0, false);
	}

	/** Formats an index as its dex number, e.g. 24 becomes "#025". */
	static String number(int index)
	{
		return String.format("#%03d", index + 1);
	}

	int getIndex()
	{
		return index;
	}

	String getEntryName()
	{
		return name;
	}

	int getCount()
	{
		return count;
	}

	boolean isCaught()
	{
		return count > 0;
	}

	/** True if the name or number contains {@code query}, which must already be lower case. */
	boolean matches(String query)
	{
		return query.isEmpty() || searchText.contains(query);
	}

	/**
	 * Updates the row for how many times this entry was caught (0 = not yet).
	 * {@code mostCaught} marks the player's most-caught entry in the detail card.
	 */
	void setCount(int timesCaught, boolean mostCaught)
	{
		count = timesCaught;
		boolean caught = timesCaught > 0;

		number.setForeground(caught ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
		nameLabel.setForeground(caught ? Theme.TEXT : Theme.TEXT_FAINT);
		countLabel.setText(timesCaught > 1 ? "x" + timesCaught : "");

		detailName.setForeground(caught ? Theme.TEXT : Theme.TEXT_MUTED);
		detailCount.setText(caught
			? "Caught " + timesCaught + (timesCaught == 1 ? " time" : " times")
			: "Not caught yet");
		detailNote.setText(mostCaught ? "Your most caught" : caught ? " " : "Still out there");
		detailNote.setForeground(mostCaught ? Theme.ACCENT : Theme.TEXT_FAINT);

		refreshPictures();
		updateBackground();
	}

	/**
	 * Turns pictures on (a sprite is on its way) or off (names only). Either way any old sprite
	 * is dropped; while one downloads, a blank placeholder holds its space.
	 */
	void useSprites(boolean on)
	{
		spritesOn = on;
		spriteCaught = spriteMissing = spriteCaughtLarge = spriteMissingLarge = null;
		refreshPictures();
	}

	/** Shows the entry's sprite once it has downloaded. Prepared once, at both sizes. */
	void setSprite(BufferedImage image)
	{
		if (!spritesOn)
		{
			return; // pictures were switched off while this one was downloading
		}
		BufferedImage trimmed = Sprites.trim(image);
		spriteCaught = Sprites.render(trimmed, Theme.SPRITE, true);
		spriteMissing = Sprites.render(trimmed, Theme.SPRITE, false);
		spriteCaughtLarge = Sprites.render(trimmed, Theme.SPRITE_LARGE, true);
		spriteMissingLarge = Sprites.render(trimmed, Theme.SPRITE_LARGE, false);
		refreshPictures();
	}

	private void refreshPictures()
	{
		boolean caught = isCaught();
		if (!spritesOn)
		{
			number.setIcon(null);
			bigSprite.setIcon(null);
		}
		else if (spriteCaught == null)
		{
			number.setIcon(Sprites.PLACEHOLDER);
			bigSprite.setIcon(Sprites.PLACEHOLDER_LARGE);
		}
		else
		{
			number.setIcon(caught ? spriteCaught : spriteMissing);
			bigSprite.setIcon(caught ? spriteCaughtLarge : spriteMissingLarge);
		}
		bigSprite.setVisible(spritesOn);
	}

	/** Closes the detail card without animating (used when the list is rebuilt or filtered). */
	void collapse()
	{
		expanded = false;
		openness = 0f;
		slider.stop();
		revalidate();
	}

	// ---------------------------------------------------------------- layout

	private void buildHeader()
	{
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));

		number.setText(number(index));
		number.setFont(Theme.SMALL);
		number.setIconTextGap(6);

		nameLabel.setText(name);
		nameLabel.setFont(Theme.BODY);

		countLabel.setFont(Theme.SMALL);
		countLabel.setForeground(Theme.ACCENT);

		header.add(number, BorderLayout.WEST);
		header.add(nameLabel, BorderLayout.CENTER);
		header.add(countLabel, BorderLayout.EAST);
	}

	private void buildDetail()
	{
		detail.setOpaque(false);
		detail.setBorder(BorderFactory.createEmptyBorder(2, 6, 8, 8));

		Layout.style(detailName, Theme.TITLE, Theme.TEXT).setText(name);
		Layout.style(detailCount, Theme.SMALL, Theme.TEXT_MUTED);
		Layout.style(detailNote, Theme.SMALL, Theme.TEXT_FAINT);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.add(detailName);
		text.add(Layout.vgap(2));
		text.add(detailCount);
		text.add(detailNote);

		detail.add(bigSprite, BorderLayout.WEST);
		detail.add(text, BorderLayout.CENTER);
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(0, Theme.ROW_HEIGHT + Math.round(Theme.DETAIL_HEIGHT * openness));
	}

	@Override
	public void doLayout()
	{
		// the detail card keeps its full height and is simply cut off while the row is shorter
		header.setBounds(0, 0, getWidth(), Theme.ROW_HEIGHT);
		detail.setBounds(0, Theme.ROW_HEIGHT, getWidth(), Theme.DETAIL_HEIGHT);
	}

	// ---------------------------------------------------------------- behaviour

	private void toggle()
	{
		expanded = !expanded;
		slider.start();
		updateBackground();
	}

	private void slideStep()
	{
		float target = expanded ? 1f : 0f;
		float stepSize = FRAME_MS / (float) Theme.ANIM_MS;
		float diff = target - openness;
		openness = Math.abs(diff) <= stepSize ? target : openness + Math.signum(diff) * stepSize;
		if (openness == target)
		{
			slider.stop();
		}
		revalidate(); // our preferred height changed, so the list re-flows around us
		repaint();
	}

	private void updateBackground()
	{
		Color base = isCaught() ? Theme.ROW_CAUGHT : Theme.ROW_MISSING;
		setBackground(hovered || expanded ? Theme.ROW_HOVER : base);
	}

	private void showMenu(MouseEvent e)
	{
		JPopupMenu menu = new JPopupMenu();
		JMenuItem copy = new JMenuItem("Copy name");
		copy.addActionListener(a -> Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(name), null));
		menu.add(copy);
		menu.show(this, e.getX(), e.getY());
	}

	private class RowMouse extends MouseAdapter
	{
		@Override
		public void mouseEntered(MouseEvent e)
		{
			hovered = true;
			updateBackground();
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			hovered = false;
			updateBackground();
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e))
			{
				showMenu(e);
			}
			else if (SwingUtilities.isLeftMouseButton(e))
			{
				toggle();
			}
		}
	}
}
