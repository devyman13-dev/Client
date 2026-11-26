package blockradar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A flat, modern row that can be clicked. */
public class FlatButton extends AbstractWidget {
	private final Runnable action;
	private String detail = "";
	private int dot;
	private Boolean switchState;
	private double share = -1;
	private boolean primary;

	public FlatButton(int x, int y, int width, String label, Runnable action) {
		super(x, y, width, 18, Component.literal(label));
		this.action = action;
	}

	/** Grey text on the right side. */
	public FlatButton detail(String text) { detail = text; return this; }
	/** Small colored square on the left. */
	public FlatButton dot(int color) { dot = color; return this; }
	/** On/off switch on the right side. */
	public FlatButton toggle(boolean on) { switchState = on; return this; }
	/** Thin bar along the bottom, from 0 to 1. */
	public FlatButton share(double value) { share = value; return this; }
	/** Filled with the accent color and centered. */
	public FlatButton primary() { primary = true; return this; }

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Font font = Minecraft.getInstance().font;
		boolean hover = isHoveredOrFocused();
		int x = getX();
		int y = getY();
		int w = getWidth();

		if (primary) {
			Theme.box(g, x, y, w, getHeight(), hover ? 0xFF6BA1FF : Theme.ACCENT);
			Theme.centered(g, font, getMessage().getString(), x + w / 2, y + 5, Theme.TEXT);
			return;
		}

		Theme.box(g, x, y, w, getHeight(), hover ? Theme.ROW_HOVER : Theme.ROW);
		int left = x + 7;
		if (dot != 0) {
			Theme.box(g, left, y + 6, 6, 6, dot);
			left += 11;
		}

		int right = x + w - 7;
		if (switchState != null) {
			right -= 18;
			Theme.toggle(g, right, y + 4, switchState);
			right -= 6;
		}
		if (!detail.isEmpty()) {
			right -= font.width(detail);
			Theme.text(g, font, detail, right, y + 5, Theme.MUTED);
			right -= 6;
		}

		String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, right - left));
		Theme.text(g, font, label, left, y + 5, Theme.TEXT);
		if (share >= 0) Theme.bar(g, x + 7, y + 15, w - 14, share, dot != 0 ? dot : Theme.ACCENT);
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
