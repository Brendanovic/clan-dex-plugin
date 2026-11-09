package com.clandex.ui;

/** Fills in the picture link patterns the clan's bot publishes. */
final class Links
{
	private Links()
	{
	}

	/** An entry's picture: {n} is the 1-based entry number. */
	static String entry(String template, int index)
	{
		return template.replace("{n}", String.valueOf(index + 1));
	}

	/** An item's picture: {id} is the item id, e.g. great-orb. */
	static String item(String template, String id)
	{
		return template.replace("{id}", id);
	}
}
