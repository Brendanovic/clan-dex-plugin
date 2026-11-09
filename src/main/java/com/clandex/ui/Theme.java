package com.clandex.ui;

import java.awt.Color;
import java.awt.Font;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Every colour and font the panel uses, in one place. Built on RuneLite's own palette so the
 * panel matches the rest of the sidebar; change the look here rather than in the components.
 */
final class Theme
{
	// Surfaces
	static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	static final Color ROW_CAUGHT = ColorScheme.DARKER_GRAY_COLOR;
	static final Color ROW_MISSING = new Color(0x2B2B2B);
	static final Color ROW_HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;

	// Text
	static final Color TEXT = Color.WHITE;
	static final Color TEXT_MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color TEXT_FAINT = ColorScheme.MEDIUM_GRAY_COLOR;
	static final Color ACCENT = ColorScheme.BRAND_ORANGE;
	static final Color ERROR = new Color(0xE06C5A);
	static final Color SHINY = new Color(0xF5C542);

	// Progress bar, loading skeleton and icons
	static final Color PROGRESS = ColorScheme.PROGRESS_COMPLETE_COLOR;
	static final Color PROGRESS_TRACK = ColorScheme.DARK_GRAY_COLOR;
	static final Color SKELETON = new Color(0x333333);
	static final Color SKELETON_SHINE = new Color(0x474747);
	static final Color ICON = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color ICON_HOVER = Color.WHITE;

	// Fonts
	static final Font TITLE = FontManager.getRunescapeBoldFont();
	static final Font BODY = FontManager.getRunescapeFont();
	static final Font SMALL = FontManager.getRunescapeSmallFont();
	static final Font BIG_NUMBER = FontManager.getRunescapeBoldFont().deriveFont(24f);

	// Spacing, in pixels
	static final int GAP = 8;
	static final int ROW_HEIGHT = 26;
	static final int SPRITE = 21;          // entry sprite in a list row
	static final int SPRITE_LARGE = 49;    // entry sprite in the expanded detail card
	static final int ITEM_SPRITE = 24;     // item picture in the bag

	// Animation, in milliseconds
	static final int ANIM_MS = 180;        // one row easing in or out
	static final int CASCADE_STEP_MS = 12; // delay between rows when the list cascades in
	static final int CASCADE_MAX_MS = 360; // so long lists don't take ages to settle
	static final int MIN_SKELETON_MS = 350; // keeps a fast sync from flashing the skeleton

	private Theme()
	{
	}
}
