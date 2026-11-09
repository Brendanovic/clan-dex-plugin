package com.clandex;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Works out which entry in the published file belongs to a player.
 *
 * <p>The bot never publishes names. It lists each player under
 * {@code HMAC-SHA256(clanKey, playerId)} in lower-case hex, so the file can't be turned back
 * into names, or checked against a guessed name, without the clan key. This class repeats the
 * same steps for the member's own name. It must stay identical to {@code playerKey()} in the
 * bot's {@code publish.js} and {@code parseRelay()} in its {@code index.js}.
 */
final class DexKey
{
	private static final String ALGORITHM = "HmacSHA256";

	private DexKey()
	{
	}

	/**
	 * The bot's id for an in-game name, e.g. "Fake Test Player" becomes "osrs:fake test player".
	 * OSRS uses non-breaking spaces in names, and the bot keeps only letters, digits, spaces
	 * and hyphens.
	 */
	static String playerId(String rsn)
	{
		String name = rsn.replace('\u00A0', ' ')
			.replaceAll("[^\\p{L}\\p{N} \\-]", "")
			.trim()
			.toLowerCase();
		return "osrs:" + name;
	}

	/** The key this player's catches are published under. */
	static String forPlayer(String clanKey, String rsn)
	{
		try
		{
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(clanKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
			byte[] digest = mac.doFinal(playerId(rsn).getBytes(StandardCharsets.UTF_8));

			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest)
			{
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (GeneralSecurityException e)
		{
			// HmacSHA256 ships with every Java runtime, so this can't happen in practice
			throw new IllegalStateException(e);
		}
	}
}
