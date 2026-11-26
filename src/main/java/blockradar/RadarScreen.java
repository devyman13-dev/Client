package blockradar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** The menu: three dropdown panels that can be folded and dragged around. */
public class RadarScreen extends Screen {
	private static final int RADAR = 0;
	private static final int CATEGORIES = 1;
	private static final int BLOCKS = 2;
	private static final String[] TITLES = {"Radar", "Categories", "Blocks"};
	private static final int ROW = FlatButton.HEIGHT;
	private static final int GAP = 6;

	/** Where each panel is right now, and the widgets that move with it. */
	private final int[] panelX = new int[3];
	private final int[] panelY = new int[3];
	private final int[] panelWidth = new int[3];
	private final int[] panelHeight = new int[3];
	private final List<List<AbstractWidget>> parts = new ArrayList<>();

	private int scroll;
	private int maxScroll;
	private Scanner.Stats stats;
	private int ticks;

	public RadarScreen() {
		super(Component.literal("Block Radar"));
	}

	@Override
	protected void init() {
		// A click that opened another screen never got its release here.
		setDragging(false);
		stats = BlockRadarClient.SCANNER.stats();
		maxScroll = 0;
		parts.clear();

		panelWidth[BLOCKS] = Math.max(100, Math.min(170, this.width - 250));
		int small = Math.max(80, Math.min(112, (this.width - panelWidth[BLOCKS] - 4 * GAP) / 2));
		panelWidth[RADAR] = small;
		panelWidth[CATEGORIES] = small;

		int total = 2 * small + panelWidth[BLOCKS] + 2 * GAP;
		int defaultX = Math.max(0, (this.width - total) / 2);
		for (int panel = 0; panel < 3; panel++) {
			// A position of -1 means the panel has never been moved.
			int x = Settings.panelX[panel] < 0 ? defaultX : Settings.panelX[panel];
			int y = Settings.panelY[panel] < 0 ? 24 : Settings.panelY[panel];
			panelX[panel] = clamp(x, this.width - panelWidth[panel]);
			panelY[panel] = clamp(y, this.height - PanelHeader.HEIGHT);
			defaultX += panelWidth[panel] + GAP;
			addPanel(panel);
		}
	}

	private static int clamp(int value, int max) {
		return Math.max(0, Math.min(value, max));
	}

	private void addPanel(int panel) {
		List<AbstractWidget> widgets = new ArrayList<>();
		parts.add(widgets);
		boolean open = Settings.panelOpen[panel];
		widgets.add(addRenderableWidget(new PanelHeader(panelX[panel], panelY[panel], panelWidth[panel],
			TITLES[panel], open,
			(x, y) -> move(panel, x, y),
			() -> {
				Settings.panelOpen[panel] = !Settings.panelOpen[panel];
				BlockRadarClient.saveSettings();
				rebuildWidgets();
			},
			() -> {
				Settings.panelX[panel] = panelX[panel];
				Settings.panelY[panel] = panelY[panel];
				BlockRadarClient.saveSettings();
				rebuildWidgets();
			})));
		panelHeight[panel] = PanelHeader.HEIGHT;
		if (!open) return;

		if (panel == RADAR) {
			row(panel, "Enabled", () -> {
				Settings.radarOn = !Settings.radarOn;
				BlockRadarClient.saveSettings();
				BlockRadarClient.SCANNER.clear();
				rebuildWidgets();
			}).lit(Settings.radarOn).color(0xFF4CE08A).detail(Settings.radarOn ? "ON" : "OFF");
			row(panel, "Chunks", () -> {
				Settings.chunks = Settings.chunks.next();
				BlockRadarClient.saveSettings();
				rebuildWidgets();
			}).lit(Settings.chunks != ChunkStyle.OFF).color(0xFF2F7BFF).detail(Settings.chunks.name());
			panelHeight[panel] += 40; // room for the live numbers
		} else if (panel == CATEGORIES) {
			for (Category category : Category.values()) {
				int count = stats.byCategory().getOrDefault(category, 0);
				row(panel, category.label, () -> {
					category.enabled = !category.enabled;
					BlockRadarClient.SCANNER.setFocus(null); // also saves the settings
					rebuildWidgets();
				}).lit(category.enabled).color(category.color).detail(Format.count(count));
			}
			row(panel, "Pick blocks...", () -> this.minecraft.setScreen(new BlockPickerScreen(this)))
				.lit(true).color(Theme.ACCENT).detail(">");
		} else {
			addBlockRows();
		}
	}

	/** Adds a row below the last one in a panel. */
	private FlatButton row(int panel, String label, Runnable action) {
		FlatButton button = new FlatButton(panelX[panel], panelY[panel] + panelHeight[panel],
			panelWidth[panel], label, action);
		panelHeight[panel] += ROW;
		parts.get(panel).add(button);
		return addRenderableWidget(button);
	}

	/** The scrollable list of block types. */
	private void addBlockRows() {
		List<Map.Entry<Block, Integer>> blocks = stats.byBlock();
		Block focus = BlockRadarClient.SCANNER.focus();
		int room = this.height - panelY[BLOCKS] - PanelHeader.HEIGHT - 16;
		int visible = Math.max(1, room / ROW);
		int total = blocks.size() + 1; // + "All blocks"
		maxScroll = Math.max(0, total - visible);
		scroll = clamp(scroll, maxScroll);

		for (int i = scroll; i < Math.min(total, scroll + visible); i++) {
			if (i == 0) {
				row(BLOCKS, "All blocks", () -> choose(null))
					.lit(focus == null).detail(Format.count(stats.highlighted()));
			} else {
				Block block = blocks.get(i - 1).getKey();
				int count = blocks.get(i - 1).getValue();
				row(BLOCKS, block.getName().getString(), () -> choose(block))
					.lit(true).color(stats.colors().get(block))
					.detail((focus == block ? "● " : "") + Format.count(count) + "  "
						+ Format.percent(count, stats.highlighted()))
					.share((double) count / Math.max(1, stats.highlighted()));
			}
		}
	}

	private void choose(Block block) {
		BlockRadarClient.SCANNER.setFocus(block);
		rebuildWidgets();
	}

	private void move(int panel, int x, int y) {
		x = clamp(x, this.width - panelWidth[panel]);
		y = clamp(y, this.height - PanelHeader.HEIGHT);
		int dx = x - panelX[panel];
		int dy = y - panelY[panel];
		for (AbstractWidget widget : parts.get(panel)) {
			widget.setX(widget.getX() + dx);
			widget.setY(widget.getY() + dy);
		}
		panelX[panel] = x;
		panelY[panel] = y;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		boolean overList = mouseX >= panelX[BLOCKS] && mouseX < panelX[BLOCKS] + panelWidth[BLOCKS]
			&& mouseY >= panelY[BLOCKS] && mouseY < panelY[BLOCKS] + panelHeight[BLOCKS];
		if (overList && vertical != 0) {
			int next = clamp(scroll - (int) Math.signum(vertical), maxScroll);
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
		// Keep the numbers up to date.
		if (++ticks % 10 != 0) return;
		Scanner.Stats old = stats;
		stats = BlockRadarClient.SCANNER.stats();
		boolean changed = !stats.byBlock().equals(old.byBlock())
			|| !stats.byCategory().equals(old.byCategory());
		// Rows are rebuilt too, but never in the middle of a drag.
		if (changed && !isDragging()) rebuildWidgets();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.renderBackground(graphics, mouseX, mouseY, delta);
		for (int panel = 0; panel < 3; panel++) {
			graphics.fill(panelX[panel], panelY[panel], panelX[panel] + panelWidth[panel],
				panelY[panel] + panelHeight[panel] + 2, Theme.PANEL);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		if (Settings.panelOpen[RADAR]) {
			int x = panelX[RADAR] + 7;
			int y = panelY[RADAR] + PanelHeader.HEIGHT + 2 * ROW + 5;
			Theme.text(graphics, this.font, Format.count(stats.suspicious()) + " suspicious", x, y, Theme.TEXT);
			Theme.text(graphics, this.font, Format.percent(stats.suspicious(), stats.blocks()) + " of blocks",
				x, y + 13, Theme.MUTED);
			Theme.bar(graphics, x, y + 27, panelWidth[RADAR] - 14,
				(double) stats.suspicious() / Math.max(1, stats.blocks()), Theme.ACCENT);
		}
		Theme.centered(graphics, this.font, "Drag a title to move  •  Click it to fold  •  Esc to close",
			this.width / 2, this.height - 11, Theme.MUTED);
	}

	@Override
	public boolean isPauseScreen() {
		return false; // keep scanning while the menu is open
	}
}
