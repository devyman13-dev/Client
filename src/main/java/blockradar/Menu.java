package blockradar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Where the menu's panels and rows are, and what the mouse does to them. No drawing here. */
public final class Menu {
	public static final int HEADER = 18;
	public static final int ROW = 15;
	/** Clicking this close to a row's right edge opens its settings. */
	public static final int MORE_WIDTH = 15;
	/** Space left and right of a slider's bar. */
	public static final int SLIDER_PAD = 6;
	private static final int BOTTOM_SPACE = 16;

	/** A number set by dragging across a row. */
	public record Slider(IntSupplier get, IntConsumer set, int min, int max, int step) {
		/** The value at a position along the bar, from 0 (left) to 1 (right). */
		public int valueAt(double share) {
			double clamped = Math.max(0, Math.min(1, share));
			int value = min + (int) Math.round(clamped * (max - min) / step) * step;
			return Math.max(min, Math.min(max, value));
		}

		public double share() {
			return (double) (get.getAsInt() - min) / (max - min);
		}
	}

	/** One line in a panel. */
	public static final class Row {
		public final String label;
		/** Text on the right. For sliders, the unit shown after the number. */
		public String detail = "";
		/** Shown at the bottom of the screen while the mouse is over the row. */
		public String help = "";
		/** Color of the small square before the label, or 0 for none. */
		public int color;
		public boolean on;
		/** An indented setting that belongs to the row above. */
		public boolean setting;
		public boolean expanded;
		/** A thin bar from 0 to 1, or -1 for none. */
		public double bar = -1;
		public Runnable click;
		/** Opens or closes the settings under this row. */
		public Runnable more;
		public Slider slider;

		public Row(String label) { this.label = label; }
		public Row detail(String text) { detail = text; return this; }
		public Row help(String text) { help = text; return this; }
		public Row color(int value) { color = value; return this; }
		public Row on(boolean value) { on = value; return this; }
		public Row setting() { setting = true; return this; }
		public Row bar(double value) { bar = value; return this; }
		public Row click(Runnable action) { click = action; return this; }
		public Row more(boolean open, Runnable action) { expanded = open; more = action; return this; }
		public Row slider(Slider value) { slider = value; return this; }
	}

	public static final class Panel {
		public final int id;
		public final String title;
		public final List<Row> rows = new ArrayList<>();
		public int x;
		public int y;
		public int width;
		public boolean open;
		/** Long panels scroll instead of running off the screen. */
		public boolean scrolls;
		private int scroll;

		public Panel(int id, String title, int x, int y, int width, boolean open) {
			this.id = id;
			this.title = title;
			this.x = x;
			this.y = y;
			this.width = width;
			this.open = open;
		}

		public int visibleRows(int screenHeight) {
			if (!open) return 0;
			if (!scrolls) return rows.size();
			int fit = Math.max(1, (screenHeight - y - HEADER - BOTTOM_SPACE) / ROW);
			return Math.min(fit, rows.size());
		}

		/** Index of the top row on screen. */
		public int firstRow(int screenHeight) {
			scroll = Math.max(0, Math.min(scroll, rows.size() - visibleRows(screenHeight)));
			return open ? scroll : 0;
		}

		public int height(int screenHeight) {
			return HEADER + visibleRows(screenHeight) * ROW;
		}
	}

	/** Told about things the player did. */
	public interface Listener {
		void fold(Panel panel);
		void moved(Panel panel);
		/** A row was clicked or a slider let go: save and rebuild the rows. */
		void changed();
	}

	/** Later panels are drawn on top of earlier ones. */
	public final List<Panel> panels = new ArrayList<>();
	private final int screenWidth;
	private final int screenHeight;
	private final Listener listener;

	private Panel dragged;
	private double grabX;
	private double grabY;
	private int startX;
	private int startY;
	private boolean moved;
	private Panel sliderPanel;
	private Slider slider;

	public Menu(int screenWidth, int screenHeight, Listener listener) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.listener = listener;
	}

	/** Adds a panel, pulled back onto the screen if it would be outside. */
	public void add(Panel panel) {
		panel.x = Math.max(0, Math.min(panel.x, screenWidth - panel.width));
		panel.y = Math.max(0, Math.min(panel.y, screenHeight - HEADER));
		panels.add(panel);
	}

	public Panel panelAt(double mouseX, double mouseY) {
		for (int i = panels.size() - 1; i >= 0; i--) {
			Panel p = panels.get(i);
			if (mouseX >= p.x && mouseX < p.x + p.width && mouseY >= p.y && mouseY < p.y + p.height(screenHeight)) {
				return p;
			}
		}
		return null;
	}

	/** The row under the mouse, or null over a title or outside the panel. */
	public Row rowAt(Panel panel, double mouseY) {
		if (panel == null || mouseY < panel.y + HEADER) return null;
		int line = (int) (mouseY - panel.y - HEADER) / ROW;
		if (line >= panel.visibleRows(screenHeight)) return null;
		return panel.rows.get(panel.firstRow(screenHeight) + line);
	}

	/** @param button 0 is the left mouse button, 1 the right */
	public boolean press(double mouseX, double mouseY, int button) {
		Panel panel = panelAt(mouseX, mouseY);
		if (panel == null) return false;
		// Bring the clicked panel to the front.
		panels.remove(panel);
		panels.add(panel);

		if (mouseY < panel.y + HEADER) {
			if (button == 1) {
				listener.fold(panel);
			} else if (button == 0) {
				dragged = panel;
				grabX = mouseX - panel.x;
				grabY = mouseY - panel.y;
				startX = panel.x;
				startY = panel.y;
				moved = false;
			}
			return true;
		}

		Row row = rowAt(panel, mouseY);
		if (row == null) return true;
		if (button == 1) {
			if (row.more != null) {
				row.more.run();
				listener.changed();
			}
		} else if (button == 0) {
			if (row.slider != null) {
				sliderPanel = panel;
				slider = row.slider;
				slide(mouseX);
				return true;
			}
			boolean onArrow = mouseX >= panel.x + panel.width - MORE_WIDTH;
			Runnable action = row.more != null && (onArrow || row.click == null) ? row.more : row.click;
			if (action != null) {
				action.run();
				listener.changed();
			}
		}
		return true;
	}

	private void slide(double mouseX) {
		double share = (mouseX - sliderPanel.x - SLIDER_PAD) / (sliderPanel.width - 2 * SLIDER_PAD);
		slider.set().accept(slider.valueAt(share));
	}

	public boolean drag(double mouseX, double mouseY) {
		if (dragged != null) {
			int x = Math.max(0, Math.min((int) (mouseX - grabX), screenWidth - dragged.width));
			int y = Math.max(0, Math.min((int) (mouseY - grabY), screenHeight - HEADER));
			if (Math.abs(x - startX) + Math.abs(y - startY) > 3) moved = true;
			if (moved) {
				dragged.x = x;
				dragged.y = y;
			}
			return true;
		}
		if (slider != null) {
			slide(mouseX);
			return true;
		}
		return false;
	}

	public boolean release() {
		if (dragged != null) {
			Panel panel = dragged;
			dragged = null;
			if (moved) listener.moved(panel);
			else listener.fold(panel);
			return true;
		}
		if (slider != null) {
			slider = null;
			listener.changed();
			return true;
		}
		return false;
	}

	/** @param amount positive scrolls up */
	public boolean scroll(double mouseX, double mouseY, double amount) {
		Panel panel = panelAt(mouseX, mouseY);
		if (panel == null || !panel.scrolls || amount == 0) return false;
		panel.scroll -= 2 * (int) Math.signum(amount);
		panel.firstRow(screenHeight); // keeps it in range
		return true;
	}
}
