package com.clandex.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Placeholder rows with a soft shine sweeping across them, shown while a sync is running.
 * The timer only runs while the skeleton is on screen.
 */
class Skeleton extends JComponent
{
	private static final int ROWS = 12;
	private static final int SWEEP_MS = 1100;
	private static final int FRAME_MS = 30;

	private final Timer timer = new Timer(FRAME_MS, e -> repaint());
	private final long startedAt = System.currentTimeMillis();

	Skeleton()
	{
		setPreferredSize(new Dimension(0, ROWS * (Theme.ROW_HEIGHT + 2)));
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		timer.start();
	}

	@Override
	public void removeNotify()
	{
		timer.stop();
		super.removeNotify();
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible && isDisplayable())
		{
			timer.start();
		}
		else
		{
			timer.stop();
		}
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// the shine is a narrow light band moving left to right, repeating every SWEEP_MS
		int width = getWidth();
		float progress = ((System.currentTimeMillis() - startedAt) % SWEEP_MS) / (float) SWEEP_MS;
		int shineX = (int) (-width * 0.5 + progress * width * 2);
		g2.setPaint(new LinearGradientPaint(shineX - 60, 0, shineX + 60, 0,
			new float[]{0f, 0.5f, 1f},
			new Color[]{Theme.SKELETON, Theme.SKELETON_SHINE, Theme.SKELETON},
			MultipleGradientPaint.CycleMethod.NO_CYCLE));

		int rowHeight = Theme.ROW_HEIGHT;
		for (int i = 0; i < ROWS; i++)
		{
			int y = i * (rowHeight + 2);
			int nameWidth = 60 + (i * 37 % 70); // varied lengths read as text, not stripes
			g2.fillRoundRect(6, y + 3, rowHeight - 6, rowHeight - 6, 4, 4);  // avatar
			g2.fillRoundRect(rowHeight + 6, y + 8, 30, 8, 4, 4);            // number
			g2.fillRoundRect(rowHeight + 44, y + 8, nameWidth, 8, 4, 4);    // name
		}
		g2.dispose();
	}
}
