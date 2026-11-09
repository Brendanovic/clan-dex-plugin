package com.clandex.ui;

import com.clandex.SyncResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

/**
 * The sidebar panel. Top to bottom: title bar with a settings cog, summary card, Sync button
 * and status line, search box, All / Caught / Missing tabs, sort menu, then the list.
 *
 * <p>While a sync runs, the list is swapped for a shimmering {@link Skeleton}. When the data
 * arrives the rows cascade in, and filtering or sorting animates rows in and out rather than
 * leaving gaps (see {@link AnimatedList}).
 *
 * <p>Everything here runs on the Swing thread. The panel never fetches anything itself:
 * pressing Sync calls {@code onSync}, and the plugin later answers with {@link #showResult}
 * or {@link #showMessage}.
 */
public class ClanDexPanel extends PluginPanel
{
	/** Minimum time between syncs, so a busy clan can't hammer the data host. */
	private static final int COOLDOWN_SECONDS = 30;

	private static final String CARD_LIST = "list";
	private static final String CARD_SKELETON = "skeleton";

	private enum Filter
	{
		ALL("All"), CAUGHT("Caught"), MISSING("Missing");

		final String label;

		Filter(String label)
		{
			this.label = label;
		}
	}

	private enum Sort
	{
		NUMBER("Sort by number", Comparator.comparingInt(DexRow::getIndex)),
		NAME("Sort by name", Comparator.comparing(DexRow::getEntryName, String.CASE_INSENSITIVE_ORDER)),
		MOST_CAUGHT("Sort by most caught", Comparator.comparingInt(DexRow::getCount).reversed()
			.thenComparingInt(DexRow::getIndex));

		final String label;
		final Comparator<DexRow> order;

		Sort(String label, Comparator<DexRow> order)
		{
			this.label = label;
			this.order = order;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final Runnable onSync;
	private final Runnable onOpenSettings;
	private final SpriteLoader sprites;

	private final DexSummary summary = new DexSummary();
	private final JButton syncButton = new JButton("Sync");
	private final JLabel status = Layout.label(" ", Theme.SMALL, Theme.TEXT_MUTED);
	private final IconTextField search = new IconTextField();
	private final JLabel[] filterTabs = new JLabel[Filter.values().length];
	private final JComboBox<Sort> sortMenu = new JComboBox<>(Sort.values());

	private final CardLayout cards = new CardLayout();
	private final JPanel body = new JPanel(cards);
	private final AnimatedList list = new AnimatedList();
	private final JLabel listMessage = Layout.label("Press Sync to load your dex", Theme.SMALL, Theme.TEXT_FAINT);

	/** Ticks once a second to count down the cooldown and age the "Synced 2m ago" text. */
	private final Timer ticker = new Timer(1000, e -> refreshSyncControls());

	private List<DexRow> rows = Collections.emptyList();
	private List<String> names = Collections.emptyList();
	private String spriteTemplate;    // the link pattern sprites were last requested with
	private Filter filter = Filter.ALL;
	private boolean syncing;
	private Instant cooldownUntil = Instant.MIN;
	private Instant lastSynced;       // null until the first successful sync
	private Instant skeletonShownAt = Instant.MIN;
	private int panelWidth = PANEL_WIDTH;

	public ClanDexPanel(Runnable onSync, Runnable onOpenSettings, SpriteLoader sprites)
	{
		super(false); // we manage our own scrolling so the header stays put
		this.onSync = onSync;
		this.onOpenSettings = onOpenSettings;
		this.sprites = sprites;

		setLayout(new BorderLayout(0, Theme.GAP));
		setBackground(Theme.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		add(buildHeader(), BorderLayout.NORTH);
		add(buildBody(), BorderLayout.CENTER);

		refreshTabs();
		ticker.start();
	}

	/** Stops the background timer; call when the plugin shuts down. */
	public void stop()
	{
		ticker.stop();
	}

	/**
	 * Changes how wide the sidebar is. RuneLite sizes the sidebar from its panel's preferred
	 * width, so overriding {@link #getPreferredSize} is enough; the window then re-lays out.
	 */
	public void setPanelWidth(int pixels)
	{
		panelWidth = pixels;
		revalidate();
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null)
		{
			window.validate();
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(panelWidth, super.getPreferredSize().height);
	}

	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(panelWidth, super.getMinimumSize().height);
	}

	// ---------------------------------------------------------------- results from the plugin

	/** Shows the outcome of a sync started by {@code onSync}. */
	public void showResult(String playerName, SyncResult result)
	{
		syncing = false;
		summary.setPlayer(playerName);

		if (result.isError())
		{
			setStatus(result.getError(), true);
			refreshSyncControls();
			revealList(false);
			return;
		}

		if (!result.getNames().equals(names))
		{
			buildRows(result.getNames());
			spriteTemplate = null; // new rows need their pictures again
		}
		applyCatches(result.getCaught());
		requestSprites(result.getSpriteTemplate());

		lastSynced = Instant.now();
		setStatus(result.isNoEntry() ? "No entries yet - play in clan chat to start" : "Synced just now", false);
		refreshSyncControls();
		revealList(true);
	}

	/** Shows why a sync couldn't start (no key, not logged in). Clears the cooldown. */
	public void showMessage(String message)
	{
		syncing = false;
		cooldownUntil = Instant.MIN; // nothing was fetched, so don't make them wait
		setStatus(message, true);
		refreshSyncControls();
		revealList(false);
	}

	// ---------------------------------------------------------------- layout

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setOpaque(false);

		syncButton.setFocusPainted(false);
		syncButton.setPreferredSize(new Dimension(0, 30));
		syncButton.addActionListener(e -> requestSync());

		status.setHorizontalAlignment(SwingConstants.CENTER);

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(0, 30));
		search.setBackground(Theme.CARD);
		search.setHoverBackgroundColor(Theme.ROW_HOVER);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				applyFilter();
			}
		});

		sortMenu.setFocusable(false);
		sortMenu.setPreferredSize(new Dimension(0, 26));
		sortMenu.addActionListener(e -> applySort());

		header.add(Layout.fullWidth(buildTitleBar()));
		header.add(Layout.vgap(Theme.GAP));
		header.add(Layout.fullWidth(summary));
		header.add(Layout.vgap(Theme.GAP));
		header.add(Layout.fullWidth(syncButton));
		header.add(Layout.vgap(4));
		header.add(Layout.fullWidth(status));
		header.add(Layout.vgap(Theme.GAP));
		header.add(Layout.fullWidth(search));
		header.add(Layout.vgap(6));
		header.add(Layout.fullWidth(buildFilterTabs()));
		header.add(Layout.vgap(6));
		header.add(Layout.fullWidth(sortMenu));
		return header;
	}

	/** "Clan Dex" on the left, a settings cog on the right that opens this plugin's config. */
	private JPanel buildTitleBar()
	{
		JLabel cog = new JLabel(Icons.COG);
		cog.setToolTipText("Clan Dex settings");
		cog.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		cog.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				cog.setIcon(Icons.COG_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				cog.setIcon(Icons.COG);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				onOpenSettings.run();
			}
		});

		JPanel bar = new JPanel(new BorderLayout());
		bar.setOpaque(false);
		bar.add(Layout.label("Clan Dex", Theme.TITLE, Theme.TEXT), BorderLayout.WEST);
		bar.add(cog, BorderLayout.EAST);
		return bar;
	}

	/** Three flat tabs; the selected one is highlighted and underlined in the accent colour. */
	private JPanel buildFilterTabs()
	{
		JPanel tabs = new JPanel(new GridLayout(1, 0, 2, 0));
		tabs.setOpaque(false);
		tabs.setPreferredSize(new Dimension(0, 24));

		for (Filter f : Filter.values())
		{
			JLabel tab = Layout.label(f.label, Theme.SMALL, Theme.TEXT_MUTED);
			tab.setHorizontalAlignment(SwingConstants.CENTER);
			tab.setOpaque(true);
			tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			tab.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					filter = f;
					refreshTabs();
					applyFilter();
				}
			});
			filterTabs[f.ordinal()] = tab;
			tabs.add(tab);
		}
		return tabs;
	}

	/** The list and the loading skeleton share this space; a CardLayout swaps between them. */
	private JPanel buildBody()
	{
		listMessage.setHorizontalAlignment(SwingConstants.CENTER);
		listMessage.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

		// pin the list to the top, with the message underneath for empty results
		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(Theme.BACKGROUND);
		content.add(list, BorderLayout.NORTH);
		content.add(listMessage, BorderLayout.CENTER);

		JScrollPane scroll = new JScrollPane(content);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(Theme.ROW_HEIGHT);

		JPanel skeleton = new JPanel(new BorderLayout());
		skeleton.setOpaque(false);
		skeleton.add(new Skeleton(), BorderLayout.NORTH);

		body.setOpaque(false);
		body.add(scroll, CARD_LIST);
		body.add(skeleton, CARD_SKELETON);
		return body;
	}

	// ---------------------------------------------------------------- behaviour

	private void requestSync()
	{
		if (syncing || Instant.now().isBefore(cooldownUntil))
		{
			return;
		}
		syncing = true;
		cooldownUntil = Instant.now().plusSeconds(COOLDOWN_SECONDS);
		skeletonShownAt = Instant.now();
		cards.show(body, CARD_SKELETON);
		setStatus("Syncing...", false);
		refreshSyncControls();
		onSync.run();
	}

	/** Creates one row per entry. Only happens on the first sync, or if the bot's list changes. */
	private void buildRows(List<String> entryNames)
	{
		names = entryNames;
		rows = new ArrayList<>(entryNames.size());
		for (int i = 0; i < entryNames.size(); i++)
		{
			rows.add(new DexRow(i, entryNames.get(i)));
		}
		list.setRows(sorted());
	}

	/**
	 * Asks for every row's sprite, once per link pattern. With no pattern (the bot sends no
	 * sprites) rows show names only.
	 */
	private void requestSprites(String template)
	{
		if (Objects.equals(template, spriteTemplate))
		{
			return;
		}
		spriteTemplate = template;
		for (DexRow row : rows)
		{
			row.useSprites(template != null);
			if (template != null)
			{
				sprites.load(template, row.getIndex(), row::setSprite);
			}
		}
	}

	private void applyCatches(Map<Integer, Integer> caught)
	{
		int mostCaught = caught.entrySet().stream()
			.filter(e -> e.getValue() > 1)
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse(-1);

		for (DexRow row : rows)
		{
			row.collapse();
			row.setCount(caught.getOrDefault(row.getIndex(), 0), row.getIndex() == mostCaught);
		}
		summary.setCaught(caught, rows.size());
	}

	/**
	 * Swaps the skeleton out for the list. Waits until the skeleton has been up for a moment,
	 * so a fast sync doesn't flash, then cascades the rows in if {@code cascade} is set.
	 */
	private void revealList(boolean cascade)
	{
		long shownFor = Duration.between(skeletonShownAt, Instant.now()).toMillis();
		int wait = (int) Math.max(0, Theme.MIN_SKELETON_MS - shownFor);

		Timer reveal = new Timer(wait, e ->
		{
			cards.show(body, CARD_LIST);
			list.reorder(sorted()); // "most caught" order may have changed
			applyFilter();
			if (cascade)
			{
				list.cascadeIn();
			}
		});
		reveal.setRepeats(false);
		reveal.start();
	}

	/** Animates rows in or out to match the search box and the selected tab. */
	private void applyFilter()
	{
		String query = search.getText().trim().toLowerCase();
		for (DexRow row : rows)
		{
			boolean tabMatches = filter == Filter.ALL || (filter == Filter.CAUGHT) == row.isCaught();
			boolean show = tabMatches && row.matches(query);
			if (!show)
			{
				row.collapse();
			}
			list.setShown(row, show);
		}

		boolean empty = list.shownCount() == 0;
		listMessage.setText(rows.isEmpty() ? "Press Sync to load your dex" : "Nothing matches");
		listMessage.setVisible(empty);
	}

	/** Re-orders the list for the chosen sort, then lets the rows cascade into place. */
	private void applySort()
	{
		list.reorder(sorted());
		list.cascadeIn();
	}

	private List<DexRow> sorted()
	{
		List<DexRow> ordered = new ArrayList<>(rows);
		ordered.sort(((Sort) sortMenu.getSelectedItem()).order);
		return ordered;
	}

	private void refreshTabs()
	{
		for (Filter f : Filter.values())
		{
			boolean selected = f == filter;
			JLabel tab = filterTabs[f.ordinal()];
			tab.setBackground(selected ? Theme.CARD : Theme.BACKGROUND);
			tab.setForeground(selected ? Theme.TEXT : Theme.TEXT_MUTED);
			tab.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, selected ? Theme.ACCENT : Theme.BACKGROUND));
		}
	}

	/** Updates the Sync button for the cooldown, and ages the "Synced ..." text. */
	private void refreshSyncControls()
	{
		long wait = Duration.between(Instant.now(), cooldownUntil).getSeconds();

		syncButton.setEnabled(!syncing && wait <= 0);
		if (syncing)
		{
			syncButton.setText("Syncing...");
		}
		else if (wait > 0)
		{
			syncButton.setText("Sync again in " + wait + "s");
		}
		else
		{
			syncButton.setText("Sync");
		}

		// only age our own "Synced ..." text; leave errors and hints until the next sync
		if (!syncing && lastSynced != null && status.getText().startsWith("Synced"))
		{
			status.setText("Synced " + ago(lastSynced));
		}
	}

	private void setStatus(String text, boolean isProblem)
	{
		status.setText(text);
		status.setForeground(isProblem ? Theme.ERROR : Theme.TEXT_MUTED);
	}

	private static String ago(Instant then)
	{
		long seconds = Duration.between(then, Instant.now()).getSeconds();
		if (seconds < 60)
		{
			return "just now";
		}
		if (seconds < 3600)
		{
			return (seconds / 60) + "m ago";
		}
		return (seconds / 3600) + "h ago";
	}
}
