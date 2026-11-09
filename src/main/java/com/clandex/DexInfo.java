package com.clandex;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What the bot's file says about the game itself: entry names, picture links and item names. */
public final class DexInfo
{
	private final List<String> names;
	private final String spriteTemplate;       // {n} = 1-based entry number; null if none
	private final String shinySpriteTemplate;  // same, for shinies; null if none
	private final String itemSpriteTemplate;   // {id} = item id; null if none
	private final Map<String, String> itemNames; // item id -> name, in the bot's order
	private final Map<Integer, int[]> baseStats;  // entry index -> {hp, atk, def, spe, spc}
	private final Map<String, String> members;    // player key -> in-game name (decrypted with the clan key)
	private final String statFormula;             // which StatFormula the clan uses; null = default
	private final Map<String, String> unlimitedItems; // display.unlimitedItems: id -> name, shown as unlimited
	private final String entryTerm;               // display.terms.entry, e.g. "entry"
	private final String entriesTerm;             // display.terms.entries, e.g. "entries"

	DexInfo(List<String> names, String spriteTemplate, String shinySpriteTemplate, String itemSpriteTemplate, Map<String, String> itemNames,
		Map<Integer, int[]> baseStats, Map<String, String> members, String statFormula,
		Map<String, String> unlimitedItems, String entryTerm, String entriesTerm)
	{
		this.statFormula = statFormula;
		this.unlimitedItems = Collections.unmodifiableMap(new LinkedHashMap<>(unlimitedItems));
		this.entryTerm = entryTerm != null ? entryTerm : "entry";
		this.entriesTerm = entriesTerm != null ? entriesTerm : "entries";
		this.baseStats = Collections.unmodifiableMap(baseStats);
		this.members = Collections.unmodifiableMap(members);
		this.names = Collections.unmodifiableList(names);
		this.spriteTemplate = spriteTemplate;
		this.shinySpriteTemplate = shinySpriteTemplate;
		this.itemSpriteTemplate = itemSpriteTemplate;
		this.itemNames = Collections.unmodifiableMap(new LinkedHashMap<>(itemNames));
	}

	public List<String> getNames()
	{
		return names;
	}

	public String getSpriteTemplate()
	{
		return spriteTemplate;
	}

	public String getShinySpriteTemplate()
	{
		return shinySpriteTemplate;
	}

	public String getItemSpriteTemplate()
	{
		return itemSpriteTemplate;
	}

	public Map<String, String> getItemNames()
	{
		return itemNames;
	}

	/** Base stats {hp, atk, def, spe, spc} for an entry, or null if the bot doesn't send them. */
	public int[] baseStats(int index)
	{
		int[] stats = baseStats.get(index);
		return stats == null ? null : stats.clone();
	}

	/**
	 * Items every player has without limit (the bot's display.unlimitedItems), id to name, in the
	 * bot's order. Shown first in the Bag as unlimited; any count of them is ignored. Empty if the
	 * bot lists none.
	 */
	public Map<String, String> getUnlimitedItems()
	{
		return unlimitedItems;
	}

	/** The on-screen word for one entry (display.terms.entry), "entry" if the bot doesn't say. */
	public String entry()
	{
		return entryTerm;
	}

	/** The on-screen word for several entries (display.terms.entries), "entries" if the bot doesn't say. */
	public String entries()
	{
		return entriesTerm;
	}

	/** The name of the stat formula the clan's bot uses (see StatFormulas), or null for the default. */
	public String getStatFormula()
	{
		return statFormula;
	}

	/** Clan members by player key, names already decrypted. Empty if the bot sends no list. */
	public Map<String, String> getMembers()
	{
		return members;
	}

	/** An item's display name: the bot's name for it, or its id made readable ("great-orb" -> "Great orb"). */
	public String itemName(String id)
	{
		String name = unlimitedItems.containsKey(id) ? unlimitedItems.get(id) : itemNames.get(id);
		if (name != null)
		{
			return name;
		}
		String words = id.replace('-', ' ');
		return words.isEmpty() ? id : Character.toUpperCase(words.charAt(0)) + words.substring(1);
	}
}
