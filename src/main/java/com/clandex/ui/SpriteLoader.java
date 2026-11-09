package com.clandex.ui;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Fetches images for the panel. The plugin supplies the implementation, so the UI never touches
 * the network itself.
 */
public interface SpriteLoader
{
	/**
	 * Loads the image at {@code url}. {@code onLoaded} runs on the Swing thread, and is simply never
	 * called if the image can't be loaded, so whatever asked for it stays without a picture.
	 */
	void load(String url, Consumer<BufferedImage> onLoaded);
}
