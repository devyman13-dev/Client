package blockradar;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** Everything the player can change in the menu, kept in a small text file. */
public final class Settings {
	private Settings() {}

	public static boolean radarOn = true;
	/** Blue outlines around chunks with highlighted blocks. */
	public static boolean chunkOutlines = true;
	/** Full name of the only block to show, such as "minecraft:piston". Empty means all. */
	public static String onlyBlock = "";
	/** Full names of the blocks chosen in the block list. */
	public static final Set<String> picked = new LinkedHashSet<>();
	/** Menu panel positions (-1 means not moved yet) and whether each panel is unfolded. */
	public static final int[] panelX = {-1, -1, -1};
	public static final int[] panelY = {-1, -1, -1};
	public static final boolean[] panelOpen = {true, true, true};

	/** Reads the file. Anything missing or unreadable keeps its current value. */
	public static void load(Path file) {
		if (!Files.exists(file)) return;
		Properties saved = new Properties();
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			saved.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			System.err.println("[Block Radar] Could not read settings: " + e);
			return;
		}
		radarOn = flag(saved, "radar", radarOn);
		chunkOutlines = flag(saved, "chunkOutlines", chunkOutlines);
		onlyBlock = saved.getProperty("onlyBlock", onlyBlock).trim();
		String chosen = saved.getProperty("picked");
		if (chosen != null) {
			picked.clear();
			for (String id : chosen.split(",")) if (!id.isBlank()) picked.add(id.trim());
		}
		for (Category category : Category.values()) {
			category.enabled = flag(saved, "category." + category.name(), category.enabled);
		}
		for (int i = 0; i < panelOpen.length; i++) {
			panelX[i] = number(saved, "panel." + i + ".x", panelX[i]);
			panelY[i] = number(saved, "panel." + i + ".y", panelY[i]);
			panelOpen[i] = flag(saved, "panel." + i + ".open", panelOpen[i]);
		}
	}

	public static void save(Path file) {
		Properties saved = new Properties();
		saved.setProperty("radar", Boolean.toString(radarOn));
		saved.setProperty("chunkOutlines", Boolean.toString(chunkOutlines));
		saved.setProperty("onlyBlock", onlyBlock);
		saved.setProperty("picked", String.join(",", picked));
		for (Category category : Category.values()) {
			saved.setProperty("category." + category.name(), Boolean.toString(category.enabled));
		}
		for (int i = 0; i < panelOpen.length; i++) {
			saved.setProperty("panel." + i + ".x", Integer.toString(panelX[i]));
			saved.setProperty("panel." + i + ".y", Integer.toString(panelY[i]));
			saved.setProperty("panel." + i + ".open", Boolean.toString(panelOpen[i]));
		}
		try {
			Files.createDirectories(file.toAbsolutePath().getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				saved.store(writer, "Block Radar settings");
			}
		} catch (IOException e) {
			System.err.println("[Block Radar] Could not save settings: " + e);
		}
	}

	private static int number(Properties saved, String key, int fallback) {
		try {
			return Integer.parseInt(saved.getProperty(key, "").trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static boolean flag(Properties saved, String key, boolean fallback) {
		String value = saved.getProperty(key, "").trim();
		if (value.equals("true")) return true;
		if (value.equals("false")) return false;
		return fallback;
	}
}
