package com.clandex.ui;

import java.util.List;

/**
 * Where the player's party (up to 6 catch ids, in order) is kept. The plugin saves it in RuneLite's
 * settings, separately for each player; nothing is sent anywhere. When battles arrive, the party
 * will also need to reach the bot.
 */
public interface PartyStore
{
	int MAX_SIZE = 6;

	List<String> load();

	void save(List<String> catchIds);
}
