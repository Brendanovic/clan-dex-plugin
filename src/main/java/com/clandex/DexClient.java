package com.clandex;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
 * {@code dex} maps an entry index (0 = #001) to a catch count; every other field (the bot's
 * {@code rev}, and anything added later such as a bag) is ignored here, so the bot can grow the
 * format without breaking the plugin. {@code sprite} is optional; only https links are accepted.
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

		List<String> names = new ArrayList<>();
		for (JsonElement name : root.getAsJsonArray("names"))
		{
			names.add(name.getAsString());
		}

		String sprite = root.has("sprite") ? root.get("sprite").getAsString() : null;
		if (sprite != null && !sprite.startsWith("https://"))
		{
			sprite = null; // only ever load images over https
		}

		JsonObject entries = playerEntries(root, version, playerKey);
		if (entries == null)
		{
			return SyncResult.noEntry(names, sprite);
		}

		Map<Integer, Integer> caught = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : entries.entrySet())
		{
			int index = Integer.parseInt(entry.getKey());
			int count = entry.getValue().getAsInt();
			if (index >= 0 && index < names.size() && count > 0)
			{
				caught.put(index, count);
			}
		}
		return SyncResult.found(names, sprite, caught);
	}

	/** This player's { index: count } catches, or null if they aren't in the file. */
	private static JsonObject playerEntries(JsonObject root, int version, String playerKey)
	{
		if (version == PREVIOUS_VERSION)
		{
			JsonObject dex = root.getAsJsonObject("dex");
			return dex != null && dex.has(playerKey) ? dex.getAsJsonObject(playerKey) : null;
		}
		JsonObject players = root.getAsJsonObject("players");
		if (players == null || !players.has(playerKey))
		{
			return null;
		}
		JsonObject record = players.getAsJsonObject(playerKey);
		return record.has("dex") ? record.getAsJsonObject("dex") : new JsonObject();
	}
}
