package blockradar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/** A searchable list of every block in the game. Click blocks to look for them. */
public class BlockPickerScreen extends Screen {
	/** One row. Blocks that share a name (a torch and its wall version) are chosen together. */
	private record Entry(String name, List<Block> blocks) {}

	private static final int TOP = 42;
	private static final int BOTTOM = 36;
	private static final int ROW = FlatButton.HEIGHT;
	private static final int GAP = 4;
	private static final int ON_COLOR = 0xFF7CF29A;

	private final Screen parent;
	private final List<Entry> all = new ArrayList<>();
	private final List<FlatButton> rows = new ArrayList<>();
	/** What was chosen when the list opened, to tell if anything changed. */
	private final Set<String> before = new HashSet<>(Settings.picked);

	private List<Entry> matches = new ArrayList<>();
	private EditBox search;
	private String query = "";
	private int scroll;
	private int maxScroll;
	private int columns;
	private int columnWidth;
	private int visibleRows;
	private int left;

	public BlockPickerScreen(Screen parent) {
		super(Component.literal("Pick blocks"));
		this.parent = parent;
		Map<String, List<Block>> byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block.defaultBlockState().isAir() || !Picks.allowed(path(block))) continue;
			byName.computeIfAbsent(block.getName().getString(), name -> new ArrayList<>()).add(block);
		}
		byName.forEach((name, blocks) -> all.add(new Entry(name, blocks)));
	}

	private static String id(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	/** The id without the mod name in front. */
	private static String path(Block block) {
		String id = id(block);
		return id.substring(id.indexOf(':') + 1);
	}

	@Override
	protected void init() {
		rows.clear();
		columns = Math.max(1, Math.min(3, (this.width - 20) / 150));
		columnWidth = Math.min(180, (this.width - 20 - (columns - 1) * GAP) / columns);
		int gridWidth = columns * columnWidth + (columns - 1) * GAP;
		left = (this.width - gridWidth) / 2;
		visibleRows = Math.max(1, (this.height - TOP - BOTTOM) / ROW);

		search = new EditBox(this.font, left, 20, gridWidth, 16, Component.literal("Search blocks"));
		search.setMaxLength(60);
		search.setValue(query);
		search.setHint(Component.literal("Type to search all blocks..."));
		search.setResponder(text -> {
			query = text;
			scroll = 0;
			filter();
		});
		addRenderableWidget(search);

		int y = this.height - 22;
		addRenderableWidget(new FlatButton(this.width / 2 - 104, y, 100, "Clear all", () -> {
			Settings.picked.clear();
			filter();
		}).primary());
		addRenderableWidget(new FlatButton(this.width / 2 + 4, y, 100, "Done", this::onClose).primary());

		filter();
		setInitialFocus(search);
	}

	private boolean selected(Entry entry) {
		return entry.blocks().stream().anyMatch(block -> Settings.picked.contains(id(block)));
	}

	/** Applies the search. Chosen blocks are listed first. */
	private void filter() {
		String wanted = query.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
		matches = new ArrayList<>();
		for (Entry entry : all) {
			if (entry.name().toLowerCase(Locale.ROOT).contains(wanted)
				|| entry.blocks().stream().anyMatch(block -> path(block).replace('_', ' ').contains(wanted))) {
				matches.add(entry);
			}
		}
		matches.sort(Comparator.comparing((Entry entry) -> !selected(entry))); // keeps A to Z otherwise
		showRows();
	}

	/** Creates buttons for the part of the list that is on screen. */
	private void showRows() {
		rows.forEach(this::removeWidget);
		rows.clear();
		maxScroll = Math.max(0, (matches.size() + columns - 1) / columns - visibleRows);
		scroll = Math.max(0, Math.min(scroll, maxScroll));

		int first = scroll * columns;
		int end = Math.min(matches.size(), first + visibleRows * columns);
		for (int i = first; i < end; i++) {
			Entry entry = matches.get(i);
			int x = left + (i - first) % columns * (columnWidth + GAP);
			int y = TOP + (i - first) / columns * ROW;
			FlatButton[] button = new FlatButton[1];
			button[0] = new FlatButton(x, y, columnWidth, entry.name(), () -> {
				toggle(entry);
				style(button[0], entry);
			});
			style(button[0], entry);
			rows.add(addRenderableWidget(button[0]));
		}
	}

	private void style(FlatButton button, Entry entry) {
		boolean on = selected(entry);
		button.lit(on).color(ON_COLOR).detail(on ? "ON" : "");
	}

	private void toggle(Entry entry) {
		boolean on = selected(entry);
		for (Block block : entry.blocks()) {
			if (on) Settings.picked.remove(id(block));
			else Settings.picked.add(id(block));
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (vertical != 0) {
			int next = Math.max(0, Math.min(scroll - 3 * (int) Math.signum(vertical), maxScroll));
			if (next != scroll) {
				scroll = next;
				showRows();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void tick() {
		// Typing should always go to the search box, even after clicking a block.
		if (getFocused() != search) setFocused(search);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.renderBackground(graphics, mouseX, mouseY, delta);
		int gridWidth = columns * columnWidth + (columns - 1) * GAP;
		Theme.box(graphics, left - 6, 2, gridWidth + 12, this.height - 4, Theme.PANEL);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		Theme.centered(graphics, this.font,
			"Pick blocks  •  " + Format.count(matches.size()) + " shown  •  scroll for more",
			this.width / 2, 8, Theme.TEXT);
		if (matches.isEmpty()) {
			Theme.centered(graphics, this.font, "No blocks match", this.width / 2, TOP + 6, Theme.MUTED);
		}
	}

	/** Runs however the list is closed. */
	@Override
	public void removed() {
		if (before.equals(Settings.picked)) return;
		// Choosing a new block also switches "My blocks" on.
		if (!before.containsAll(Settings.picked)) Category.PICKED.enabled = true;
		before.clear();
		before.addAll(Settings.picked);
		BlockRadarClient.SCANNER.rescan();
		BlockRadarClient.SCANNER.setFocus(null); // also saves the settings
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
