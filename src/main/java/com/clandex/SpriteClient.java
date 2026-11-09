package com.clandex;

import com.clandex.ui.SpriteLoader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads the images the clan's bot links to (entries, shinies, items), and keeps them in
 * memory for the rest of the session, so each image is fetched at most once.
 * Failures are only logged: the row just stays without a picture.
 */
@Slf4j
@Singleton
class SpriteClient implements SpriteLoader
{
	@Inject
	private OkHttpClient okHttpClient;

	private final Map<String, BufferedImage> cache = new ConcurrentHashMap<>();

	@Override
	public void load(String url, Consumer<BufferedImage> onLoaded)
	{
		BufferedImage cached = cache.get(url);
		if (cached != null)
		{
			SwingUtilities.invokeLater(() -> onLoaded.accept(cached));
			return;
		}

		HttpUrl httpUrl = HttpUrl.parse(url);
		if (httpUrl == null || !httpUrl.isHttps())
		{
			return;
		}

		okHttpClient.newCall(new Request.Builder().url(httpUrl).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Sprite {} failed to load", url, e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						return;
					}
					BufferedImage image;
					try (InputStream in = body.byteStream())
					{
						// RuneLite convention: ImageIO isn't safe to use from several threads at once
						synchronized (ImageIO.class)
						{
							image = ImageIO.read(in);
						}
					}
					if (image != null)
					{
						cache.put(url, image);
						SwingUtilities.invokeLater(() -> onLoaded.accept(image));
					}
				}
				catch (IOException e)
				{
					log.debug("Sprite {} couldn't be read", url, e);
				}
			}
		});
	}
}
