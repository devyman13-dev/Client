package blockradar;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Colors and shapes shared by the menus. Colors are ARGB. */
public final class Theme {
	private Theme() {}

	public static final int PANEL = 0xF0101319;
	public static final int ROW = 0xFF1A1F29;
	public static final int ROW_HOVER = 0xFF262D3B;
	public static final int ACCENT = 0xFF4C8DFF;
	public static final int TEXT = 0xFFF2F4F8;
	public static final int MUTED = 0xFF8A93A6;
	public static final int TRACK = 0xFF343C4C;

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

	/** An on/off switch, 18 wide and 10 tall. */
	public static void toggle(GuiGraphics g, int x, int y, boolean on) {
		box(g, x, y, 18, 10, on ? ACCENT : TRACK);
		box(g, on ? x + 9 : x + 1, y + 1, 8, 8, TEXT);
	}

	/** A thin bar showing a share from 0 to 1. */
	public static void bar(GuiGraphics g, int x, int y, int w, double share, int color) {
		g.fill(x, y, x + w, y + 2, TRACK);
		if (share > 0) g.fill(x, y, x + Math.max(1, (int) Math.round(w * Math.min(1, share))), y + 2, color);
	}
}
