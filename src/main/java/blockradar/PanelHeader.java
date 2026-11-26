package blockradar;

import java.util.function.BiConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** The top of a dropdown panel. Click it to fold the panel, drag it to move the panel. */
public class PanelHeader extends AbstractWidget {
	public static final int HEIGHT = 18;

	private final boolean open;
	private final BiConsumer<Integer, Integer> move;
	private final Runnable fold;
	private final Runnable drop;
	private double draggedX;
	private double draggedY;
	private int startX;
	private int startY;
	private boolean moved;

	/**
	 * @param move called with the new top left corner while dragging
	 * @param fold called after a click without dragging
	 * @param drop called after dragging ends
	 */
	public PanelHeader(int x, int y, int width, String title, boolean open,
			BiConsumer<Integer, Integer> move, Runnable fold, Runnable drop) {
		super(x, y, width, HEIGHT, Component.literal(title));
		this.open = open;
		this.move = move;
		this.fold = fold;
		this.drop = drop;
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Font font = Minecraft.getInstance().font;
		int x = getX();
		int y = getY();
		int w = getWidth();
		g.fill(x, y, x + w, y + HEIGHT, isHoveredOrFocused() ? Theme.ROW_HOVER : Theme.HEADER);
		g.fill(x, y + HEIGHT - 1, x + w, y + HEIGHT, Theme.ACCENT);
		Theme.text(g, font, getMessage().getString(), x + 6, y + 5, Theme.TEXT);
		Theme.text(g, font, open ? "▲" : "▼", x + w - 12, y + 5, Theme.MUTED);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		draggedX = 0;
		draggedY = 0;
		startX = getX();
		startY = getY();
		moved = false;
	}

	@Override
	public void onDrag(MouseButtonEvent event, double dx, double dy) {
		draggedX += dx;
		draggedY += dy;
		if (Math.abs(draggedX) + Math.abs(draggedY) > 3) moved = true;
		if (moved) move.accept(startX + (int) draggedX, startY + (int) draggedY);
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		if (moved) drop.run();
		else fold.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
