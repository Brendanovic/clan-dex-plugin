package com.clandex;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Guards the one thing that must match the bot exactly: how a name becomes a published key.
 * The expected hash was produced by the bot's own {@code playerKey('test-key', 'osrs:fake test player')}
 * in publish.js. If this test fails, members won't find their dex.
 */
public class DexKeyTest
{
	@Test
	public void playerIdMatchesTheBotsRelayRules()
	{
		assertEquals("osrs:fake test player", DexKey.playerId("Fake\u00A0Test\u00A0Player")); // OSRS non-breaking spaces
		assertEquals("osrs:fake test player", DexKey.playerId("  Fake Test Player "));
		assertEquals("osrs:iron-guy", DexKey.playerId("Iron-Guy"));
		assertEquals("osrs:abc", DexKey.playerId("A_b.c!"));             // symbols dropped, like the bot
	}

	@Test
	public void keyMatchesTheBotsHmac()
	{
		assertEquals(
			"7bd615e18c244ab085d8cc1fa97d74773cad52aeedf95fea11ce5f2de4e6dd3c",
			DexKey.forPlayer("test-key", "Fake Test Player"));
	}
}
