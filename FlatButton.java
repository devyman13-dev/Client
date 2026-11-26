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
	private boolean selected;
	private boolean star;
	private String description = "";

	public FlatButton(int x, int y, int width, String label, Runnable action) {
		this(x, y, width, 22, label, action);
	}

	public FlatButton(int x, int y, int width, int height, String label, Runnable action) {
		super(x, y, width, height, Component.literal(label));
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
	/** Marks the currently selected category. */
	public FlatButton selected(boolean value) { selected = value; return this; }
	/** Supporting text shown on larger module cards. */
	public FlatButton description(String text) { description = text; return this; }
	/** Compact favorite-star button. */
	public FlatButton star(boolean value) { star = value; return this; }

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Font font = Minecraft.getInstance().font;
		boolean hover = isHoveredOrFocused();
		int x = getX();
		int y = getY();
		int w = getWidth();
		if (star) {
			if (hover) Theme.box(g, x, y, w, getHeight(), Theme.ROW_HOVER);
			Theme.centered(g, font, selected ? "★" : "☆", x + w / 2, y + 6,
				selected ? 0xFFFFD65A : 0xFFB2BDCA);
			return;
		}

		if (primary) {
			Theme.box(g, x, y, w, getHeight(), hover ? 0xFF6BA1FF : Theme.ACCENT);
			Theme.centered(g, font, getMessage().getString(), x + w / 2, y + 7, Theme.TEXT);
			return;
		}

		int background = selected ? 0xFF203246 : hover ? Theme.ROW_HOVER : Theme.ROW;
		Theme.box(g, x, y, w, getHeight(), background);
		if (!description.isEmpty()) {
			if (selected) Theme.outline(g, x, y, w, getHeight(), 0x883F89D4);
			int dotX = x + 14;
			if (dot != 0) Theme.box(g, dotX, y + 14, 10, 10, dot);
			int left = dot != 0 ? x + 34 : x + 16;
			int detailRight = x + w - 148;
			if (!detail.isEmpty()) {
				Theme.text(g, font, detail, detailRight - font.width(detail), y + 14, Theme.TEXT);
			}
			Theme.text(g, font, font.plainSubstrByWidth(getMessage().getString(),
				Math.max(0, detailRight - 8 - left)),
				left, y + 13, Theme.TEXT);
			Theme.text(g, font, font.plainSubstrByWidth(description, Math.max(0, w - 124)),
				left, y + 34, Theme.MUTED);
			Theme.toggle(g, x + w - 38, y + 21, switchState != null && switchState);
			if (share >= 0) Theme.bar(g, x + 12, y + getHeight() - 3, w - 24, share, dot != 0 ? dot : Theme.ACCENT);
			return;
		}
		if (selected) {
			Theme.box(g, x, y + 5, 3, getHeight() - 10, dot != 0 ? dot : Theme.ACCENT);
			Theme.outline(g, x, y, w, getHeight(), 0x663C83CB);
		}
		int left = x + 7;
		if (dot != 0) {
			Theme.box(g, left, y + 7, 8, 8, dot);
			left += 13;
		}

		int right = x + w - 7;
		if (switchState != null) {
			right -= 22;
			Theme.toggle(g, right, y + 5, switchState);
			right -= 6;
		}
		if (!detail.isEmpty()) {
			right -= font.width(detail);
			Theme.text(g, font, detail, right, y + 7, Theme.MUTED);
			right -= 6;
		}

		String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, right - left));
		Theme.text(g, font, label, left, y + 7, Theme.TEXT);
		if (share >= 0) Theme.bar(g, x + 7, y + getHeight() - 3, w - 14, share, dot != 0 ? dot : Theme.ACCENT);
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
