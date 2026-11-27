package blockradar;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** The small overlay in the top left corner while playing. */
public final class RadarHud {
	private RadarHud() {}

	private record Line(String name, String value, int color) {}

	/** The counts are only worked out once a second, to keep frames cheap. */
	private static final int UPDATE_TICKS = 20;

	private static List<Line> lines = List.of();
	private static int ticks;

	public static void tick() {
		if (!Settings.hud || !Settings.radarOn || ++ticks % UPDATE_TICKS != 0) return;
		Scanner.Stats stats = BlockRadarClient.SCANNER.stats();
		List<Line> next = new ArrayList<>();
		next.add(new Line("Suspicious", Format.compact(stats.suspicious()) + "  "
			+ Format.percent(stats.suspicious(), stats.blocks()), Theme.TEXT));
		for (Category category : Category.values()) {
			int count = stats.byCategory().getOrDefault(category, 0);
			if (category.enabled && count > 0) {
				next.add(new Line(category.label, Format.compact(count), category.color));
			}
		}
		lines = next;
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (Freecam.active() && client.player != null && client.screen == null && !client.options.hideGui) {
			Theme.centered(g, client.font, "FREECAM  " + Settings.freecamSpeed + " b/s",
				client.getWindow().getGuiScaledWidth() / 2, 6, Theme.accent());
		}
		if (!Settings.hud || !Settings.radarOn || client.player == null || client.screen != null
			|| client.options.hideGui || client.getDebugOverlay().showDebugScreen()) return;

		Font font = client.font;
		String fps = client.getFps() + " FPS";
		int width = Math.max(132, font.width("BLOCK RADAR") + font.width(fps) + 38);
		for (Line line : lines) width = Math.max(width, font.width(line.name()) + font.width(line.value()) + 30);

		int x = 4;
		int y = 4;
		int height = 20 + lines.size() * 12 + (lines.isEmpty() ? 0 : 2);
		g.fill(x, y, x + width, y + height, Theme.HUD);
		Theme.frame(g, x, y, width, height, Theme.accent());
		g.fill(x + 7, y + 6, x + 11, y + 10, 0xFF4AFFA8);
		Theme.text(g, font, "BLOCK RADAR", x + 16, y + 4, Theme.accent());
		Theme.text(g, font, fps, x + width - 8 - font.width(fps), y + 4, Theme.MUTED);
		g.fill(x + 7, y + 17, x + width - 7, y + 18, Theme.EDGE);
		int lineY = y + 22;
		for (Line line : lines) {
			Theme.text(g, font, line.name(), x + 8, lineY, line.color());
			Theme.text(g, font, line.value(), x + width - 8 - font.width(line.value()), lineY, Theme.TEXT);
			lineY += 12;
		}
	}
}
