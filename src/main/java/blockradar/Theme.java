package blockradar;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Colors and shapes shared by the menus. Colors are ARGB. */
public final class Theme {
	private Theme() {}

	public static final int PANEL = 0xF20A111B;
	public static final int HEADER = 0xFF0C1C2B;
	public static final int ROW_BG = 0xA50C1924;
	public static final int ROW_HOVER = 0xFF123044;
	public static final int SETTING = 0xB20B1B28;
	public static final int SHADOW = 0xA0000000;
	public static final int DIM = 0xA800060D;
	public static final int HUD = 0xE6091420;
	public static final int TEXT = 0xFFE8F7FF;
	public static final int MUTED = 0xFF80A5B8;
	public static final int TRACK = 0xFF183244;
	public static final int EDGE = 0xFF24516A;
	public static final int GRID_LINE = 0x553B9DB8;

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

	/** Thin sci-fi frame with bright corner ticks around a panel. */
	public static void frame(GuiGraphics g, int x, int y, int w, int h, int accent) {
		if (w < 4 || h < 4) return;
		g.fill(x, y, x + w, y + 1, EDGE);
		g.fill(x, y + h - 1, x + w, y + h, EDGE);
		g.fill(x, y, x + 1, y + h, EDGE);
		g.fill(x + w - 1, y, x + w, y + h, EDGE);
		int tick = Math.min(9, Math.min(w / 3, h / 3));
		g.fill(x, y, x + tick, y + 2, accent);
		g.fill(x, y, x + 2, y + tick, accent);
		g.fill(x + w - tick, y + h - 2, x + w, y + h, accent);
		g.fill(x + w - 2, y + h - tick, x + w, y + h, accent);
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
