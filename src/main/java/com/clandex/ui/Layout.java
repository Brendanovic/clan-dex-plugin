package com.clandex.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;

/** Small Swing helpers shared by the panel's components, to keep layout code short. */
final class Layout
{
	private Layout()
	{
	}

	/**
	 * Lets a component fill the width of a vertical BoxLayout while keeping its own height.
	 * Without this, BoxLayout centres narrow components or stretches them tall.
	 */
	static <T extends JComponent> T fullWidth(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
		return component;
	}

	/** Fixed vertical space between stacked components. */
	static Component vgap(int pixels)
	{
		return Box.createVerticalStrut(pixels);
	}

	static JLabel label(String text, Font font, Color color)
	{
		return style(new JLabel(text), font, color);
	}

	static JLabel style(JLabel label, Font font, Color color)
	{
		label.setFont(font);
		label.setForeground(color);
		return label;
	}
}
