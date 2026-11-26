package blockradar;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Shown after clicking a highlighted block. */
public class InfoScreen extends Screen {
	private final Scanner.Hit hit;
	private final List<String> lines = new ArrayList<>();

	public InfoScreen(Scanner.Hit hit) {
		super(hit.block().getName());
		this.hit = hit;
		BlockPos p = hit.pos();
		lines.add(hit.block().getName().getString());
		lines.add("Reason: " + hit.category().label);
		lines.add("Position: " + p.getX() + ", " + p.getY() + ", " + p.getZ());
		lines.add("In this connected group: " + BlockRadarClient.SCANNER.clusterSize(hit));
		lines.add("In all scanned chunks: " + BlockRadarClient.SCANNER.total(hit.block()));
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 110;
		int y = this.height / 2 + 25;
		addRenderableWidget(Button.builder(Component.literal("Show only this block"), b -> {
			BlockRadarClient.SCANNER.setFocus(hit.block());
			onClose();
		}).bounds(x, y, 220, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
			.bounds(x, y + 22, 220, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		int y = this.height / 2 - 55;
		for (String line : lines) {
			graphics.drawString(this.font, line, (this.width - this.font.width(line)) / 2, y, 0xFFFFFFFF, true);
			y += 14;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
