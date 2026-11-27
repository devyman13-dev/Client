package blockradar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** A flat dropdown row. Rows that are switched on light up in their own color. */
public class FlatButton extends AbstractWidget {
	public static final int HEIGHT = 16;

	private final Runnable action;
	private String detail = "";
	private int color = Theme.accent();
	private boolean lit;
	private double share = -1;
	private boolean primary;
	/** The block pictured at the start of the row, or null. */
	private Block icon;

	public FlatButton(int x, int y, int width, String label, Runnable action) {
		super(x, y, width, HEIGHT, Component.literal(label));
		this.action = action;
	}

	/** Grey text on the right side. */
	public FlatButton detail(String text) { detail = text; return this; }
	/** Color used when the row is lit. */
	public FlatButton color(int value) { color = value; return this; }
	/** Switched on: colored text, side stripe and tint. */
	public FlatButton lit(boolean on) { lit = on; return this; }
	/** Thin bar along the bottom, from 0 to 1. */
	public FlatButton share(double value) { share = value; return this; }
	/** Filled with the accent color and centered. */
	public FlatButton primary() { primary = true; return this; }
	/** Pictures this block at the left edge of the row. */
	public FlatButton icon(Block block) { icon = block; return this; }

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Font font = Minecraft.getInstance().font;
		boolean hover = isHoveredOrFocused();
		int x = getX();
		int y = getY();
		int w = getWidth();

		if (primary) {
			Theme.box(g, x, y, w, getHeight(), Theme.alpha(Theme.accent(), hover ? 0xFF : 0xC8));
			Theme.centered(g, font, getMessage().getString(), x + w / 2, y + 4, Theme.TEXT);
			return;
		}

		g.fill(x, y, x + w, y + HEIGHT, hover ? Theme.ROW_HOVER : Theme.ROW_BG);
		g.fill(x + 4, y + HEIGHT - 1, x + w - 4, y + HEIGHT, Theme.GRID_LINE);
		if (lit) {
			g.fill(x, y, x + w, y + HEIGHT, (color & 0x00FFFFFF) | 0x22000000);
			g.fill(x, y, x + 2, y + HEIGHT, color);
		}

		int right = x + w - 5;
		if (!detail.isEmpty()) {
			right -= font.width(detail);
			Theme.text(g, font, detail, right, y + 4, Theme.MUTED);
			right -= 4;
		}

		int textX = x + 7;
		if (icon != null) {
			Icons.draw(g, icon, x + 3, y);
			textX = x + 22;
		}
		String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, right - textX));
		Theme.text(g, font, label, textX, y + 4, lit ? color : hover ? Theme.TEXT : Theme.MUTED);
		if (share >= 0) {
			int length = (int) Math.round((w - 9) * Math.min(1, share));
			g.fill(x + 7, y + HEIGHT - 2, x + 7 + Math.max(1, length), y + HEIGHT - 1, color);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		action.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
