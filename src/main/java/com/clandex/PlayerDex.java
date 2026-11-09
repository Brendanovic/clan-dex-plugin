package com.clandex;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One player's data from the bot's file: catch counts, each individual catch, and their bag. */
public final class PlayerDex
{
	static final PlayerDex EMPTY = new PlayerDex(Collections.emptyMap(), Collections.emptyList(), Collections.emptyMap());

	private final Map<Integer, Integer> caught; // entry index -> times caught
	private final List<Catch> catches;          // empty if the bot doesn't send them
	private final Map<String, Integer> bag;     // item id -> count, in the bot's item order

	PlayerDex(Map<Integer, Integer> caught, List<Catch> catches, Map<String, Integer> bag)
	{
		this.caught = Collections.unmodifiableMap(caught);
		this.catches = Collections.unmodifiableList(catches);
		this.bag = Collections.unmodifiableMap(new LinkedHashMap<>(bag));
	}

	public Map<Integer, Integer> getCaught()
	{
		return caught;
	}

	public List<Catch> getCatches()
	{
		return catches;
	}

	public Map<String, Integer> getBag()
	{
		return bag;
	}

	public long shinyCount()
	{
		return catches.stream().filter(Catch::isShiny).count();
	}
}
