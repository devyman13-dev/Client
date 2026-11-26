package blockradar;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Colors and shapes shared by the menus. Colors are ARGB. */
public final class Theme {
	private Theme() {}

	public static final int PANEL = 0xF20D0F14;
	public static final int HEADER = 0xFF161A23;
	public static final int ROW_HOVER = 0xFF1D2230;
	public static final int SETTING = 0x70000000;
	public static final int SHADOW = 0x60000000;
	public static final int DIM = 0x90000000;
	public static final int HUD = 0xB00D0F14;
	public static final int TEXT = 0xFFF2F4F8;
	public static final int MUTED = 0xFF8A93A6;
	public static final int TRACK = 0xFF2A303E;

	/** The color chosen in the menu. */
	public static int accent() {
		return Settings.accent.color;
	}

	/** The same color with a different see-through amount, from 0 to 255. */
	public static int alpha(int color, int amount) {
		return (color & 0x00FFFFFF) | (amount << 24);
	}

	/** A rectangle with softened corners. */
	public static void box(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x + 2, y, x + w - 2, y + 1, color);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, color);
		g.fill(x, y + 2, x + w, y + h - 2, color);
		g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
		g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
	}

	public static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, false);
	}

	public static void centered(GuiGraphics g, Font font, String text, int centerX, int y, int color) {
		text(g, font, text, centerX - font.width(text) / 2, y, color);
	}

	/** A thin bar showing a share from 0 to 1. */
	public static void bar(GuiGraphics g, int x, int y, int w, double share, int color) {
		g.fill(x, y, x + w, y + 2, TRACK);
		if (share > 0) g.fill(x, y, x + Math.max(1, (int) Math.round(w * Math.min(1, share))), y + 2, color);
	}
}
