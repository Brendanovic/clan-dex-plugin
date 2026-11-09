package com.clandex.ui;

import com.clandex.Catch;
import com.clandex.DexInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * One entry in the list.
 *
 * <pre>
 *  [sprite] #025  Name                 * x3     <- always visible (* = owns a shiny)
 *  [ big  ]  Name                               <- detail card, slides open on click
 *  [sprite]  Caught 3 times (1 shiny)
 *            Your most caught
 *  [s] Moroka #001                     Lv 14   <- one block per catch, by the player's number
 *      Male, Great Orb, 3 throws                  for it ("--rarecandy moroka 1" uses it)
 *      Adamant nature, Static                     "+ Party" next to the level adds it to the party
 *      Growl, Tackle
 *      9 Nov 2026, 01:11, IVs 2/5/5/9
 * </pre>
 *
 * Caught entries are bright with their sprite in full (the shiny sprite if they own a shiny);
 * missing ones are dimmed silhouettes. If the clan's bot sends no sprites, rows show names only.
 * Right-click offers "Copy name".
 */
class DexRow extends JPanel
{
	private static final int FRAME_MS = 15;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

	/** By the player's number for each catch (#001, #002...), the order the bot uses too. */
	private static final Comparator<Catch> CATCH_ORDER = Comparator.comparing(Catch::getNumber, Comparator.nullsLast(Comparator.naturalOrder()));

	private final int index;
	private final String name;
	private final String searchText;

	private final JPanel header = new JPanel(new BorderLayout(6, 0));
	private final JLabel number = new JLabel();
	private final JLabel nameLabel = new JLabel();
	private final JLabel countLabel = new JLabel();

	private final JPanel detail = new JPanel();
	private final JLabel bigSprite = new JLabel();
	private final JLabel detailName = new JLabel();
	private final JLabel detailCount = new JLabel();
	private final JLabel detailNote = new JLabel();
	private final JPanel catchList = new JPanel();

	/** Animates the detail card open and closed; 0 = closed, 1 = open. */
	private final Timer slider = new Timer(FRAME_MS, e -> slideStep());
	private float openness;
	private boolean expanded;
	private boolean hovered;

	private int count;
	private List<Catch> catches = Collections.emptyList();
	private DexInfo info;
	private PartyControl party; // null until the panel provides it

	/** Whether this row shows pictures: false when the bot sends no sprites. */
	private boolean spritesOn;

	// pictures, prepared once when they download; null until then
	private ImageIcon spriteCaught;
	private ImageIcon spriteMissing;
	private ImageIcon spriteCaughtLarge;
	private ImageIcon spriteMissingLarge;
	private ImageIcon shinySprite;
	private ImageIcon shinySpriteLarge;

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
		setPlayerData(0, false, Collections.emptyList(), null);
	}

	/** Lets each catch line add itself to, or remove itself from, the party. */
	void setPartyControl(PartyControl control)
	{
		this.party = control;
	}

	/** Redraws the catch lines, e.g. after the party changed. */
	void refreshCatchList()
	{
		rebuildCatchList();
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

	boolean hasShiny()
	{
		return catches.stream().anyMatch(Catch::isShiny);
	}

	/** True if the name or number contains {@code query}, which must already be lower case. */
	boolean matches(String query)
	{
		return query.isEmpty() || searchText.contains(query);
	}

	/**
	 * Updates the row from the latest sync: how many times this entry was caught, whether it is
	 * the player's most caught, and each individual catch of it (may be empty if the bot doesn't
	 * send them). {@code info} names the balls.
	 */
	void setPlayerData(int timesCaught, boolean mostCaught, List<Catch> catchesOfThis, DexInfo info)
	{
		this.count = timesCaught;
		this.info = info;
		this.catches = new ArrayList<>(catchesOfThis);
		this.catches.sort(CATCH_ORDER);
		boolean caught = timesCaught > 0;
		long shinies = catches.stream().filter(Catch::isShiny).count();

		number.setForeground(caught ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
		nameLabel.setForeground(caught ? Theme.TEXT : Theme.TEXT_FAINT);
		countLabel.setText(timesCaught > 1 ? "x" + timesCaught : "");
		countLabel.setIcon(shinies > 0 ? Icons.SHINY : null);

		detailName.setForeground(caught ? Theme.TEXT : Theme.TEXT_MUTED);
		detailCount.setText(!caught ? "Not caught yet"
			: "Caught " + timesCaught + (timesCaught == 1 ? " time" : " times") + (shinies > 0 ? " (" + shinies + " shiny)" : ""));
		detailNote.setText(mostCaught ? "Your most caught" : caught ? " " : "Still out there");
		detailNote.setForeground(mostCaught ? Theme.ACCENT : Theme.TEXT_FAINT);

		refreshPictures();
		updateBackground();
	}

	/**
	 * Turns pictures on (sprites are on their way) or off (names only). Either way any old
	 * pictures are dropped; while one downloads, a blank placeholder holds its space.
	 */
	void useSprites(boolean on)
	{
		spritesOn = on;
		spriteCaught = spriteMissing = spriteCaughtLarge = spriteMissingLarge = shinySprite = shinySpriteLarge = null;
		refreshPictures();
	}

	/** The entry's normal sprite, once it has downloaded. Prepared once, at both sizes. */
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

	/** The entry's shiny sprite, once it has downloaded (only asked for if the player owns one). */
	void setShinySprite(BufferedImage image)
	{
		if (!spritesOn)
		{
			return;
		}
		BufferedImage trimmed = Sprites.trim(image);
		shinySprite = Sprites.render(trimmed, Theme.SPRITE, true);
		shinySpriteLarge = Sprites.render(trimmed, Theme.SPRITE_LARGE, true);
		refreshPictures();
	}

	/** Closes the detail card without animating (used when the list is rebuilt or filtered). */
	void collapse()
	{
		expanded = false;
		openness = 0f;
		slider.stop();
		revalidate();
	}

	// ---------------------------------------------------------------- pictures

	/** Puts the right picture everywhere: header, detail card and each catch line. */
	private void refreshPictures()
	{
		boolean showShiny = hasShiny() && shinySprite != null;
		number.setIcon(pick(showShiny ? shinySprite : isCaught() ? spriteCaught : spriteMissing, Sprites.PLACEHOLDER));
		bigSprite.setIcon(pick(showShiny ? shinySpriteLarge : isCaught() ? spriteCaughtLarge : spriteMissingLarge, Sprites.PLACEHOLDER_LARGE));
		bigSprite.setVisible(spritesOn);
		rebuildCatchList();
	}

	/** The picture if pictures are on and it has loaded, a blank placeholder while loading, or none. */
	private ImageIcon pick(ImageIcon loaded, ImageIcon placeholder)
	{
		if (!spritesOn)
		{
			return null;
		}
		return loaded != null ? loaded : placeholder;
	}

	// ---------------------------------------------------------------- catch list

	private void rebuildCatchList()
	{
		catchList.removeAll();
		for (Catch c : catches)
		{
			catchList.add(catchLine(c));
		}
		if (openness > 0)
		{
			revalidate(); // the card's height changed while it is showing
		}
	}

	private JPanel catchLine(Catch c)
	{
		String title = name + (c.getNumber() != null ? String.format(" #%03d", c.getNumber()) : "") + (c.isShiny() ? "  Shiny" : "");

		List<String> how = new ArrayList<>();
		if (c.getGender() != null)
		{
			how.add(c.getGender().equals("female") ? "Female" : "Male");
		}
		if (c.getCaughtWith() != null && info != null)
		{
			how.add(info.itemName(c.getCaughtWith()));
		}
		if (c.getThrowCount() != null)
		{
			how.add(c.getThrowCount() + (c.getThrowCount() == 1 ? " throw" : " throws"));
		}

		List<String> traits = new ArrayList<>();
		if (c.getNature() != null)
		{
			traits.add(c.getNature() + " nature");
		}
		if (c.getAbility() != null)
		{
			traits.add(c.getAbility() + (c.isHiddenAbility() ? " (hidden ability)" : ""));
		}

		List<String> when = new ArrayList<>();
		when.add(c.getCaughtAt() != null ? DATE.format(c.getCaughtAt()) : "Date not recorded");
		int[] ivs = c.getIvs();
		if (ivs != null)
		{
			when.add("IVs " + ivs[0] + "/" + ivs[1] + "/" + ivs[2] + "/" + ivs[3]);
		}

		JPanel titleRow = new JPanel(new BorderLayout(6, 0));
		titleRow.setOpaque(false);
		titleRow.add(Layout.label(title, Theme.SMALL, c.isShiny() ? Theme.SHINY : Theme.TEXT), BorderLayout.CENTER);
		JPanel titleRight = new JPanel(new BorderLayout(8, 0));
		titleRight.setOpaque(false);
		if (c.getLevel() != null)
		{
			titleRight.add(Layout.label("Lv " + c.getLevel(), Theme.SMALL, Theme.ACCENT), BorderLayout.WEST);
		}
		if (party != null && !c.getId().isEmpty())
		{
			titleRight.add(partyToggle(c), BorderLayout.EAST);
		}
		titleRow.add(titleRight, BorderLayout.EAST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.add(Layout.fullWidth(titleRow));
		if (!how.isEmpty())
		{
			text.add(Layout.label(String.join(", ", how), Theme.SMALL, Theme.TEXT_MUTED));
		}
		if (!traits.isEmpty())
		{
			text.add(Layout.label(String.join(", ", traits), Theme.SMALL, c.isHiddenAbility() ? Theme.SHINY : Theme.TEXT_MUTED));
		}
		if (!c.getMoves().isEmpty())
		{
			text.add(Layout.label(String.join(", ", c.getMoves()), Theme.SMALL, Theme.TEXT_MUTED));
		}
		text.add(Layout.label(String.join(", ", when), Theme.SMALL, Theme.TEXT_FAINT));

		JLabel picture = new JLabel(pick(c.isShiny() ? shinySprite : spriteCaught, Sprites.PLACEHOLDER));
		picture.setVerticalAlignment(SwingConstants.TOP);

		JPanel line = new JPanel(new BorderLayout(6, 0));
		line.setOpaque(false);
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		line.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		line.setToolTipText(c.getId().isEmpty() ? null : "Catch id " + c.getId());
		line.add(picture, BorderLayout.WEST);
		line.add(text, BorderLayout.CENTER);
		return line;
	}

	/** "+ Party" / "In party": click to add this catch to the party or take it out. */
	private JLabel partyToggle(Catch c)
	{
		boolean in = party.contains(c.getId());
		JLabel toggle = Layout.label(in ? "In party" : "+ Party", Theme.SMALL, in ? Theme.PROGRESS : Theme.TEXT_MUTED);
		toggle.setToolTipText(in ? "Remove from your party" : "Add to your party (up to " + PartyStore.MAX_SIZE + ")");
		toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		toggle.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (!party.toggle(c.getId()))
				{
					toggle.setText("Party full");
					toggle.setForeground(Theme.ERROR);
				}
			}
		});
		return toggle;
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
		countLabel.setIconTextGap(4);

		header.add(number, BorderLayout.WEST);
		header.add(nameLabel, BorderLayout.CENTER);
		header.add(countLabel, BorderLayout.EAST);
	}

	private void buildDetail()
	{
		detail.setOpaque(false);
		detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
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

		JPanel summary = new JPanel(new BorderLayout(10, 0));
		summary.setOpaque(false);
		summary.setAlignmentX(Component.LEFT_ALIGNMENT);
		summary.add(bigSprite, BorderLayout.WEST);
		summary.add(text, BorderLayout.CENTER);

		catchList.setOpaque(false);
		catchList.setLayout(new BoxLayout(catchList, BoxLayout.Y_AXIS));
		catchList.setAlignmentX(Component.LEFT_ALIGNMENT);

		detail.add(summary);
		detail.add(catchList);
	}

	/** Full height of the detail card: grows with the number of catches. */
	private int detailHeight()
	{
		return detail.getPreferredSize().height;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(0, Theme.ROW_HEIGHT + Math.round(detailHeight() * openness));
	}

	@Override
	public void doLayout()
	{
		// the detail card keeps its full height and is simply cut off while the row is shorter
		header.setBounds(0, 0, getWidth(), Theme.ROW_HEIGHT);
		detail.setBounds(0, Theme.ROW_HEIGHT, getWidth(), detailHeight());
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
