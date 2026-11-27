package blockradar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** One chunk's base chance and suspicious blocks, shown in the top right corner. */
public class ChunkScreen extends Screen {
	private static final int PANEL_WIDTH = 210;
	private static final int MARGIN = 6;
	private static final int HEADER = 44;
	private static final int FOOTER = 24;
	private static final int ROW = FlatButton.HEIGHT;
	/** Height of the base chance section without its reasons, and of each reason. */
	private static final int CHANCE = 30;
	private static final int REASON = 10;
	private static final int MOST_REASONS = 3;

	private final Scanner.ChunkReport report;
	private int scroll;
	private int maxScroll;
	private int panelHeight;
	/** How many reasons fit, and where the block rows start. */
	private int reasons;
	private int listTop;

	public ChunkScreen(Scanner.ChunkReport report) {
		super(Component.literal("Chunk " + report.chunk().x() + ", " + report.chunk().z()));
		this.report = report;
	}

	private int panelWidth() { return Math.min(PANEL_WIDTH, this.width - 2 * MARGIN); }
	private int left() { return this.width - panelWidth() - MARGIN; }

	@Override
	protected void init() {
		// Short windows show fewer reasons so the block list keeps some room.
		reasons = Math.min(report.chance().signs().size(),
			Math.max(0, Math.min(MOST_REASONS, (this.height - 150) / 30)));
		int above = HEADER + CHANCE + reasons * REASON;
		listTop = MARGIN + above;
		int fit = Math.max(1, (this.height - 2 * MARGIN - above - FOOTER) / ROW);
		int visible = Math.min(fit, report.rows().size());
		maxScroll = report.rows().size() - visible;
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		panelHeight = above + Math.max(1, visible) * ROW + FOOTER;

		for (int i = 0; i < visible; i++) {
			Scanner.Row row = report.rows().get(scroll + i);
			FlatButton button = new FlatButton(left(), listTop + i * ROW, panelWidth(),
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
		drawChance(graphics, MARGIN + HEADER);
		if (report.rows().isEmpty()) {
			Theme.centered(graphics, this.font, "Nothing found in this chunk", center, listTop + 4, Theme.MUTED);
		}
	}

	/** The percentage, a bar, and the biggest reasons for it. */
	private void drawChance(GuiGraphics g, int y) {
		BaseChance.Result chance = report.chance();
		int x = left() + 10;
		int right = left() + panelWidth() - 10;
		int color = chanceColor(chance.percent());
		g.fill(left() + 4, y, left() + panelWidth() - 4, y + CHANCE + reasons * REASON - 3, Theme.ROW_BG);

		String value = chance.percent() + "%  " + chance.verdict();
		Theme.text(g, this.font, "Base / loot chance", x, y + 5, Theme.TEXT);
		Theme.text(g, this.font, value, right - this.font.width(value), y + 5, color);

		g.fill(x, y + 17, right, y + 21, Theme.TRACK);
		int length = (right - x) * chance.percent() / 100;
		if (length > 0) g.fill(x, y + 17, x + length, y + 21, color);

		for (int i = 0; i < reasons; i++) {
			BaseChance.Sign sign = chance.signs().get(i);
			int lineY = y + 25 + i * REASON;
			String strength = sign.strong() ? "strong sign" : "weak sign";
			int strengthX = right - this.font.width(strength);
			String name = this.font.plainSubstrByWidth(
				Format.count(sign.count()) + " x " + sign.label(), Math.max(0, strengthX - x - 4));
			Theme.text(g, this.font, name, x, lineY, Theme.MUTED);
			Theme.text(g, this.font, strength, strengthX, lineY, sign.strong() ? color : Theme.MUTED);
		}
	}

	private static int chanceColor(int percent) {
		if (percent >= 75) return 0xFF4AFFA8;
		if (percent >= 50) return 0xFFFFD54D;
		if (percent >= 25) return 0xFFFFA04D;
		return Theme.MUTED;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
