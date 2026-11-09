package com.clandex;

/**
 * The outcome of one sync: the game info plus this player's data, a "no entries yet" result, or an
 * error. Immutable, so it can be handed from the network thread to Swing safely.
 */
public final class SyncResult
{
	private final DexInfo info;      // null on error
	private final PlayerDex player;  // PlayerDex.EMPTY when not found or on error
	private final boolean noEntry;   // the file was read but this player isn't in it yet
	private final String error;      // a short message for the panel, or null

	private SyncResult(DexInfo info, PlayerDex player, boolean noEntry, String error)
	{
		this.info = info;
		this.player = player;
		this.noEntry = noEntry;
		this.error = error;
	}

	static SyncResult found(DexInfo info, PlayerDex player)
	{
		return new SyncResult(info, player, false, null);
	}

	static SyncResult noEntry(DexInfo info)
	{
		return new SyncResult(info, PlayerDex.EMPTY, true, null);
	}

	static SyncResult failed(String error)
	{
		return new SyncResult(null, PlayerDex.EMPTY, false, error);
	}

	public DexInfo getInfo()
	{
		return info;
	}

	public PlayerDex getPlayer()
	{
		return player;
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
