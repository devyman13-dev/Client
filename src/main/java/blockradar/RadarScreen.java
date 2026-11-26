package blockradar;

import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** The menu: radar switch, a category dropdown and a block dropdown. */
public class RadarScreen extends Screen {
	private enum Open { NONE, CATEGORIES, BLOCKS }

	private static final int WIDTH = 220;
	private static final int ROW = 22;

	private Open open = Open.NONE;
	private int page;

	public RadarScreen() {
		super(Component.literal("Block Radar"));
	}

	private void button(String label, int x, int y, int w, Runnable action) {
		addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
			.bounds(x, y, w, 20)
			.build());
	}

	private void show(Open next) {
		open = open == next ? Open.NONE : next;
		page = 0;
		rebuildWidgets();
	}

	@Override
	protected void init() {
		int x = (this.width - WIDTH) / 2;
		int y = 30;

		button("Radar: " + (BlockRadarClient.active ? "ON" : "OFF"), x, y, WIDTH, () -> {
			BlockRadarClient.active = !BlockRadarClient.active;
			BlockRadarClient.SCANNER.clear();
			rebuildWidgets();
		});
		y += ROW;

		button("Categories " + (open == Open.CATEGORIES ? "▲" : "▼"), x, y, WIDTH,
			() -> show(Open.CATEGORIES));
		y += ROW;

		if (open == Open.CATEGORIES) {
			for (Category category : Category.values()) {
				button((category.enabled ? "[x] " : "[ ] ") + category.label, x + 10, y, WIDTH - 10, () -> {
					category.enabled = !category.enabled;
					BlockRadarClient.SCANNER.setFocus(null);
					BlockRadarClient.SCANNER.clear();
					rebuildWidgets();
				});
				y += ROW;
			}
		}

		Block focus = BlockRadarClient.SCANNER.focus();
		String selected = focus == null ? "All blocks" : focus.getName().getString();
		button("Showing: " + selected + (open == Open.BLOCKS ? " ▲" : " ▼"), x, y, WIDTH,
			() -> show(Open.BLOCKS));
		y += ROW;

		if (open == Open.BLOCKS) {
			List<Map.Entry<Block, Integer>> totals = BlockRadarClient.SCANNER.totals();
			// Fit the list to the window, leaving room for "All blocks", the page buttons and Done.
			int perPage = Math.max(1, (this.height - y - 3 * ROW - 10) / ROW);
			int pages = Math.max(1, (totals.size() + perPage - 1) / perPage);
			page = Math.min(page, pages - 1);

			button("All blocks", x + 10, y, WIDTH - 10, () -> choose(null));
			y += ROW;
			int from = page * perPage;
			for (Map.Entry<Block, Integer> entry : totals.subList(from, Math.min(totals.size(), from + perPage))) {
				String label = entry.getKey().getName().getString() + "  x" + entry.getValue();
				button(label, x + 10, y, WIDTH - 10, () -> choose(entry.getKey()));
				y += ROW;
			}
			if (pages > 1) {
				button("< Prev", x + 10, y, 100, () -> { page = Math.max(0, page - 1); rebuildWidgets(); });
				button("Next >", x + WIDTH - 100, y, 100, () -> { page++; rebuildWidgets(); });
			}
		}

		button("Done", x, this.height - 26, WIDTH, this::onClose);
	}

	private void choose(Block block) {
		BlockRadarClient.SCANNER.setFocus(block);
		show(Open.NONE);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		String title = this.title.getString();
		graphics.drawString(this.font, title, (this.width - this.font.width(title)) / 2, 12, 0xFFFFFFFF, true);
	}

	@Override
	public boolean isPauseScreen() {
		return false; // keep scanning while the menu is open
	}
}
