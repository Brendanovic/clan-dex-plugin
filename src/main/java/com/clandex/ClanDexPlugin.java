package com.clandex;

import com.clandex.ui.ClanDexPanel;
import com.clandex.ui.Icons;
import com.clandex.ui.PartyStore;
import com.google.inject.Provides;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;

/**
 * Entry point. Adds the Clan Dex button to the sidebar and runs a sync when the panel asks.
 *
 * <p>Threads matter in RuneLite, and a sync crosses all three:
 * <ol>
 *   <li>Swing thread: the Sync button is pressed and {@link #sync()} runs.</li>
 *   <li>Client thread: the logged-in player's name is read (game state is only safe here).</li>
 *   <li>OkHttp thread: {@link DexClient} downloads the file, then we hop back to Swing to draw.</li>
 * </ol>
 */
@Slf4j
@PluginDescriptor(
	name = "Clan Dex",
	description = "Shows your progress in your clan's Discord collecting game",
	tags = {"clan", "discord", "collection", "minigame"}
)
public class ClanDexPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private EventBus eventBus;

	@Inject
	private ClanDexConfig config;

	@Inject
	private DexClient dexClient;

	@Inject
	private SpriteClient spriteClient;

	@Inject
	private ConfigManager configManager;

	private ClanDexPanel panel;
	private NavigationButton navButton;

	/** Which player's party is showing: the start of their key, set at each sync. */
	private volatile String partyOwner;

	@Override
	protected void startUp()
	{
		panel = new ClanDexPanel(this::sync, this::openSettings, spriteClient, new SavedParty());
		panel.setPanelWidth(config.panelWidth().getPixels());
		navButton = NavigationButton.builder()
			.tooltip("Clan Dex")
			.icon(Icons.NAV)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel.stop();
		navButton = null;
		panel = null;
	}

	@Provides
	ClanDexConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanDexConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ClanDexConfig.GROUP.equals(event.getGroup()) && "panelWidth".equals(event.getKey()))
		{
			SwingUtilities.invokeLater(() -> panel.setPanelWidth(config.panelWidth().getPixels()));
		}
	}

	/**
	 * Opens this plugin's page in RuneLite's settings, for the panel's cog button.
	 * RuneLite has no direct call for that, but its config plugin opens a plugin's settings
	 * when an overlay's "Configure" menu entry is clicked. We send that same event, naming
	 * this plugin through a stand-in overlay that is never added to the screen.
	 */
	private void openSettings()
	{
		OverlayMenuEntry configure = new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", "Clan Dex");
		eventBus.post(new OverlayMenuClicked(configure, new SettingsLink(this)));
	}

	/** Called by the panel's Sync button, on the Swing thread. */
	private void sync()
	{
		String clanKey = config.clanKey().trim();
		if (clanKey.isEmpty())
		{
			panel.showMessage("Enter your clan key in settings");
			return;
		}

		String url = config.dataUrl().trim();
		if (url.isEmpty())
		{
			panel.showMessage("Set the Dex data URL in settings");
			return;
		}

		String override = config.playerName().trim();
		if (!override.isEmpty())
		{
			fetchFor(override, clanKey, url);
			return;
		}

		clientThread.invoke(() ->
		{
			Player me = client.getLocalPlayer();
			String name = client.getGameState() == GameState.LOGGED_IN && me != null ? me.getName() : null;
			if (name == null)
			{
				SwingUtilities.invokeLater(() -> panel.showMessage("Log in to sync your dex"));
				return;
			}
			fetchFor(name, clanKey, url);
		});
	}

	private void fetchFor(String name, String clanKey, String url)
	{
		String displayName = name.replace('\u00A0', ' '); // OSRS names use non-breaking spaces
		ClanDexPanel target = panel; // the plugin may shut down before the reply arrives
		String playerKey = DexKey.forPlayer(clanKey, name);
		partyOwner = playerKey.substring(0, 16); // each player keeps their own party
		dexClient.fetch(url, playerKey, clanKey, result ->
			SwingUtilities.invokeLater(() -> target.showResult(displayName, result)));
	}

	/**
	 * Keeps each player's party in RuneLite's settings (not shown on the settings page), under the
	 * start of their player key so a name never appears there. Nothing is sent anywhere.
	 */
	private final class SavedParty implements PartyStore
	{
		@Override
		public List<String> load()
		{
			String saved = partyOwner == null ? null : configManager.getConfiguration(ClanDexConfig.GROUP, "party_" + partyOwner);
			return saved == null || saved.isEmpty() ? Collections.emptyList() : Arrays.asList(saved.split(","));
		}

		@Override
		public void save(List<String> catchIds)
		{
			if (partyOwner != null)
			{
				configManager.setConfiguration(ClanDexConfig.GROUP, "party_" + partyOwner, String.join(",", catchIds));
			}
		}
	}

	/** Names this plugin in {@link #openSettings()}. Never added to the screen; draws nothing. */
	private static final class SettingsLink extends Overlay
	{
		SettingsLink(Plugin plugin)
		{
			super(plugin);
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			return null;
		}
	}
}
