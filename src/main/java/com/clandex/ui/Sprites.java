package com.clandex.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/**
 * Prepares entry sprites for display: trims their empty margins, scales them to row or
 * detail-card size, and makes the dark silhouette shown for entries not caught yet.
 *
 * <p>The sprites themselves come from the link pattern in the clan bot's file. If the bot sends
 * none, entries show their names only; the plugin has no pictures of its own.
 */
final class Sprites
{
	private static final Color SILHOUETTE = new Color(0x4A4A4A);

	/** Transparent stand-ins that hold a sprite's space while it downloads, so text doesn't jump. */
	static final ImageIcon PLACEHOLDER = blank(Theme.SPRITE);
	static final ImageIcon PLACEHOLDER_LARGE = blank(Theme.SPRITE_LARGE);

	private Sprites()
	{
	}

	/**
	 * Crops a sprite to its visible pixels. Sprites usually carry wide transparent margins,
	 * which would leave the picture tiny once scaled down to row size.
	 */
	static BufferedImage trim(BufferedImage sprite)
	{
		Rectangle bounds = null;
		for (int y = 0; y < sprite.getHeight(); y++)
		{
			for (int x = 0; x < sprite.getWidth(); x++)
			{
				if ((sprite.getRGB(x, y) >>> 24) > 0)
				{
					bounds = bounds == null ? new Rectangle(x, y, 1, 1) : bounds.union(new Rectangle(x, y, 1, 1));
				}
			}
		}
		return bounds == null ? sprite : sprite.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
	}

	/** A trimmed sprite scaled to fit {@code size} pixels square, centred; a flat silhouette if not caught. */
	static ImageIcon render(BufferedImage trimmed, int size, boolean caught)
	{
		double scale = Math.min(size / (double) trimmed.getWidth(), size / (double) trimmed.getHeight());
		int w = Math.max(1, (int) Math.round(trimmed.getWidth() * scale));
		int h = Math.max(1, (int) Math.round(trimmed.getHeight() * scale));

		BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		// area averaging keeps small sprites readable when shrunk, unlike plain bilinear
		g.drawImage(trimmed.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING), (size - w) / 2, (size - h) / 2, null);
		g.dispose();

		if (!caught)
		{
			// keep each pixel's transparency but paint it one flat colour: the outline stays, the details go
			int shadow = SILHOUETTE.getRGB() & 0xFFFFFF;
			for (int y = 0; y < size; y++)
			{
				for (int x = 0; x < size; x++)
				{
					int alpha = out.getRGB(x, y) >>> 24;
					out.setRGB(x, y, (alpha << 24) | shadow);
				}
			}
		}
		return new ImageIcon(out);
	}

	private static ImageIcon blank(int size)
	{
		return new ImageIcon(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB));
	}
}
