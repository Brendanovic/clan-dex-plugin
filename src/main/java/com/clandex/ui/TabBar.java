package com.clandex.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** A row of flat tabs; the selected one is highlighted and underlined in the accent colour. */
class TabBar extends JPanel
{
	private final JLabel[] tabs;
	private int selected;

	/** {@code onSelect} gets the index of the tab the user picks. */
	TabBar(String[] labels, IntConsumer onSelect)
	{
		super(new GridLayout(1, 0, 2, 0));
		setOpaque(false);
		setPreferredSize(new Dimension(0, 24));

		tabs = new JLabel[labels.length];
		for (int i = 0; i < labels.length; i++)
		{
			int index = i;
			JLabel tab = Layout.label(labels[i], Theme.SMALL, Theme.TEXT_MUTED);
			tab.setHorizontalAlignment(SwingConstants.CENTER);
			tab.setOpaque(true);
			tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			tab.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					select(index);
					onSelect.accept(index);
				}
			});
			tabs[i] = tab;
			add(tab);
		}
		select(0);
	}

	void select(int index)
	{
		selected = index;
		for (int i = 0; i < tabs.length; i++)
		{
			boolean on = i == selected;
			tabs[i].setBackground(on ? Theme.CARD : Theme.BACKGROUND);
			tabs[i].setForeground(on ? Theme.TEXT : Theme.TEXT_MUTED);
			tabs[i].setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, on ? Theme.ACCENT : Theme.BACKGROUND));
		}
	}
}
