package blockradar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** A floating module panel with category navigation and a color-coded block list. */
public class RadarScreen extends Screen {
	private static final int PANEL_WIDTH = 560;
	private static final int ROW_STEP = 26;
	private static final int BODY_OFFSET = 104;

	private Category selectedCategory = Category.REDSTONE;
	private int scroll;
	private int maxScroll;
	private Scanner.Stats stats;
	private int ticks;

	public RadarScreen() {
		super(Component.literal("Block Radar"));
	}

	private int panelWidth() { return Math.min(PANEL_WIDTH, Math.max(280, width - 24)); }
	private int panelHeight() { return Math.min(420, Math.max(220, height - 24)); }
	private int left() { return (width - panelWidth()) / 2; }
	private int top() { return (height - panelHeight()) / 2; }
	private int contentX() { return left() + 190; }
	private int contentWidth() { return panelWidth() - 208; }
	private int bodyTop() { return top() + BODY_OFFSET; }

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
		int navX = panelLeft + 14;
		int mainX = contentX();
		int y = bodyTop();

		row(navX, y, 162, "Radar", () -> {
			Settings.radarOn = !Settings.radarOn;
			BlockRadarClient.saveSettings();
			BlockRadarClient.SCANNER.clear();
			rebuildWidgets();
		}).dot(Theme.ACCENT).toggle(Settings.radarOn);
		y += 28;

		for (Category category : Category.values()) {
			int count = stats.byCategory().getOrDefault(category, 0);
			row(navX, y, 124, menuLabel(category), () -> selectCategory(category))
				.dot(category.color).detail(Format.count(count)).selected(selectedCategory == category);
			row(navX + 128, y, 32, "", () -> {
				category.enabled = !category.enabled;
				BlockRadarClient.saveSettings();
				stats = BlockRadarClient.SCANNER.stats();
				rebuildWidgets();
			}).toggle(category.enabled);
			y += ROW_STEP;
		}

		int contentY = bodyTop();
		row(mainX, contentY, contentWidth(), "Show all highlighted", () -> choose(null))
			.dot(Theme.ACCENT).detail(Format.count(stats.suspicious()))
			.selected(BlockRadarClient.SCANNER.focus() == null);
		contentY += ROW_STEP + 4;

		if (!selectedCategory.enabled) {
			maxScroll = 0;
		} else {
			List<Map.Entry<Block, Integer>> blocks = categoryBlocks();
			int footerTop = top() + panelHeight() - 40;
			int visible = Math.max(1, (footerTop - contentY) / ROW_STEP);
			maxScroll = Math.max(0, blocks.size() - visible);
			scroll = Math.max(0, Math.min(scroll, maxScroll));
			for (int i = scroll; i < Math.min(blocks.size(), scroll + visible); i++) {
				Map.Entry<Block, Integer> entry = blocks.get(i);
				Block block = entry.getKey();
				int count = entry.getValue();
				Category colorCategory = stats.blockCategories().getOrDefault(block, selectedCategory);
				row(mainX, contentY, contentWidth(), block.getName().getString(), () -> choose(block))
					.dot(colorCategory.color)
					.detail(Format.count(count) + "  " + Format.percent(count, stats.suspicious()))
					.share((double) count / Math.max(1, stats.suspicious()))
					.selected(BlockRadarClient.SCANNER.focus() == block);
				contentY += ROW_STEP;
			}
		}

		row(panelLeft + panelWidth() - 130, top() + panelHeight() - 34, 112, "Close", this::onClose).primary();
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
		stats = BlockRadarClient.SCANNER.stats();
		rebuildWidgets();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		int x = contentX();
		if (mouseX >= x && mouseX <= x + contentWidth() && mouseY >= bodyTop() && vertical != 0) {
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
		int y = top();
		int w = panelWidth();
		int h = panelHeight();
		Theme.box(graphics, x, y, w, h, Theme.PANEL);
		Theme.outline(graphics, x, y, w, h, 0x8855687E);
		Theme.box(graphics, x + 14, y + 98, 162, h - 144, Theme.CARD);
		Theme.box(graphics, x + 182, y + 98, w - 200, h - 144, Theme.CARD);
		graphics.fill(x + 18, y + 3, x + w - 18, y + 4, Theme.ACCENT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int panelLeft = left();
		int panelTop = top();
		int width = panelWidth();
		Theme.text(graphics, font, "BLOCK RADAR", panelLeft + 20, panelTop + 17, Theme.TEXT);
		Theme.text(graphics, font, "Live scan of nearby loaded chunks", panelLeft + 20, panelTop + 34,
			Theme.MUTED);
		Theme.pill(graphics, font, Settings.radarOn ? "●  ACTIVE" : "○  PAUSED",
			panelLeft + width - 116, panelTop + 18, 96, Settings.radarOn ? 0xFF1D493F : 0xFF3A414C);
		String summary = Format.count(stats.suspicious()) + " matches     "
			+ Format.percent(stats.suspicious(), stats.blocks()) + " of scanned blocks";
		Theme.text(graphics, font, summary, panelLeft + 20, panelTop + 56, Theme.TEXT);
		Theme.bar(graphics, panelLeft + 20, panelTop + 76, width - 40,
			(double) stats.suspicious() / Math.max(1, stats.blocks()), Theme.ACCENT);
		Theme.text(graphics, font, "CATEGORIES", panelLeft + 18, panelTop + 85, Theme.MUTED);
		Theme.text(graphics, font, selectedCategory.label.toUpperCase(), contentX(), panelTop + 85,
			selectedCategory.color);
		if (selectedCategory.enabled && categoryBlocks().isEmpty()) {
			Theme.text(graphics, font, "No blocks found in this category yet.", contentX() + 8, bodyTop() + 37,
				Theme.MUTED);
		} else if (!selectedCategory.enabled) {
			Theme.text(graphics, font, "Category is turned off.", contentX() + 8, bodyTop() + 37, Theme.MUTED);
		}
		String focus = "FILTER  ·  " + (BlockRadarClient.SCANNER.focus() == null ? "All highlighted blocks"
			: BlockRadarClient.SCANNER.focus().getName().getString());
		Theme.text(graphics, font, font.plainSubstrByWidth(focus, width - 170), panelLeft + 20,
			panelTop + panelHeight() - 26, Theme.MUTED);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
