package com.clandex;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The outcome of one sync: the entry list plus this player's catches, a "no entries yet"
 * result, or an error. Immutable, so it can be handed from the network thread to Swing safely.
 */
public final class SyncResult
{
	/** Entry names in dex order, as published by the bot. Empty on error. */
	private final List<String> names;

	/** Entry index (0 = #001) to how many times it was caught. Empty if nothing caught. */
	private final Map<Integer, Integer> caught;

	/** Link pattern for entry images, with {n} for the 1-based number. Null if the bot sends none. */
	private final String spriteTemplate;

	/** True when the file was read but this player isn't in it yet. */
	private final boolean noEntry;

	/** A short message for the panel when the sync failed, otherwise null. */
	private final String error;

	private SyncResult(List<String> names, String spriteTemplate, Map<Integer, Integer> caught, boolean noEntry, String error)
	{
		this.names = Collections.unmodifiableList(names);
		this.spriteTemplate = spriteTemplate;
		this.caught = Collections.unmodifiableMap(caught);
		this.noEntry = noEntry;
		this.error = error;
	}

	static SyncResult found(List<String> names, String spriteTemplate, Map<Integer, Integer> caught)
	{
		return new SyncResult(names, spriteTemplate, caught, false, null);
	}

	static SyncResult noEntry(List<String> names, String spriteTemplate)
	{
		return new SyncResult(names, spriteTemplate, Collections.emptyMap(), true, null);
	}

	static SyncResult failed(String error)
	{
		return new SyncResult(Collections.emptyList(), null, Collections.emptyMap(), false, error);
	}

	public List<String> getNames()
	{
		return names;
	}

	public String getSpriteTemplate()
	{
		return spriteTemplate;
	}

	public Map<Integer, Integer> getCaught()
	{
		return caught;
	}

	public boolean isNoEntry()
	{
		return noEntry;
	}

	public String getError()
	{
		return error;
	}

	public boolean isError()
	{
		return error != null;
	}
}
