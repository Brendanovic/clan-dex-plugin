package com.clandex.ui;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * A list that animates rows in and out instead of leaving gaps.
 *
 * <p>Each row sits in a {@link Slot} with a "presence" between 0 (gone) and 1 (fully shown).
 * Hiding a row eases its presence to 0, which fades it and shrinks its height so the rows
 * below slide up; showing does the reverse. When the list is wide enough, rows flow into
 * several columns, so the same component works in a narrow or wide panel.
 */
class AnimatedList extends JPanel
{
	/** Narrowest a column may get before the list drops back to fewer columns. */
	private static final int MIN_COLUMN_WIDTH = 170;
	private static final int GAP = 2;
	private static final int FRAME_MS = 15;

	private final List<Slot> order = new ArrayList<>();
	private final Map<JComponent, Slot> slots = new IdentityHashMap<>();
	private final Timer animator = new Timer(FRAME_MS, e -> step());

	AnimatedList()
	{
		setOpaque(false);
		setLayout(new FlowingColumns());
	}

	/** Replaces every row. New rows start hidden; call {@link #setShown} or {@link #cascadeIn}. */
	void setRows(List<? extends JComponent> rows)
	{
		removeAll();
		order.clear();
		slots.clear();
		for (JComponent row : rows)
		{
			Slot slot = new Slot(row);
			slots.put(row, slot);
			order.add(slot);
			add(slot);
		}
		revalidate();
		repaint();
	}

	/** Puts the existing rows in a new order (for sorting). Their shown/hidden state is kept. */
	void reorder(List<? extends JComponent> rows)
	{
		order.clear();
		for (JComponent row : rows)
		{
			order.add(slots.get(row));
		}
		revalidate();
		repaint();
	}

	/** Animates one row in or out. */
	void setShown(JComponent row, boolean shown)
	{
		Slot slot = slots.get(row);
		slot.target = shown ? 1f : 0f;
		slot.delayMs = 0;
		animator.start();
	}

	/**
	 * Replays every shown row's entrance from the top, one after another. Used after a sync
	 * and after sorting, so the list visibly settles into its new shape.
	 */
	void cascadeIn()
	{
		int position = 0;
		for (Slot slot : order)
		{
			if (slot.target > 0)
			{
				slot.presence = 0f;
				slot.delayMs = Math.min(position++ * Theme.CASCADE_STEP_MS, Theme.CASCADE_MAX_MS);
			}
		}
		animator.start();
	}

	/** How many rows are shown, or on their way in. */
	int shownCount()
	{
		int count = 0;
		for (Slot slot : order)
		{
			if (slot.target > 0)
			{
				count++;
			}
		}
		return count;
	}

	/** Advances every animating slot by one frame; stops the timer once all have settled. */
	private void step()
	{
		float stepSize = FRAME_MS / (float) Theme.ANIM_MS;
		boolean moving = false;

		for (Slot slot : order)
		{
			if (slot.delayMs > 0)
			{
				slot.delayMs -= FRAME_MS;
				moving = true;
			}
			else if (slot.presence != slot.target)
			{
				float diff = slot.target - slot.presence;
				slot.presence = Math.abs(diff) <= stepSize ? slot.target : slot.presence + Math.signum(diff) * stepSize;
				moving = true;
			}
		}

		if (!moving)
		{
			animator.stop();
		}
		revalidate();
		repaint();
	}

	/** Ease-out curve: quick to start, gentle to land. */
	private static float ease(float t)
	{
		float inverse = 1 - t;
		return 1 - inverse * inverse * inverse;
	}

	/**
	 * Holds one row at its full height and clips it, so a shrinking slot hides the bottom of
	 * the row instead of squashing its contents. Also paints the row faded by its presence.
	 */
	private static final class Slot extends JPanel
	{
		private final JComponent row;
		private float presence;    // what is drawn now
		private float target;      // where it is heading
		private int delayMs;       // wait before moving, for the cascade

		Slot(JComponent row)
		{
			super(null);
			this.row = row;
			setOpaque(false);
			add(row);
		}

		int fullHeight()
		{
			return row.getPreferredSize().height;
		}

		int currentHeight()
		{
			return Math.round(fullHeight() * ease(presence));
		}

		@Override
		public void doLayout()
		{
			row.setBounds(0, 0, getWidth(), fullHeight());
		}

		@Override
		public void paint(Graphics g)
		{
			if (presence <= 0f)
			{
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, presence)));
			super.paint(g2);
			g2.dispose();
		}
	}

	/** Places slots left to right in as many columns as fit, each grid row as tall as its tallest slot. */
	private final class FlowingColumns implements LayoutManager
	{
		@Override
		public void layoutContainer(Container parent)
		{
			arrange(parent, parent.getWidth(), true);
		}

		@Override
		public Dimension preferredLayoutSize(Container parent)
		{
			// before the first layout the width is 0, so fall back to one column
			int width = parent.getWidth() > 0 ? parent.getWidth() : MIN_COLUMN_WIDTH;
			return new Dimension(width, arrange(parent, width, false));
		}

		@Override
		public Dimension minimumLayoutSize(Container parent)
		{
			return new Dimension(0, 0);
		}

		@Override
		public void addLayoutComponent(String name, Component comp)
		{
		}

		@Override
		public void removeLayoutComponent(Component comp)
		{
		}

		/** Works out (and optionally applies) every slot's bounds. Returns the total height. */
		private int arrange(Container parent, int width, boolean apply)
		{
			Insets in = parent.getInsets();
			int usable = Math.max(0, width - in.left - in.right);
			int columns = Math.max(1, (usable + GAP) / (MIN_COLUMN_WIDTH + GAP));
			int columnWidth = (usable - GAP * (columns - 1)) / columns;

			int y = in.top;
			int column = 0;
			int rowHeight = 0;

			for (Slot slot : order)
			{
				int height = slot.currentHeight();
				if (apply)
				{
					slot.setVisible(height > 0);
				}
				if (height <= 0)
				{
					continue;
				}
				if (apply)
				{
					slot.setBounds(in.left + column * (columnWidth + GAP), y, columnWidth, height);
				}
				rowHeight = Math.max(rowHeight, height);
				if (++column == columns)
				{
					y += rowHeight + GAP;
					column = 0;
					rowHeight = 0;
				}
			}
			return y + rowHeight + in.bottom;
		}
	}
}
