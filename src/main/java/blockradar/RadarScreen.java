package blockradar;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** The click menu: panels of switches and settings that can be dragged around. */
public class RadarScreen extends Screen implements Menu.Listener {
	private static final int FINDER = 0;
	private static final int RENDER = 1;
	private static final int SETTINGS = 2;
	private static final int RESULTS = 3;
	private static final String[] TITLES = {"Finder", "Render", "Settings", "Results"};
	private static final int GAP = 6;
	private static final int TOP = 24;

	/** Rows whose settings are unfolded. Remembered while the game runs. */
	private static final Set<String> EXPANDED = new HashSet<>();

	private Menu menu;
	private Scanner.Stats stats;
	private int ticks;

	public RadarScreen() {
		super(Component.literal("Block Radar"));
	}

	@Override
	protected void init() {
		stats = BlockRadarClient.SCANNER.stats();
		Menu old = menu;
		menu = new Menu(this.width, this.height, this);

		int side = Math.max(82, Math.min(108, (this.width - 130 - 5 * GAP) / 3));
		int results = Math.max(96, Math.min(160, this.width - 3 * side - 5 * GAP));
		int x = Math.max(0, (this.width - 3 * side - results - 3 * GAP) / 2);
		for (int id = 0; id < TITLES.length; id++) {
			int width = id == RESULTS ? results : side;
			// A position of -1 means the panel has never been moved.
			int panelX = Settings.panelX[id] < 0 ? x : Settings.panelX[id];
			int panelY = Settings.panelY[id] < 0 ? TOP : Settings.panelY[id];
			Menu.Panel panel = new Menu.Panel(id, TITLES[id], panelX, panelY, width, Settings.panelOpen[id]);
			panel.scrolls = id == RESULTS;
			menu.add(panel);
			x += width + GAP;
		}
		if (old != null) keepOrder(old);
		fill();
	}

	/** Keeps whichever panel was on top in front after a window resize. */
	private void keepOrder(Menu old) {
		for (Menu.Panel before : old.panels) {
			for (Menu.Panel now : List.copyOf(menu.panels)) {
				if (now.id != before.id) continue;
				menu.panels.remove(now);
				menu.panels.add(now);
			}
		}
	}

	/** Rebuilds the rows of every panel from the current settings and counts. */
	private void fill() {
		for (Menu.Panel panel : menu.panels) {
			panel.rows.clear();
			if (panel.id == FINDER) finder(panel.rows);
			else if (panel.id == RENDER) renderRows(panel.rows);
			else if (panel.id == SETTINGS) settings(panel.rows);
			else results(panel.rows);
		}
	}

	private static String shortName(Category category) {
		return switch (category) {
			case STORAGE -> "Storage";
			case PLACED -> "Placed";
			default -> category.label;
		};
	}

	private static String about(Category category) {
		return switch (category) {
			case REDSTONE -> "Redstone parts such as pistons, observers and dust";
			case STORAGE -> "Chests, barrels, ender chests and shulker boxes";
			case PLACED -> "Blocks that world generation does not place";
			case PICKED -> "The blocks you chose under Pick blocks";
		};
	}

	private void finder(List<Menu.Row> rows) {
		for (Category category : Category.values()) {
			int count = stats.byCategory().getOrDefault(category, 0);
			rows.add(new Menu.Row(shortName(category)).color(category.color).on(category.enabled)
				.detail(Format.compact(count)).help(about(category))
				.click(() -> {
					category.enabled = !category.enabled;
					BlockRadarClient.SCANNER.setFocus(null);
				}));
		}
		rows.add(new Menu.Row("Pick blocks...").help("Choose any blocks to look for")
			.click(() -> this.minecraft.setScreen(new BlockPickerScreen(this))));
	}

	private void renderRows(List<Menu.Row> rows) {
		rows.add(new Menu.Row("Radar").on(Settings.radarOn).help("Turns everything on or off (K)")
			.click(() -> {
				Settings.radarOn = !Settings.radarOn;
				BlockRadarClient.SCANNER.clear();
			}));

		boolean boxes = EXPANDED.contains("outlines");
		rows.add(new Menu.Row("Block ESP").on(Settings.outlines).help("Boxes around highlighted blocks")
			.click(() -> Settings.outlines = !Settings.outlines)
			.more(boxes, () -> expand("outlines")));
		if (boxes) {
			rows.add(new Menu.Row("Fill").setting().on(Settings.fill).detail(onOff(Settings.fill))
				.help("Tint the inside of each box (off is faster)")
				.click(() -> Settings.fill = !Settings.fill));
			rows.add(new Menu.Row("Max").setting().help("Most boxes drawn, nearest first (fewer is faster)")
				.slider(new Menu.Slider(() -> Settings.maxOutlines, v -> Settings.maxOutlines = v, 100, 2000, 50)));
		}

		boolean columns = EXPANDED.contains("chunks");
		rows.add(new Menu.Row("Chunks").on(Settings.chunks != ChunkStyle.OFF).color(BlockRadarClient.CHUNK_BLUE)
			.detail(Settings.chunks.name()).help("Blue chunks: solid, lines or off")
			.click(() -> Settings.chunks = Settings.chunks.next())
			.more(columns, () -> expand("chunks")));
		if (columns) {
			rows.add(new Menu.Row("Max").setting().help("Most chunks drawn, nearest first (fewer is faster)")
				.slider(new Menu.Slider(() -> Settings.maxChunks, v -> Settings.maxChunks = v, 8, 256, 8)));
		}

		rows.add(new Menu.Row("HUD").on(Settings.hud).help("Counts and FPS in the top left while playing")
			.click(() -> Settings.hud = !Settings.hud));
		rows.add(new Menu.Row("Click hints").on(Settings.hints).help("The \"left click to see\" message")
			.click(() -> Settings.hints = !Settings.hints));
	}

	private void settings(List<Menu.Row> rows) {
		Preset preset = Preset.current();
		rows.add(new Menu.Row("Preset").detail(preset == null ? "Custom" : preset.label)
			.help("Max FPS, Balanced or Quality")
			.click(() -> (preset == null ? Preset.BALANCED
				: Preset.values()[(preset.ordinal() + 1) % Preset.values().length]).apply()));
		rows.add(new Menu.Row("Scan").detail(" ms").help("Scan time per tick (lower is smoother)")
			.slider(new Menu.Slider(() -> Settings.scanMillis, v -> Settings.scanMillis = v, 1, 8, 1)));
		rows.add(new Menu.Row("Rescan").detail(" s").help("Pause between scans (higher is faster)")
			.slider(new Menu.Slider(() -> Settings.rescanSeconds, v -> Settings.rescanSeconds = v, 1, 15, 1)));
		rows.add(new Menu.Row("Color").detail(Settings.accent.label).color(Settings.accent.color)
			.help("Menu color").click(() -> Settings.accent = Settings.accent.next()));
		rows.add(new Menu.Row("Reset layout").help("Put the panels back where they started")
			.click(this::resetLayout));
	}

	private void results(List<Menu.Row> rows) {
		double share = (double) stats.suspicious() / Math.max(1, stats.blocks());
		rows.add(new Menu.Row("Suspicious").detail(Format.compact(stats.suspicious()))
			.help("Blocks flagged by the radar's own rules"));
		rows.add(new Menu.Row("Of all blocks").detail(Format.percent(stats.suspicious(), stats.blocks()))
			.bar(share).help("Share of all scanned blocks"));

		Block focus = BlockRadarClient.SCANNER.focus();
		rows.add(new Menu.Row("All blocks").on(focus == null).detail(Format.compact(stats.highlighted()))
			.help("Show every highlighted block")
			.click(() -> BlockRadarClient.SCANNER.setFocus(null)));
		for (Map.Entry<Block, Integer> entry : stats.byBlock()) {
			Block block = entry.getKey();
			int count = entry.getValue();
			rows.add(new Menu.Row(block.getName().getString()).on(focus == block)
				.color(stats.colors().getOrDefault(block, Theme.accent()))
				.detail(Format.compact(count))
				.bar((double) count / Math.max(1, stats.highlighted()))
				.help(Format.count(count) + " found, " + Format.percent(count, stats.highlighted())
					+ " of highlighted. Click to show only this.")
				.click(() -> BlockRadarClient.SCANNER.setFocus(block)));
		}
	}

	private static String onOff(boolean on) {
		return on ? "ON" : "OFF";
	}

	private static void expand(String name) {
		if (!EXPANDED.remove(name)) EXPANDED.add(name);
	}

	private void resetLayout() {
		for (int i = 0; i < Settings.panelOpen.length; i++) {
			Settings.panelX[i] = -1;
			Settings.panelY[i] = -1;
			Settings.panelOpen[i] = true;
		}
		menu = null;
		init();
	}

	@Override
	public void fold(Menu.Panel panel) {
		panel.open = !panel.open;
		Settings.panelOpen[panel.id] = panel.open;
		BlockRadarClient.saveSettings();
	}

	@Override
	public void moved(Menu.Panel panel) {
		Settings.panelX[panel.id] = panel.x;
		Settings.panelY[panel.id] = panel.y;
		BlockRadarClient.saveSettings();
	}

	@Override
	public void changed() {
		BlockRadarClient.saveSettings();
		BlockRadarClient.SCANNER.refresh();
		stats = BlockRadarClient.SCANNER.stats();
		fill();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return menu.press(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return menu.drag(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return menu.release() || super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		return menu.scroll(mouseX, mouseY, vertical)
			|| super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void tick() {
		// Keep the numbers up to date.
		if (++ticks % 10 != 0) return;
		stats = BlockRadarClient.SCANNER.stats();
		fill();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// A plain dim instead of the game's blur, which costs frames.
		graphics.fill(0, 0, this.width, this.height, Theme.DIM);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		Menu.Panel over = menu.panelAt(mouseX, mouseY);
		Menu.Row hovered = menu.rowAt(over, mouseY);
		for (Menu.Panel panel : menu.panels) draw(graphics, panel, hovered);

		String help = hovered != null && !hovered.help.isEmpty() ? hovered.help
			: "Left click: use  •  Right click: settings  •  Drag titles to move";
		Theme.centered(graphics, this.font, help, this.width / 2, this.height - 11, Theme.MUTED);
	}

	private void draw(GuiGraphics g, Menu.Panel panel, Menu.Row hovered) {
		int x = panel.x;
		int y = panel.y;
		int w = panel.width;
		int height = panel.height(this.height);
		int accent = Theme.accent();

		g.fill(x + 2, y + 2, x + w + 2, y + height + 3, Theme.SHADOW);
		g.fill(x, y, x + w, y + height + 1, Theme.PANEL);
		g.fill(x, y, x + w, y + Menu.HEADER, Theme.HEADER);
		g.fill(x + 2, y + 2, x + w - 2, y + 3, Theme.EDGE);
		g.fill(x + 8, y, x + w - 8, y + 2, accent);
		Theme.frame(g, x, y, w, height + 1, accent);
		Theme.centered(g, this.font, panel.title, x + w / 2, y + 6, Theme.TEXT);
		Theme.text(g, this.font, panel.open ? "-" : "+", x + w - 10, y + 6, Theme.MUTED);

		int first = panel.firstRow(this.height);
		for (int i = 0; i < panel.visibleRows(this.height); i++) {
			Menu.Row row = panel.rows.get(first + i);
			drawRow(g, row, x, y + Menu.HEADER + i * Menu.ROW, w, row == hovered);
		}
	}

	private void drawRow(GuiGraphics g, Menu.Row row, int x, int y, int w, boolean hover) {
		int accent = Theme.accent();
		if (row.setting) g.fill(x, y, x + w, y + Menu.ROW, Theme.SETTING);
		g.fill(x, y, x + w, y + Menu.ROW, hover ? Theme.ROW_HOVER : Theme.ROW_BG);
		g.fill(x + 5, y + Menu.ROW - 1, x + w - 5, y + Menu.ROW, Theme.GRID_LINE);
		if (row.on && !row.setting) {
			g.fill(x, y, x + w, y + Menu.ROW, Theme.alpha(accent, 0x2E));
			g.fill(x, y, x + 2, y + Menu.ROW, accent);
		}

		int left = x + (row.setting ? 12 : 7);
		if (row.color != 0) {
			g.fill(left, y + 6, left + 3, y + 9, row.color);
			left += 6;
		}

		int right = x + w - 5;
		if (row.more != null) {
			Theme.text(g, this.font, row.expanded ? "-" : "+", right - 5, y + 4, Theme.MUTED);
			right -= 11;
		}
		String detail = row.slider != null ? row.slider.get().getAsInt() + row.detail : row.detail;
		if (!detail.isEmpty()) {
			right -= this.font.width(detail);
			Theme.text(g, this.font, detail, right, y + 4, row.setting && row.on ? accent : Theme.MUTED);
			right -= 3;
		}

		String label = this.font.plainSubstrByWidth(row.label, Math.max(0, right - left));
		Theme.text(g, this.font, label, left, y + 4, row.on || hover ? Theme.TEXT : Theme.MUTED);

		int track = w - 2 * Menu.SLIDER_PAD;
		if (row.slider != null) {
			Theme.bar(g, x + Menu.SLIDER_PAD, y + Menu.ROW - 2, track, row.slider.share(), accent);
		} else if (row.bar >= 0) {
			Theme.bar(g, x + Menu.SLIDER_PAD, y + Menu.ROW - 2, track, row.bar,
				row.color != 0 ? row.color : accent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false; // keep scanning while the menu is open
	}
}
