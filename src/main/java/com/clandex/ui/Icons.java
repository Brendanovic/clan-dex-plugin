package com.clandex.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/**
 * The plugin's icons, drawn in code so it ships no image files and uses no one else's artwork.
 * Shapes are drawn on a 16-unit grid and scaled.
 */
public final class Icons
{
	// The ball's colours: deliberately teal and gold, not the classic red and white
	static final Color BALL_TOP = new Color(0x1FA39A);
	static final Color BALL_BOTTOM = new Color(0xF2EAD8);
	static final Color BALL_BUTTON = new Color(0xF0B429);
	static final Color BALL_LINE = new Color(0x1B1B1B);

	/** Sidebar button: a teal-and-cream ball with a gold button. */
	public static final BufferedImage NAV = ball(16);

	/** Header button that opens the plugin's settings. */
	static final ImageIcon COG = new ImageIcon(cog(14, Theme.ICON));
	static final ImageIcon COG_HOVER = new ImageIcon(cog(14, Theme.ICON_HOVER));

	private Icons()
	{
	}

	private static Graphics2D start(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.scale(img.getWidth() / 16.0, img.getHeight() / 16.0);
		return g;
	}

	private static BufferedImage ball(int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		g.setColor(BALL_TOP);
		g.fillArc(1, 1, 14, 14, 0, 180);
		g.setColor(BALL_BOTTOM);
		g.fillArc(1, 1, 14, 14, 180, 180);

		g.setColor(BALL_LINE);
		g.setStroke(new BasicStroke(1.3f));
		g.drawOval(1, 1, 14, 14);
		g.drawLine(1, 8, 15, 8);

		g.setColor(BALL_BUTTON);
		g.fillOval(5, 5, 6, 6);
		g.setColor(BALL_LINE);
		g.drawOval(5, 5, 6, 6);
		g.dispose();
		return img;
	}

	private static BufferedImage cog(int size, Color color)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = start(img);
		g.setColor(color);

		// eight teeth around a ring, then a hole punched in the middle
		Path2D teeth = new Path2D.Double();
		for (int i = 0; i < 8; i++)
		{
			double angle = i * Math.PI / 4;
			double cx = 8 + Math.cos(angle) * 6.2;
			double cy = 8 + Math.sin(angle) * 6.2;
			teeth.append(new Ellipse2D.Double(cx - 1.7, cy - 1.7, 3.4, 3.4), false);
		}
		g.fill(teeth);
		g.fillOval(3, 3, 10, 10);
		g.setComposite(AlphaComposite.Clear);
		g.fillOval(5, 5, 6, 6);
		g.dispose();
		return img;
	}
}
