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
		assertEquals(1, r.getCaught().size());
		assertEquals(Integer.valueOf(2), r.getCaught().get(1));
	}

	@Test
	public void stillReadsFormat2()
	{
		SyncResult r = client.parse("{\"version\":2," + NAMES + ",\"dex\":{\"" + KEY + "\":{\"0\":1}}}", KEY);
		assertEquals(Integer.valueOf(1), r.getCaught().get(0));
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
}
