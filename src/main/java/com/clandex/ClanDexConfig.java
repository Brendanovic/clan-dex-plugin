package com.clandex;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(ClanDexConfig.GROUP)
public interface ClanDexConfig extends Config
{
	String GROUP = "clandex";

	/**
	 * Where our clan's bot publishes its dex file: a secret gist. Safe to have in public code,
	 * because the file holds no names (see CLAUDE.md, "Data contract").
	 */
	String DEFAULT_DATA_URL = "https://gist.githubusercontent.com/modiov2-web/587c835cce6d317a0565f5c2f84ede7b/raw/dex.json";

	/** Sidebar widths on offer. RuneLite's own panels are 225 pixels wide. */
	enum PanelWidth
	{
		STANDARD("Standard", 225),
		WIDE("Wide", 300),
		EXTRA_WIDE("Extra wide", 400);

		private final String label;
		private final int pixels;

		PanelWidth(String label, int pixels)
		{
			this.label = label;
			this.pixels = pixels;
		}

		public int getPixels()
		{
			return pixels;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "clanKey",
		name = "Clan key",
		description = "The key your clan shares with members. Without it the dex can't be read.",
		secret = true,
		position = 1
	)
	default String clanKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "panelWidth",
		name = "Panel width",
		description = "How wide the Clan Dex sidebar panel is",
		position = 2
	)
	default PanelWidth panelWidth()
	{
		return PanelWidth.STANDARD;
	}

	@ConfigItem(
		keyName = "playerName",
		name = "Name override",
		description = "Look up this name instead of the logged-in player (leave blank to use your own)",
		position = 3
	)
	default String playerName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "dataUrl",
		name = "Dex data URL",
		description = "Where your clan's bot publishes the dex. A gist's page link works too. Syncing downloads this file, so its host can see your IP address.",
		position = 4
	)
	default String dataUrl()
	{
		return DEFAULT_DATA_URL;
	}
}
