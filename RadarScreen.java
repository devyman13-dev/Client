package blockradar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** A compact module-style panel with category navigation and a color-coded block list. */
public class RadarScreen extends Screen {
	private static final int PANEL_WIDTH = 448;
	private static final int ROW_WIDTH = 254;
	private static final int ROW = 21;
	private static final int LIST_TOP = 68;

	private Category selectedCategory = Category.REDSTONE;
	private int scroll;
	private int maxScroll;
	private Scanner.Stats stats;
	private int ticks;

	public RadarScreen() {
		super(Component.literal("Block Radar"));
	}

	private int left() { return (width - PANEL_WIDTH) / 2; }

	private FlatButton row(int x, int y, int w, String label, Runnable action) {
		return addRenderableWidget(new FlatButton(x, y, w, label, action));
	}

	private void selectCategory(Category category) {
		selectedCategory = category;
		scroll = 0;
		rebuildWidgets();
	}

	@Override
	protected void init() {
		stats = BlockRadarClient.SCANNER.stats();
		maxScroll = 0;
		int panelLeft = left();
		int navX = panelLeft + 12;
		int contentX = panelLeft + 182;
		int y = LIST_TOP;

		row(navX, y, 130, "Radar", () -> {
			Settings.radarOn = !Settings.radarOn;
			BlockRadarClient.saveSettings();
			BlockRadarClient.SCANNER.clear();
			rebuildWidgets();
		}).dot(Theme.ACCENT).toggle(Settings.radarOn);
		y += ROW + 7;

		for (Category category : Category.values()) {
			int count = stats.byCategory().getOrDefault(category, 0);
			row(navX, y, 124, menuLabel(category), () -> selectCategory(category))
				.dot(category.color).detail(Format.count(count)).selected(selectedCategory == category);
			row(navX + 128, y, 28, "", () -> {
				category.enabled = !category.enabled;
				BlockRadarClient.saveSettings();
				stats = BlockRadarClient.SCANNER.stats();
				rebuildWidgets();
			}).toggle(category.enabled);
			y += ROW;
		}

		int contentY = LIST_TOP;
		row(contentX, contentY, ROW_WIDTH, "Show all highlighted", () -> choose(null))
			.dot(Theme.ACCENT).detail(Format.count(stats.suspicious()))
			.selected(BlockRadarClient.SCANNER.focus() == null);
		contentY += ROW + 7;

		if (!selectedCategory.enabled) {
			maxScroll = 0;
		} else {
			List<Map.Entry<Block, Integer>> blocks = categoryBlocks();
			int visible = Math.max(1, (height - 39 - contentY) / ROW);
			maxScroll = Math.max(0, blocks.size() - visible);
			scroll = Math.max(0, Math.min(scroll, maxScroll));
			for (int i = scroll; i < Math.min(blocks.size(), scroll + visible); i++) {
				Map.Entry<Block, Integer> entry = blocks.get(i);
				Block block = entry.getKey();
				int count = entry.getValue();
				Category colorCategory = stats.blockCategories().getOrDefault(block, selectedCategory);
				String label = block.getName().getString();
				row(contentX, contentY, ROW_WIDTH, label, () -> choose(block))
					.dot(colorCategory.color)
					.detail(Format.count(count) + "  " + Format.percent(count, stats.suspicious()))
					.share((double) count / Math.max(1, stats.suspicious()))
					.selected(BlockRadarClient.SCANNER.focus() == block);
				contentY += ROW;
			}
		}

		row(panelLeft + PANEL_WIDTH - 132, height - 30, 120, "Done", this::onClose).primary();
	}

	private List<Map.Entry<Block, Integer>> categoryBlocks() {
		List<Map.Entry<Block, Integer>> filtered = new ArrayList<>();
		for (Map.Entry<Block, Integer> entry : stats.byBlock()) {
			if (stats.blockCategories().get(entry.getKey()) == selectedCategory) filtered.add(entry);
		}
		return filtered;
	}

	private static String menuLabel(Category category) {
		return switch (category) {
			case REDSTONE -> "Redstone";
			case WRONG_DIMENSION -> "Dimension";
			case WRONG_BIOME -> "Biome";
			case PLACED -> "Placed";
		};
	}

	private void choose(Block block) {
		BlockRadarClient.SCANNER.setFocus(block);
		showMenuAgain();
	}

	private void showMenuAgain() {
		stats = BlockRadarClient.SCANNER.stats();
		rebuildWidgets();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		int contentX = left() + 182;
		if (mouseX >= contentX && mouseX <= contentX + ROW_WIDTH && mouseY >= LIST_TOP && vertical != 0) {
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
		if (++ticks % 10 == 0) {
			stats = BlockRadarClient.SCANNER.stats();
			rebuildWidgets();
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.renderBackground(graphics, mouseX, mouseY, delta);
		int x = left();
		Theme.box(graphics, x, 6, PANEL_WIDTH, height - 12, Theme.PANEL);
		graphics.fill(x + 10, 6, x + PANEL_WIDTH - 10, 8, Theme.ACCENT);
		int separator = x + 171;
		graphics.fill(separator, 58, separator + 1, height - 42, 0xFF303744);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int panelLeft = left();
		Theme.centered(graphics, font, "BLOCK RADAR", width / 2, 15, Theme.TEXT);
		String summary = Format.count(stats.suspicious()) + " detected  ·  "
			+ Format.percent(stats.suspicious(), stats.blocks()) + " of scanned blocks";
		Theme.centered(graphics, font, summary, width / 2, 29, Theme.MUTED);
		Theme.bar(graphics, panelLeft + 12, 42, PANEL_WIDTH - 24,
			(double) stats.suspicious() / Math.max(1, stats.blocks()), Theme.ACCENT);
		Theme.text(graphics, font, "CATEGORIES", panelLeft + 14, 56, Theme.MUTED);
		Theme.text(graphics, font, selectedCategory.label.toUpperCase(), panelLeft + 182, 56,
			selectedCategory.color);
		if (selectedCategory.enabled && categoryBlocks().isEmpty()) {
			Theme.text(graphics, font, "No blocks found in this category yet.", panelLeft + 188, LIST_TOP + 28,
				Theme.MUTED);
		} else if (!selectedCategory.enabled) {
			Theme.text(graphics, font, "Category is turned off.", panelLeft + 188, LIST_TOP + 28, Theme.MUTED);
		}
		String focus = "Showing: " + (BlockRadarClient.SCANNER.focus() == null ? "All blocks"
			: BlockRadarClient.SCANNER.focus().getName().getString());
		Theme.text(graphics, font, font.plainSubstrByWidth(focus, PANEL_WIDTH - 28), panelLeft + 14,
			height - 42, Theme.MUTED);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
