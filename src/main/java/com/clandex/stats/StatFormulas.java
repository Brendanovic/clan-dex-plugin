package com.clandex.stats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every stat formula the plugin knows, by name. The bot's file says which one the clan uses; an
 * unknown or missing name falls back to {@link #DEFAULT}, so an older plugin still shows stats
 * (by the old rules) if the bot switches to a formula it doesn't have yet.
 */
public final class StatFormulas
{
	public static final StatFormula DEFAULT = new Gen1Formula();

	private static final Map<String, StatFormula> BY_NAME = new LinkedHashMap<>();

	static
	{
		register(DEFAULT);
		// register(new Gen3Formula());  <- future formulas go here
	}

	private StatFormulas()
	{
	}

	private static void register(StatFormula formula)
	{
		BY_NAME.put(formula.name(), formula);
	}

	/** The formula with this name, or {@link #DEFAULT} if the name is null or unknown. */
	public static StatFormula forName(String name)
	{
		return name == null ? DEFAULT : BY_NAME.getOrDefault(name, DEFAULT);
	}
}
