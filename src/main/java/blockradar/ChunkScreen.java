package blockradar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The suspicious blocks in one chunk, shown in the top right corner. */
public class ChunkScreen extends Screen {
	private static final int PANEL_WIDTH = 210;
	private static final int MARGIN = 6;
	private static final int HEADER = 44;
	private static final int FOOTER = 24;
	private static final int ROW = FlatButton.HEIGHT;

	private final Scanner.ChunkReport report;
	private int scroll;
	private int maxScroll;
	private int panelHeight;

	public ChunkScreen(Scanner.ChunkReport report) {
		super(Component.literal("Chunk " + report.chunk().x() + ", " + report.chunk().z()));
		this.report = report;
	}

	private int panelWidth() { return Math.min(PANEL_WIDTH, this.width - 2 * MARGIN); }
	private int left() { return this.width - panelWidth() - MARGIN; }

	@Override
	protected void init() {
		int fit = Math.max(1, (this.height - 2 * MARGIN - HEADER - FOOTER) / ROW);
		int visible = Math.min(fit, report.rows().size());
		maxScroll = report.rows().size() - visible;
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		panelHeight = HEADER + Math.max(1, visible) * ROW + FOOTER;

		for (int i = 0; i < visible; i++) {
			Scanner.Row row = report.rows().get(scroll + i);
			FlatButton button = new FlatButton(left(), MARGIN + HEADER + i * ROW, panelWidth(),
				row.block().getName().getString(), () -> {
					BlockRadarClient.SCANNER.setFocus(row.block());
					onClose();
				}).icon(row.block()).detail(Format.count(row.count())).lit(true).color(row.color());
			addRenderableWidget(button);
		}

		addRenderableWidget(new FlatButton(left() + 10, MARGIN + panelHeight - 20, panelWidth() - 20,
			"Close", this::onClose).primary());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (vertical != 0) {
			int next = Math.max(0, Math.min(scroll - 3 * (int) Math.signum(vertical), maxScroll));
			if (next != scroll) {
				scroll = next;
				rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// No super call: the world stays clear behind the corner panel.
		Theme.box(graphics, left(), MARGIN, panelWidth(), panelHeight, Theme.PANEL);
		Theme.frame(graphics, left(), MARGIN, panelWidth(), panelHeight, Theme.accent());
		graphics.fill(left() + 10, MARGIN + 2, left() + panelWidth() - 10, MARGIN + 3, Theme.EDGE);
		graphics.fill(left() + 10, MARGIN + 2, left() + panelWidth() / 3, MARGIN + 3, Theme.accent());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int center = left() + panelWidth() / 2;
		int x = report.chunk().x() << 4;
		int z = report.chunk().z() << 4;
		Theme.centered(graphics, this.font, this.title.getString(), center, MARGIN + 7, Theme.TEXT);
		Theme.centered(graphics, this.font, "X " + x + " to " + (x + 15) + "  •  Z " + z + " to " + (z + 15),
			center, MARGIN + 19, Theme.MUTED);
		Theme.centered(graphics, this.font, Format.count(report.total()) + " suspicious  •  "
			+ report.rows().size() + " block types", center, MARGIN + 31, Theme.MUTED);
		if (report.rows().isEmpty()) {
			Theme.centered(graphics, this.font, "Nothing found in this chunk", center, MARGIN + HEADER + 4, Theme.MUTED);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
