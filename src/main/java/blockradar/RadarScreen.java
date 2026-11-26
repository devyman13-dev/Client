package blockradar;

import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** The menu: radar switch, a category dropdown and a block dropdown. */
public class RadarScreen extends Screen {
	private enum Open { NONE, CATEGORIES, BLOCKS }

	private static final int PANEL_WIDTH = 270;
	private static final int ROW_WIDTH = 250;
	private static final int ROW = 20;

	private Open open = Open.NONE;
	private int scroll;
	private int maxScroll;
	private Scanner.Stats stats;
	private int ticks;

	public RadarScreen() {
		super(Component.literal("Block Radar"));
	}

	private FlatButton row(int x, int y, int w, String label, Runnable action) {
		return addRenderableWidget(new FlatButton(x, y, w, label, action));
	}

	private void show(Open next) {
		open = open == next ? Open.NONE : next;
		scroll = 0;
		rebuildWidgets();
	}

	@Override
	protected void init() {
		stats = BlockRadarClient.SCANNER.stats();
		maxScroll = 0;
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 50;

		row(x, y, ROW_WIDTH, "Radar", () -> {
			Settings.radarOn = !Settings.radarOn;
			BlockRadarClient.saveSettings();
			BlockRadarClient.SCANNER.clear();
			rebuildWidgets();
		}).toggle(Settings.radarOn);
		y += ROW;

		row(x, y, ROW_WIDTH, "Categories", () -> show(Open.CATEGORIES))
			.detail(open == Open.CATEGORIES ? "▲" : "▼");
		y += ROW;

		if (open == Open.CATEGORIES) {
			for (Category category : Category.values()) {
				int count = stats.byCategory().getOrDefault(category, 0);
				row(x + 12, y, ROW_WIDTH - 12, category.label, () -> {
					category.enabled = !category.enabled;
					BlockRadarClient.SCANNER.setFocus(null); // also saves the settings
					rebuildWidgets();
				}).dot(category.color).detail(Format.count(count)).toggle(category.enabled);
				y += ROW;
			}
		}

		Block focus = BlockRadarClient.SCANNER.focus();
		String selected = focus == null ? "All blocks" : focus.getName().getString();
		row(x, y, ROW_WIDTH, "Showing: " + selected, () -> show(Open.BLOCKS))
			.detail(open == Open.BLOCKS ? "▲" : "▼");
		y += ROW;

		if (open == Open.BLOCKS) addBlockList(x + 12, y, ROW_WIDTH - 12);

		row(x, this.height - 30, ROW_WIDTH, "Done", this::onClose).primary();
	}

	/** The scrollable part of the "Showing" dropdown. */
	private void addBlockList(int x, int y, int w) {
		List<Map.Entry<Block, Integer>> blocks = stats.byBlock();
		int visible = Math.max(1, (this.height - 34 - y) / ROW);
		int total = blocks.size() + 1; // + "All blocks"
		maxScroll = Math.max(0, total - visible);
		scroll = Math.max(0, Math.min(scroll, maxScroll));

		for (int i = scroll; i < Math.min(total, scroll + visible); i++) {
			if (i == 0) {
				row(x, y, w, "All blocks", () -> choose(null)).detail(Format.count(stats.suspicious()));
			} else {
				Block block = blocks.get(i - 1).getKey();
				int count = blocks.get(i - 1).getValue();
				row(x, y, w, block.getName().getString(), () -> choose(block))
					.detail(Format.count(count) + "  " + Format.percent(count, stats.suspicious()))
					.share((double) count / Math.max(1, stats.suspicious()));
			}
			y += ROW;
		}
	}

	private void choose(Block block) {
		BlockRadarClient.SCANNER.setFocus(block);
		show(Open.NONE);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (open == Open.BLOCKS && vertical != 0) {
			int next = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(vertical)));
			if (next != scroll) {
				scroll = next;
				rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void tick() {
		// Keep the numbers at the top up to date.
		if (++ticks % 10 == 0) stats = BlockRadarClient.SCANNER.stats();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.renderBackground(graphics, mouseX, mouseY, delta);
		int x = (this.width - PANEL_WIDTH) / 2;
		Theme.box(graphics, x, 6, PANEL_WIDTH, this.height - 12, Theme.PANEL);
		graphics.fill(x + 10, 6, x + PANEL_WIDTH - 10, 8, Theme.ACCENT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int middle = this.width / 2;
		Theme.centered(graphics, this.font, "BLOCK RADAR", middle, 15, Theme.TEXT);
		String summary = Format.count(stats.suspicious()) + " suspicious  •  "
			+ Format.percent(stats.suspicious(), stats.blocks()) + " of scanned blocks";
		Theme.centered(graphics, this.font, summary, middle, 29, Theme.MUTED);
		Theme.bar(graphics, middle - ROW_WIDTH / 2, 42, ROW_WIDTH,
			(double) stats.suspicious() / Math.max(1, stats.blocks()), Theme.ACCENT);
	}

	@Override
	public boolean isPauseScreen() {
		return false; // keep scanning while the menu is open
	}
}
