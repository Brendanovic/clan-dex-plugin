package com.clandex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;

/** Link handling, and reading each file format the clan's bot may publish. */
public class DexClientTest
{
	private static final String RAW = "https://gist.githubusercontent.com/owner/587c835cce6d317a0565f5c2f84ede7b/raw/dex.json";
	private static final String KEY = "abc123";
	private static final String NAMES = "\"names\":[\"First\",\"Second\",\"Third\"]";

	private final DexClient client = new DexClient();

	@Before
	public void giveClientGson() throws Exception
	{
		// RuneLite normally injects this
		Field gson = DexClient.class.getDeclaredField("gson");
		gson.setAccessible(true);
		gson.set(client, new Gson());
	}

	@Test
	public void gistPageLinkBecomesRawFileLink()
	{
		assertEquals(RAW, DexClient.toFileUrl("https://gist.github.com/owner/587c835cce6d317a0565f5c2f84ede7b"));
		assertEquals(RAW, DexClient.toFileUrl("  https://gist.github.com/owner/587c835cce6d317a0565f5c2f84ede7b/ "));
		assertEquals(RAW, DexClient.toFileUrl("https://gist.github.com/owner/587c835cce6d317a0565f5c2f84ede7b#file-dex-json"));
	}

	@Test
	public void otherLinksAreLeftAlone()
	{
		assertEquals(RAW, DexClient.toFileUrl(RAW));
		assertEquals("http://127.0.0.1:8765/dex.sample.json", DexClient.toFileUrl("http://127.0.0.1:8765/dex.sample.json"));
	}

	@Test
	public void readsFormat3AndIgnoresFieldsItDoesNotKnow()
	{
		SyncResult r = client.parse("{\"version\":3," + NAMES + ",\"players\":{\"" + KEY + "\":"
			+ "{\"rev\":4,\"dex\":{\"1\":2},\"bag\":{\"potion\":3},\"region\":\"caves\"}}}", KEY);
		assertEquals(null, r.getError());
		assertEquals(1, r.getPlayer().getCaught().size());
		assertEquals(Integer.valueOf(2), r.getPlayer().getCaught().get(1));
	}

	@Test
	public void stillReadsFormat2()
	{
		SyncResult r = client.parse("{\"version\":2," + NAMES + ",\"dex\":{\"" + KEY + "\":{\"0\":1}}}", KEY);
		assertEquals(Integer.valueOf(1), r.getPlayer().getCaught().get(0));
		assertTrue(r.getPlayer().getCatches().isEmpty());
	}

	@Test
	public void missingPlayerIsNoEntry()
	{
		assertTrue(client.parse("{\"version\":3," + NAMES + ",\"players\":{}}", KEY).isNoEntry());
	}

	@Test
	public void newerFormatAsksForAnUpdate()
	{
		assertEquals("Update the plugin to read the new dex", client.parse("{\"version\":4," + NAMES + "}", KEY).getError());
	}

	@Test
	public void readsCatchesBagShinyAndItems()
	{
		SyncResult r = client.parse("{\"version\":3," + NAMES
			+ ",\"sprite\":\"https://x/{n}.png\",\"spriteShiny\":\"https://x/shiny/{n}.png\""
			+ ",\"itemSprite\":\"http://not-https/{id}.png\""
			+ ",\"items\":{\"basic-orb\":{\"name\":\"Basic Orb\"},\"level-treat\":{\"name\":\"Level Treat\"}}"
			+ ",\"players\":{\"" + KEY + "\":{\"rev\":3,\"dex\":{\"1\":2}"
			+ ",\"mons\":["
			+ "{\"id\":\"aaaa111111\",\"no\":1,\"dex\":1,\"shiny\":true,\"gender\":\"female\",\"nature\":\"Adamant\",\"ability\":\"Venvenlo Quatumo\",\"hiddenAbility\":true,\"caughtAt\":\"2026-11-09T01:11:52.595Z\","
			+ "\"caughtWith\":\"basic-orb\",\"throws\":3,\"level\":14,\"ivs\":{\"atk\":2,\"def\":5,\"spe\":5,\"spc\":9},\"nickname\":\"later field\"},"
			+ "{\"id\":\"bbbb222222\",\"dex\":1,\"shiny\":false,\"caughtAt\":null}]"
			+ ",\"bag\":{\"level-treat\":1,\"basic-orb\":2,\"mystery\":1}}}}", KEY);

		DexInfo info = r.getInfo();
		assertEquals("https://x/shiny/{n}.png", info.getShinySpriteTemplate());
		assertEquals(null, info.getItemSpriteTemplate());           // not https, so not used
		assertEquals("Level Treat", info.itemName("level-treat"));
		assertEquals("Mystery", info.itemName("mystery"));          // unknown id made readable

		PlayerDex p = r.getPlayer();
		assertEquals(2, p.getCatches().size());
		assertEquals(1, p.shinyCount());
		Catch shiny = p.getCatches().get(0);
		assertTrue(shiny.isShiny());
		assertEquals(Integer.valueOf(14), shiny.getLevel());
		assertEquals(Integer.valueOf(1), shiny.getNumber());
		assertEquals("female", shiny.getGender());
		assertEquals("Adamant", shiny.getNature());
		assertEquals("Venvenlo Quatumo", shiny.getAbility());
		assertTrue(shiny.isHiddenAbility());
		assertEquals(Integer.valueOf(3), shiny.getThrowCount());
		assertEquals("basic-orb", shiny.getCaughtWith());
		assertEquals(9, shiny.getIvs()[3]);
		assertEquals(java.time.Instant.parse("2026-11-09T01:11:52.595Z"), shiny.getCaughtAt());
		Catch old = p.getCatches().get(1);
		assertEquals(null, old.getCaughtAt());
		assertEquals(null, old.getLevel());
		assertEquals(null, old.getIvs());
		assertEquals(null, old.getNumber());
		assertEquals(null, old.getGender());
		assertEquals(null, old.getNature());

		// bag follows the bot's item order, unknown items last; Basic Orbs are unlimited, so never counted
		assertEquals(java.util.Arrays.asList("level-treat", "mystery"), new java.util.ArrayList<>(p.getBag().keySet()));
	}
}
