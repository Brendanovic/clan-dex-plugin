package com.clandex;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads the file the clan's bot publishes and picks out one player's catches.
 *
 * <p>The file looks like this (see CLAUDE.md, "Data contract"):
 * <pre>
 * {
 *   "version": 3,
 *   "updated": "2026-11-08T20:00:00.000Z",
 *   "names": ["First entry", "Second entry", ...],
 *   "sprite": "https://example.com/sprites/{n}.png",
 *   "players": { "&lt;64 hex chars&gt;": { "rev": 7, "dex": { "0": 1, "24": 3 } } }
 * }
 * </pre>
 * Keys under {@code players} come from {@link DexKey#forPlayer}. In each player's record,
 * {@code dex} maps an entry index (0 = #001) to a catch count; {@code mons} lists each catch
 * (see {@link Catch}) and {@code bag} counts items by id. All of these, and the top-level
 * {@code spriteShiny}, {@code items} and {@code itemSprite}, are optional, and fields this
 * version doesn't know are ignored, so the bot can grow the format without breaking the plugin.
 * Picture links are only used if they are https.
 * Format 2 (a {@code dex} object mapping each key straight to its catches) is still read.
 */
@Slf4j
@Singleton
class DexClient
{
	/** The newest file layout this version understands. Bumped by both repos together. */
	private static final int SUPPORTED_VERSION = 3;

	/** The previous layout, still read so the plugin works with a bot that hasn't updated yet. */
	private static final int PREVIOUS_VERSION = 2;

	/** A gist's web page link, e.g. https://gist.github.com/owner/abc123. */
	private static final Pattern GIST_PAGE = Pattern.compile("https?://gist\\.github\\.com/([^/\\s]+)/([0-9a-fA-F]+)/?(#.*)?");

	// RuneLite supplies a shared, preconfigured client and Gson; plugins must not build their own.
	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	/**
	 * Fetches the file and reports the result for the player published under {@code playerKey}.
	 * {@code done} runs on OkHttp's thread, so callers must hop to Swing before touching the UI.
	 */
	void fetch(String url, String playerKey, Consumer<SyncResult> done)
	{
		HttpUrl httpUrl = HttpUrl.parse(toFileUrl(url));
		if (httpUrl == null)
		{
			done.accept(SyncResult.failed("The dex URL in settings isn't valid"));
			return;
		}

		Request request = new Request.Builder()
			.url(httpUrl)
			.header("Cache-Control", "no-cache")
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Dex fetch failed", e);
				done.accept(SyncResult.failed("Couldn't reach the dex server"));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						done.accept(SyncResult.failed("Dex server replied " + response.code()));
						return;
					}
					String text = body.string();
					if (text.trim().startsWith("<"))
					{
						done.accept(SyncResult.failed("That link is a web page, not the dex file"));
						return;
					}
					done.accept(parse(text, playerKey));
				}
				catch (Exception e)
				{
					log.debug("Dex parse failed", e);
					done.accept(SyncResult.failed("The dex file couldn't be read"));
				}
			}
		});
	}

	/**
	 * Turns a gist's page link into the link of its raw file, since the page link is the one
	 * people copy from the address bar. Any other link is returned as it is.
	 */
	static String toFileUrl(String url)
	{
		String trimmed = url.trim();
		Matcher page = GIST_PAGE.matcher(trimmed);
		if (!page.matches())
		{
			return trimmed;
		}
		return "https://gist.githubusercontent.com/" + page.group(1) + "/" + page.group(2) + "/raw/dex.json";
	}

	// package-private for DexClientTest
	SyncResult parse(String json, String playerKey)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);

		int version = root.has("version") ? root.get("version").getAsInt() : 0;
		if (version != SUPPORTED_VERSION && version != PREVIOUS_VERSION)
		{
			return SyncResult.failed(version > SUPPORTED_VERSION
				? "Update the plugin to read the new dex"
				: "The dex file is in an old format");
		}

		DexInfo info = readInfo(root);
		JsonObject record = playerRecord(root, version, playerKey);
		if (record == null)
		{
			return SyncResult.noEntry(info);
		}

		int entries = info.getNames().size();
		Map<Integer, Integer> caught = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : object(record, "dex").entrySet())
		{
			int index = Integer.parseInt(entry.getKey());
			int count = entry.getValue().getAsInt();
			if (index >= 0 && index < entries && count > 0)
			{
				caught.put(index, count);
			}
		}

		List<Catch> catches = new ArrayList<>();
		if (record.has("mons") && record.get("mons").isJsonArray())
		{
			for (JsonElement element : record.getAsJsonArray("mons"))
			{
				Catch c = readCatch(element.getAsJsonObject());
				if (c != null && c.getIndex() < entries)
				{
					catches.add(c);
				}
			}
		}

		// bag in the bot's item order, then any items it didn't list. Basic Orbs are unlimited, so
		// any count of them in the data is ignored; the Bag tab always shows them as unlimited.
		Map<String, Integer> bag = new LinkedHashMap<>();
		JsonObject rawBag = object(record, "bag");
		rawBag.remove(DexInfo.UNLIMITED_BALL);
		for (String id : info.getItemNames().keySet())
		{
			if (rawBag.has(id) && rawBag.get(id).getAsInt() > 0)
			{
				bag.put(id, rawBag.get(id).getAsInt());
			}
		}
		for (Map.Entry<String, JsonElement> entry : rawBag.entrySet())
		{
			if (!bag.containsKey(entry.getKey()) && entry.getValue().getAsInt() > 0)
			{
				bag.put(entry.getKey(), entry.getValue().getAsInt());
			}
		}

		return SyncResult.found(info, new PlayerDex(caught, catches, bag));
	}

	/** Names, picture links and item names from the top of the file. */
	private static DexInfo readInfo(JsonObject root)
	{
		List<String> names = new ArrayList<>();
		for (JsonElement name : root.getAsJsonArray("names"))
		{
			names.add(name.getAsString());
		}

		Map<String, String> itemNames = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> item : object(root, "items").entrySet())
		{
			JsonObject details = item.getValue().getAsJsonObject();
			itemNames.put(item.getKey(), details.has("name") ? details.get("name").getAsString() : item.getKey());
		}

		return new DexInfo(names, httpsLink(root, "sprite"), httpsLink(root, "spriteShiny"), httpsLink(root, "itemSprite"), itemNames);
	}

	/** One entry of a player's "mons" list, or null if it's unusable. Missing details stay null. */
	private static Catch readCatch(JsonObject mon)
	{
		if (!mon.has("dex"))
		{
			return null;
		}
		int[] ivs = null;
		if (mon.has("ivs") && mon.get("ivs").isJsonObject())
		{
			JsonObject raw = mon.getAsJsonObject("ivs");
			ivs = new int[]{intOr(raw, "atk", 0), intOr(raw, "def", 0), intOr(raw, "spe", 0), intOr(raw, "spc", 0)};
		}
		Instant caughtAt = null;
		if (mon.has("caughtAt") && !mon.get("caughtAt").isJsonNull())
		{
			try
			{
				caughtAt = Instant.parse(mon.get("caughtAt").getAsString());
			}
			catch (DateTimeParseException e)
			{
				// leave the date unknown rather than drop the catch
			}
		}
		return new Catch(
			mon.has("id") ? mon.get("id").getAsString() : "",
			mon.has("no") ? mon.get("no").getAsInt() : null,
			mon.get("dex").getAsInt(),
			mon.has("shiny") && mon.get("shiny").getAsBoolean(),
			text(mon, "gender"),
			text(mon, "nature"),
			text(mon, "ability"),
			mon.has("hiddenAbility") && mon.get("hiddenAbility").getAsBoolean(),
			caughtAt,
			mon.has("caughtWith") && !mon.get("caughtWith").isJsonNull() ? mon.get("caughtWith").getAsString() : null,
			mon.has("throws") ? mon.get("throws").getAsInt() : null,
			mon.has("level") ? mon.get("level").getAsInt() : null,
			ivs);
	}

	/**
	 * This player's record, or null if they aren't in the file. Format 2 stored the catches
	 * directly; it is wrapped as { dex: ... } so both read the same way.
	 */
	private static JsonObject playerRecord(JsonObject root, int version, String playerKey)
	{
		String section = version == PREVIOUS_VERSION ? "dex" : "players";
		JsonObject players = object(root, section);
		if (!players.has(playerKey))
		{
			return null;
		}
		JsonObject found = players.getAsJsonObject(playerKey);
		if (version == PREVIOUS_VERSION)
		{
			JsonObject wrapped = new JsonObject();
			wrapped.add("dex", found);
			return wrapped;
		}
		return found;
	}

	/** A child object, or an empty one if it's missing or not an object. */
	private static JsonObject object(JsonObject parent, String key)
	{
		return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();
	}

	/** A text field, or null if it's missing or null. */
	private static String text(JsonObject parent, String key)
	{
		return parent.has(key) && !parent.get(key).isJsonNull() ? parent.get(key).getAsString() : null;
	}

	private static int intOr(JsonObject parent, String key, int fallback)
	{
		return parent.has(key) ? parent.get(key).getAsInt() : fallback;
	}

	/** A link from the file, only if it is https (images are never loaded over plain http). */
	private static String httpsLink(JsonObject root, String key)
	{
		if (!root.has(key) || root.get(key).isJsonNull())
		{
			return null;
		}
		String link = root.get(key).getAsString();
		return link.startsWith("https://") ? link : null;
	}
}
