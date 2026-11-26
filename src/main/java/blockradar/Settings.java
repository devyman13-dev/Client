package blockradar;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Everything the player can change in the menu, kept in a small text file. */
public final class Settings {
	private Settings() {}

	public static boolean radarOn = true;
	/** Full name of the only block to show, such as "minecraft:piston". Empty means all. */
	public static String onlyBlock = "";

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
		onlyBlock = saved.getProperty("onlyBlock", onlyBlock).trim();
		for (Category category : Category.values()) {
			category.enabled = flag(saved, "category." + category.name(), category.enabled);
		}
	}

	public static void save(Path file) {
		Properties saved = new Properties();
		saved.setProperty("radar", Boolean.toString(radarOn));
		saved.setProperty("onlyBlock", onlyBlock);
		for (Category category : Category.values()) {
			saved.setProperty("category." + category.name(), Boolean.toString(category.enabled));
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

	private static boolean flag(Properties saved, String key, boolean fallback) {
		String value = saved.getProperty(key, "").trim();
		if (value.equals("true")) return true;
		if (value.equals("false")) return false;
		return fallback;
	}
}
