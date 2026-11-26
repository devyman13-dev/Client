package blockradar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Searchable Xray-style grid of block icons. Click an icon to toggle that block. */
public class BlockPickerScreen extends Screen {
	private static final int TOP = 44;
	private static final int BOTTOM = 36;
	private static final int TILE = 32;
	private static final int GAP = 3;
	private static final int ON_COLOR = 0xFF55C9FF;

	private final Screen parent;
	private final List<Block> all = new ArrayList<>();
	private final List<IconButton> tiles = new ArrayList<>();
	private final Set<String> before = new HashSet<>(Settings.picked);
	private List<Block> matches = new ArrayList<>();
	private EditBox search;
	private String query = "";
	private int scroll;
	private int maxScroll;
	private int columns;
	private int visibleRows;
	private int left;
	private int gridWidth;

	public BlockPickerScreen(Screen parent) {
		super(Component.literal("Pick blocks"));
		this.parent = parent;
		for (Block block : BuiltInRegistries.BLOCK) {
			if (!block.defaultBlockState().isAir()) all.add(block);
		}
		all.sort(Comparator.comparing((Block block) -> block.getName().getString(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(BlockPickerScreen::path, String.CASE_INSENSITIVE_ORDER));
	}

	private static String id(Block block) { return BuiltInRegistries.BLOCK.getKey(block).toString(); }
	private static String path(Block block) {
		String id = id(block);
		return id.substring(id.indexOf(':') + 1);
	}

	@Override
	protected void init() {
		tiles.clear();
		columns = Math.max(1, Math.min(18, (this.width - 24 + GAP) / (TILE + GAP)));
		gridWidth = columns * TILE + (columns - 1) * GAP;
		left = (this.width - gridWidth) / 2;
		visibleRows = Math.max(1, (this.height - TOP - BOTTOM) / (TILE + GAP));

		search = new EditBox(this.font, left, 21, gridWidth, 16, Component.literal("Search blocks"));
		search.setMaxLength(80);
		search.setValue(query);
		search.setHint(Component.literal("Search every block..."));
		search.setResponder(text -> { query = text; scroll = 0; filter(); });
		addRenderableWidget(search);

		int y = this.height - 23;
		addRenderableWidget(new FlatButton(this.width / 2 - 104, y, 100, "Clear all", () -> {
			Settings.picked.clear(); filter();
		}).primary());
		addRenderableWidget(new FlatButton(this.width / 2 + 4, y, 100, "Done", this::onClose).primary());
		filter();
		setInitialFocus(search);
	}

	private boolean selected(Block block) { return Settings.picked.contains(id(block)); }

	private void filter() {
		String wanted = query.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
		matches = new ArrayList<>();
		for (Block block : all) {
			if (block.getName().getString().toLowerCase(Locale.ROOT).contains(wanted)
				|| path(block).replace('_', ' ').contains(wanted)) matches.add(block);
		}
		matches.sort(Comparator.comparing((Block block) -> !selected(block))
			.thenComparing(block -> block.getName().getString(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(BlockPickerScreen::path, String.CASE_INSENSITIVE_ORDER));
		showTiles();
	}

	private void showTiles() {
		tiles.forEach(this::removeWidget);
		tiles.clear();
		maxScroll = Math.max(0, (matches.size() + columns - 1) / columns - visibleRows);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		int first = scroll * columns;
		int end = Math.min(matches.size(), first + visibleRows * columns);
		for (int i = first; i < end; i++) {
			Block block = matches.get(i);
			int x = left + (i - first) % columns * (TILE + GAP);
			int y = TOP + (i - first) / columns * (TILE + GAP);
			IconButton tile = new IconButton(x, y, block, () -> {
				String key = id(block);
				if (!Settings.picked.remove(key)) Settings.picked.add(key);
				filter();
			});
			tiles.add(addRenderableWidget(tile));
		}
	}

	private final class IconButton extends AbstractWidget {
		private final Block block;
		private final ItemStack icon;
		private final Runnable action;

		IconButton(int x, int y, Block block, Runnable action) {
			super(x, y, TILE, TILE, Component.literal(block.getName().getString()));
			this.block = block;
			this.icon = new ItemStack(block);
			this.action = action;
			setTooltip(Tooltip.create(Component.literal(block.getName().getString() + "  •  " + id(block))));
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
			boolean on = selected(block);
			boolean hover = isHoveredOrFocused();
			int bg = on ? 0xD0143145 : hover ? Theme.ROW_HOVER : 0xA5081722;
			Theme.box(g, getX(), getY(), TILE, TILE, bg);
			g.fill(getX() + 5, getY() + 5, getX() + TILE - 5, getY() + 6, Theme.GRID_LINE);
			if (on) {
				g.fill(getX(), getY(), getX() + TILE, getY() + 2, ON_COLOR);
				g.fill(getX(), getY() + TILE - 2, getX() + TILE, getY() + TILE, ON_COLOR);
				g.fill(getX(), getY(), getX() + 2, getY() + TILE, ON_COLOR);
				g.fill(getX() + TILE - 2, getY(), getX() + TILE, getY() + TILE, ON_COLOR);
			}
			if (!icon.isEmpty()) g.renderItem(icon, getX() + 8, getY() + 8);
		}

		@Override public void onClick(MouseButtonEvent event, boolean doubleClick) { action.run(); }
		@Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (vertical != 0) {
			int next = Math.max(0, Math.min(scroll - 3 * (int) Math.signum(vertical), maxScroll));
			if (next != scroll) { scroll = next; showTiles(); }
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override public void tick() { if (getFocused() != search) setFocused(search); }

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, Theme.DIM);
		Theme.box(graphics, left - 6, 2, gridWidth + 12, this.height - 4, Theme.PANEL);
		Theme.frame(graphics, left - 6, 2, gridWidth + 12, this.height - 4, Theme.accent());
		for (int x = left; x < left + gridWidth; x += 24) {
			graphics.fill(x, TOP - 2, x + 1, this.height - BOTTOM + 2, Theme.GRID_LINE);
		}
		graphics.fill(left, TOP - 3, left + gridWidth, TOP - 2, Theme.EDGE);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		Theme.centered(graphics, this.font,
			"Pick blocks  •  " + Format.count(matches.size()) + " blocks  •  hover for name, click to toggle",
			this.width / 2, 8, Theme.TEXT);
		if (matches.isEmpty()) Theme.centered(graphics, this.font, "No blocks match", this.width / 2, TOP + 6, Theme.MUTED);
	}

	@Override
	public void removed() {
		if (before.equals(Settings.picked)) return;
		if (!before.containsAll(Settings.picked)) Category.PICKED.enabled = true;
		before.clear(); before.addAll(Settings.picked);
		BlockRadarClient.SCANNER.rescan();
		BlockRadarClient.SCANNER.setFocus(null);
	}

	@Override public void onClose() { this.minecraft.setScreen(parent); }
	@Override public boolean isPauseScreen() { return false; }
}
