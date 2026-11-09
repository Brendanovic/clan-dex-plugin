package com.clandex.ui;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Fetches entry images for the panel. The plugin supplies the implementation, so the UI never
 * touches the network itself.
 */
public interface SpriteLoader
{
	/**
	 * Loads the image for entry {@code index} from {@code urlTemplate}, where {@code {n}} stands
	 * for the 1-based entry number. {@code onLoaded} runs on the Swing thread, and is simply never
	 * called if the image can't be loaded, so the row stays without a picture.
	 */
	void load(String urlTemplate, int index, Consumer<BufferedImage> onLoaded);
}
