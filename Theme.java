package blockradar;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Colors and shapes shared by the menus. Colors are ARGB. */
public final class Theme {
	private Theme() {}

	public static final int PANEL = 0xF00D1118;
	public static final int CARD = 0xFF131A24;
	public static final int ROW = 0xFF19222E;
	public static final int ROW_HOVER = 0xFF243243;
	public static final int ACCENT = 0xFF62A8FF;
	public static final int TEXT = 0xFFF2F4F8;
	public static final int MUTED = 0xFF9AA8BB;
	public static final int TRACK = 0xFF303C4D;

	/** A rounded rectangle with a modern, compact radius. */
	public static void box(GuiGraphics g, int x, int y, int w, int h, int color) {
		int radius = Math.min(5, Math.min(w / 2, h / 2));
		for (int row = 0; row < radius; row++) {
			int edge = radius - row - 1;
			int inset = (int) Math.ceil(radius - Math.sqrt(radius * radius - edge * edge));
			g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
			g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
		}
		g.fill(x, y + radius, x + w, y + h - radius, color);
	}

	public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x + 5, y, x + w - 5, y + 1, color);
		g.fill(x + 2, y + 1, x + w - 2, y + 2, color);
		g.fill(x, y + 4, x + 1, y + h - 4, color);
		g.fill(x + 1, y + 2, x + 2, y + h - 2, color);
		g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, color);
		g.fill(x + w - 1, y + 4, x + w, y + h - 4, color);
		g.fill(x + 5, y + h - 1, x + w - 5, y + h, color);
		g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, color);
	}

	public static void pill(GuiGraphics g, Font font, String text, int x, int y, int w, int color) {
		box(g, x, y, w, 18, color);
		centered(g, font, text, x + w / 2, y + 5, TEXT);
	}

	public static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, false);
	}

	public static void centered(GuiGraphics g, Font font, String text, int centerX, int y, int color) {
		text(g, font, text, centerX - font.width(text) / 2, y, color);
	}

	/** An on/off switch, 22 wide and 12 tall. */
	public static void toggle(GuiGraphics g, int x, int y, boolean on) {
		box(g, x, y, 22, 12, on ? ACCENT : TRACK);
		box(g, on ? x + 11 : x + 1, y + 2, 8, 8, TEXT);
	}

	/** A thin bar showing a share from 0 to 1. */
	public static void bar(GuiGraphics g, int x, int y, int w, double share, int color) {
		box(g, x, y, w, 3, TRACK);
		if (share > 0) box(g, x, y, Math.max(3, (int) Math.round(w * Math.min(1, share))), 3, color);
	}
}
