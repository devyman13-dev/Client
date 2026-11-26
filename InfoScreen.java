package blockradar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

/** Shown in the top right corner after clicking a highlighted block. */
public class InfoScreen extends Screen {
	private static final int PANEL_WIDTH = 240;
	private static final int PANEL_HEIGHT = 156;
	private static final int MARGIN = 6;

	private final Scanner.Hit hit;
	private final Scanner.Group group;
	private final int total;
	private final long highlighted;

	public InfoScreen(Scanner.Hit hit) {
		super(hit.block().getName());
		this.hit = hit;
		Scanner.Stats stats = BlockRadarClient.SCANNER.stats();
		this.highlighted = stats.highlighted();
		this.total = stats.byBlock().stream()
			.filter(entry -> entry.getKey() == hit.block())
			.mapToInt(entry -> entry.getValue())
			.sum();
		this.group = BlockRadarClient.SCANNER.clusterSize(hit);
	}

	private int left() { return this.width - PANEL_WIDTH - MARGIN; }
	private int top() { return MARGIN; }

	@Override
	protected void init() {
		int x = left() + 10;
		int y = top() + PANEL_HEIGHT - 28;
		addRenderableWidget(new FlatButton(x, y, 76, "Only this", () -> {
			BlockRadarClient.SCANNER.setFocus(hit.block());
			onClose();
		}).primary());
		addRenderableWidget(new FlatButton(x + 80, y, 86, "Chunk list", () -> {
			Scanner.Chunk chunk = new Scanner.Chunk(hit.pos().getX() >> 4, hit.pos().getZ() >> 4);
			this.minecraft.setScreen(new ChunkScreen(BlockRadarClient.SCANNER.report(chunk)));
		}).primary());
		addRenderableWidget(new FlatButton(x + 170, y, PANEL_WIDTH - 190, "Close", this::onClose).primary());
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// No super call: the world stays clear behind the corner panel.
		Theme.box(graphics, left(), top(), PANEL_WIDTH, PANEL_HEIGHT, Theme.PANEL);
		graphics.fill(left() + 10, top(), left() + PANEL_WIDTH - 10, top() + 2, hit.color());
	}

	private void line(GuiGraphics graphics, String name, String value, int y) {
		Theme.text(graphics, this.font, name, left() + 12, y, Theme.MUTED);
		Theme.text(graphics, this.font, value, left() + PANEL_WIDTH - 12 - this.font.width(value), y, Theme.TEXT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int y = top() + 12;
		Theme.centered(graphics, this.font, this.title.getString(), left() + PANEL_WIDTH / 2, y, hit.color());
		Theme.centered(graphics, this.font, hit.category().label, left() + PANEL_WIDTH / 2, y + 14, hit.category().color);

		BlockPos p = hit.pos();
		line(graphics, "Position", p.getX() + ", " + p.getY() + ", " + p.getZ(), y + 34);
		line(graphics, "In this connected group",
			Format.count(group.size()) + (group.partial() ? "+" : ""), y + 49);
		line(graphics, "In all scanned chunks", Format.count(total), y + 64);
		line(graphics, "Share of highlighted blocks", Format.percent(total, highlighted), y + 79);
		Theme.bar(graphics, left() + 12, y + 93, PANEL_WIDTH - 24,
			(double) total / Math.max(1, highlighted), hit.color());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
