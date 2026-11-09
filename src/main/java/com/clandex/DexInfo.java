package com.clandex;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What the bot's file says about the game itself: entry names, picture links and item names. */
public final class DexInfo
{
	/** Item id of the free ball every player has without limit; never counted in a bag. */
	public static final String UNLIMITED_BALL = "basic-orb";

	private final List<String> names;
	private final String spriteTemplate;       // {n} = 1-based entry number; null if none
	private final String shinySpriteTemplate;  // same, for shinies; null if none
	private final String itemSpriteTemplate;   // {id} = item id; null if none
	private final Map<String, String> itemNames; // item id -> name, in the bot's order

	DexInfo(List<String> names, String spriteTemplate, String shinySpriteTemplate, String itemSpriteTemplate, Map<String, String> itemNames)
	{
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

	/** An item's display name: the bot's name for it, or its id made readable ("great-orb" -> "Great Orb"). */
	public String itemName(String id)
	{
		String name = itemNames.get(id);
		if (name != null)
		{
			return name;
		}
		String words = id.replace('-', ' ');
		return words.isEmpty() ? id : Character.toUpperCase(words.charAt(0)) + words.substring(1);
	}
}
