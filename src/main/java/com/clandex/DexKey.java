package com.clandex;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
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

	/** Decrypts to this when the clan key is right (see the bot's names.js). */
	static final String KEY_CHECK = "clan-dex-key-check";
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

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

	/**
	 * Decrypts a name from the bot's player list, or returns null if the clan key is wrong (or the
	 * value was changed). Mirrors decryptName() in the bot's names.js: AES-256-GCM with the key
	 * SHA-256("clan-dex-names|" + clanKey), value = base64(12-byte IV, ciphertext, 16-byte tag).
	 * GCM checks the tag, so a wrong key fails instead of producing scrambled text.
	 */
	static String decryptName(String clanKey, String value)
	{
		try
		{
			byte[] raw = Base64.getDecoder().decode(value);
			byte[] key = MessageDigest.getInstance("SHA-256").digest(("clan-dex-names|" + clanKey).getBytes(StandardCharsets.UTF_8));
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES));
			byte[] plain = cipher.doFinal(Arrays.copyOfRange(raw, IV_BYTES, raw.length));
			return new String(plain, StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException | IllegalArgumentException e)
		{
			return null;
		}
	}
}
