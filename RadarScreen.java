package blockradar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** Mist-inspired module layout with a sidebar, search field, and large block cards. */
public class RadarScreen extends Screen {
	private static final int PANEL_WIDTH = 1040;
	private static final int BODY_OFFSET = 137;
	private static final int CARD_HEIGHT = 58;
	private static final int CARD_STEP = 66;
	private static final Set<Block> FAVORITES = new HashSet<>();
	private static final Map<Block, Category> PICKER_BLOCKS = pickerBlocks();

	private Category selectedCategory = Category.REDSTONE;
	private int scroll;
	private int maxScroll;
	private Scanner.Stats stats;
	private int ticks;
	private String searchQuery = "";
	private String pendingSearchQuery = "";
	private boolean searchDirty;
	private boolean favoritesOnly;
	private EditBox searchBox;

	public RadarScreen() {
		super(Component.literal("Block Radar"));
	}

	private int panelWidth() { return Math.min(PANEL_WIDTH, Math.max(300, width - 24)); }
	private int panelHeight() { return Math.min(680, Math.max(220, height - 24)); }
	private int left() { return (width - panelWidth()) / 2; }
	private int top() { return (height - panelHeight()) / 2; }
	private int navX() { return left() + 14; }
	private int contentX() { return left() + 212; }
	private int contentWidth() { return panelWidth() - 234; }
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
		int sideX = navX();
		int y = bodyTop();

		row(sideX, y, 162, "Base Finder", () -> {
			Settings.radarOn = !Settings.radarOn;
			BlockRadarClient.saveSettings();
			BlockRadarClient.SCANNER.clear();
			rebuildWidgets();
		}).dot(Theme.ACCENT).toggle(Settings.radarOn);

		int rangeY = y + 48;
		row(sideX, rangeY, 32, "-", () -> changeRange(-2)).primary();
		row(sideX + 36, rangeY, 90, Settings.scanRange + " chunks", () -> { });
		row(sideX + 130, rangeY, 32, "+", () -> changeRange(2)).primary();

		int categoryY = y + 92;
		for (Category category : Category.values()) {
			int count = stats.byCategory().getOrDefault(category, 0);
			row(sideX, categoryY, 124, menuLabel(category), () -> selectCategory(category))
				.dot(category.color).detail(Format.count(count)).selected(selectedCategory == category);
			row(sideX + 128, categoryY, 32, "", () -> {
				category.enabled = !category.enabled;
				BlockRadarClient.saveSettings();
			BlockRadarClient.SCANNER.refreshVisibility();
			stats = BlockRadarClient.SCANNER.stats();
				rebuildWidgets();
			}).toggle(category.enabled);
			categoryY += 28;
		}

		int mainX = contentX();
		FlatButton allCard = new FlatButton(mainX, y, contentWidth(), CARD_HEIGHT,
			"All highlighted blocks", () -> choose(null));
		allCard.dot(Theme.ACCENT).detail(Format.count(stats.suspicious())).toggle(BlockRadarClient.SCANNER.focus() == null)
			.description(stats.suspiciousChunks() + " suspicious chunks in the current scan radius")
			.selected(BlockRadarClient.SCANNER.focus() == null);
		addRenderableWidget(allCard);

		List<Map.Entry<Block, Integer>> blocks = categoryBlocks();
		int cardsY = y + CARD_STEP;
		int footerTop = top() + panelHeight() - 42;
		int visible = Math.max(1, (footerTop - cardsY) / CARD_STEP);
		maxScroll = Math.max(0, blocks.size() - visible);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		for (int i = scroll; i < Math.min(blocks.size(), scroll + visible); i++) {
			Map.Entry<Block, Integer> entry = blocks.get(i);
			Block block = entry.getKey();
			int count = entry.getValue();
			Category colorCategory = stats.blockCategories().getOrDefault(block, selectedCategory);
			boolean focused = BlockRadarClient.SCANNER.focus() == block;
			boolean watched = Settings.blockSelected(block);
			String description = focused
				? "Current outline focus · click the card to clear the focus filter"
				: count == 0 ? "No instances found in scanned chunks yet · toggle to watch this block"
				: Format.percent(count, stats.suspicious()) + " of selected matches · click card to focus";
			FlatButton card = new FlatButton(mainX, cardsY + (i - scroll) * CARD_STEP,
				contentWidth(), CARD_HEIGHT, block.getName().getString(), () -> choose(focused ? null : block));
			card.dot(colorCategory.color).detail(Format.count(count))
				.description(description).share((double) count / Math.max(1, stats.suspicious()));
			addRenderableWidget(card);
			row(mainX + contentWidth() - 82, cardsY + (i - scroll) * CARD_STEP + 18, 20, "",
				() -> toggleFavorite(block)).star(FAVORITES.contains(block)).selected(FAVORITES.contains(block));
			row(mainX + contentWidth() - 44, cardsY + (i - scroll) * CARD_STEP + 18, 28, "",
				() -> toggleBlock(block)).toggleControl(watched);
		}

		int searchWidth = Math.min(300, Math.max(150, panelWidth() - 440));
		int searchX = left() + panelWidth() - searchWidth - 20;
		searchBox = new EditBox(font, searchX, top() + 19, searchWidth, 27, Component.literal("Search blocks"));
		searchBox.setMaxLength(64);
		searchBox.setHint(Component.literal("Search blocks..."));
		searchBox.setValue(searchQuery);
		searchBox.setResponder(value -> {
			pendingSearchQuery = value;
			searchDirty = true;
		});
		addRenderableWidget(searchBox);

		row(left() + panelWidth() - 130, top() + panelHeight() - 34, 112, "Close", this::onClose).primary();
		if (contentWidth() >= 506) {
			row(mainX + 394, top() + 78, 112, favoritesOnly ? "★ Favorites" : "☆ Favorites",
				() -> { favoritesOnly = !favoritesOnly; scroll = 0; rebuildWidgets(); })
				.selected(favoritesOnly);
		}
	}

	private void changeRange(int delta) {
		Settings.scanRange = Math.max(2, Math.min(32, Settings.scanRange + delta));
		BlockRadarClient.saveSettings();
		BlockRadarClient.SCANNER.clear();
		rebuildWidgets();
	}

	private void toggleBlock(Block block) {
		Settings.setBlockSelected(block, !Settings.blockSelected(block));
		BlockRadarClient.saveSettings();
		BlockRadarClient.SCANNER.refreshVisibility();
		stats = BlockRadarClient.SCANNER.stats();
		rebuildWidgets();
	}

	private List<Map.Entry<Block, Integer>> categoryBlocks() {
		Map<Block, Integer> discovered = new java.util.LinkedHashMap<>();
		for (Map.Entry<Block, Integer> entry : stats.byBlock()) {
			if (stats.blockCategories().get(entry.getKey()) == selectedCategory) {
				discovered.put(entry.getKey(), entry.getValue());
			}
		}
		for (Map.Entry<Block, Category> entry : PICKER_BLOCKS.entrySet()) {
			if (entry.getValue() == selectedCategory) discovered.putIfAbsent(entry.getKey(), 0);
		}
		List<Map.Entry<Block, Integer>> filtered = new ArrayList<>(discovered.entrySet());
		filtered.sort((first, second) -> {
			int byCount = Integer.compare(second.getValue(), first.getValue());
			return byCount != 0 ? byCount
				: first.getKey().getName().getString().compareToIgnoreCase(second.getKey().getName().getString());
		});
		String query = searchQuery.toLowerCase(Locale.ROOT).trim();
		filtered.removeIf(entry -> (favoritesOnly && !FAVORITES.contains(entry.getKey()))
			|| (!query.isEmpty() && !entry.getKey().getName().getString().toLowerCase(Locale.ROOT).contains(query)));
		return filtered;
	}

	private static Map<Block, Category> pickerBlocks() {
		Map<Block, Category> blocks = new java.util.LinkedHashMap<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
			Category category = Rules.catalogCategory(path);
			if (category != null) blocks.put(block, category);
		}
		return Map.copyOf(blocks);
	}

	private void toggleFavorite(Block block) {
		if (!FAVORITES.add(block)) FAVORITES.remove(block);
		rebuildWidgets();
	}

	private static String menuLabel(Category category) {
		return switch (category) {
			case REDSTONE -> "Redstone";
			case WRONG_DIMENSION -> "Dimension";
			case WRONG_BIOME -> "Biome";
			case PLACED -> "Base";
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
		if (searchDirty) {
			searchDirty = false;
			if (!searchQuery.equals(pendingSearchQuery)) {
				searchQuery = pendingSearchQuery;
				scroll = 0;
				rebuildWidgets();
				if (searchBox != null) searchBox.setFocused(true);
			}
		}
		if (++ticks % 10 == 0) {
			stats = BlockRadarClient.SCANNER.stats();
			boolean searchFocused = searchBox != null && searchBox.isFocused();
			rebuildWidgets();
			if (searchFocused && searchBox != null) searchBox.setFocused(true);
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
		Theme.outline(graphics, x, y, w, h, 0x99647583);
		Theme.box(graphics, x + 10, y + 10, 180, h - 20, Theme.CARD);
		Theme.box(graphics, x + 198, y + 10, w - 208, h - 20, Theme.CARD);
		graphics.fill(x + 14, y + 3, x + w - 14, y + 4, Theme.ACCENT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int panelLeft = left();
		int panelTop = top();
		int width = panelWidth();
		int mainX = contentX();
		Theme.text(graphics, font, "BLOCK RADAR", panelLeft + 24, panelTop + 18, Theme.TEXT);
		Theme.text(graphics, font, "MODULES", panelLeft + 24, panelTop + 43, Theme.MUTED);
		String heading = selectedCategory.label + " Modules";
		Theme.text(graphics, font, font.plainSubstrByWidth(heading, Math.max(20, width - 550)),
			mainX, panelTop + 20, selectedCategory.color);
		Theme.text(graphics, font, "Click a card to focus its block outlines; toggle it again to clear the filter.",
			mainX, panelTop + 48, Theme.MUTED);
		Theme.pill(graphics, font, Format.count(stats.suspicious()) + " MATCHES", mainX, panelTop + 78,
			116, 0xFF243342);
		Theme.pill(graphics, font, stats.suspiciousChunks() + " CHUNKS", mainX + 124, panelTop + 78,
			112, 0xFF243342);
		Theme.pill(graphics, font, Settings.scanRange + " CHUNK RANGE", mainX + 244, panelTop + 78,
			140, 0xFF243342);
		Theme.text(graphics, font, "SCAN RANGE", navX(), bodyTop() + 31, Theme.MUTED);
		Theme.text(graphics, font, "BLOCK CATEGORIES", navX(), bodyTop() + 75, Theme.MUTED);
		String focus = "FILTER  ·  " + (BlockRadarClient.SCANNER.focus() == null ? "All highlighted blocks"
			: BlockRadarClient.SCANNER.focus().getName().getString());
		Theme.text(graphics, font, font.plainSubstrByWidth(focus, width - 170), panelLeft + 22,
			panelTop + panelHeight() - 25, Theme.MUTED);
		if (selectedCategory.enabled && categoryBlocks().isEmpty()) {
			Theme.text(graphics, font, searchQuery.isEmpty() ? "No matches in this category yet."
				: "No blocks match this search.", mainX + 12, bodyTop() + 48, Theme.MUTED);
		} else if (!selectedCategory.enabled) {
			Theme.text(graphics, font, "This category is disabled.", mainX + 12, bodyTop() + 48, Theme.MUTED);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
