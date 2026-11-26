package blockradar;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
/** Everything the player can change in the menu, kept in a small text file. */
public final class Settings {
	private Settings() {}

	public static boolean radarOn = true;
	public static int scanRange = 8;
	private static final Set<String> DEFAULT_BLOCKS = defaultBlocks();
	public static final Set<String> selectedBlocks = new HashSet<>(DEFAULT_BLOCKS);
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
		scanRange = Math.max(2, Math.min(32, integer(saved, "scanRange", scanRange)));
		String blockList = saved.getProperty("selectedBlocks");
		if (blockList != null) {
			selectedBlocks.clear();
			for (String id : blockList.split(",")) {
				String cleaned = id.trim();
				String path = cleaned.substring(cleaned.lastIndexOf(':') + 1);
				if (!cleaned.isEmpty() && !path.contains("copper") && !path.equals("netherrack")
						&& !path.equals("reinforced_deepslate")) selectedBlocks.add(cleaned);
			}
		}
		onlyBlock = saved.getProperty("onlyBlock", onlyBlock).trim();
		for (Category category : Category.values()) {
			category.enabled = flag(saved, "category." + category.name(), category.enabled);
		}
	}

	public static void save(Path file) {
		Properties saved = new Properties();
		saved.setProperty("radar", Boolean.toString(radarOn));
		saved.setProperty("scanRange", Integer.toString(scanRange));
		saved.setProperty("selectedBlocks", String.join(",", new TreeSet<>(selectedBlocks)));
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

	private static int integer(Properties saved, String key, int fallback) {
		try {
			return Integer.parseInt(saved.getProperty(key, Integer.toString(fallback)).trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static boolean blockSelected(Block block) {
		var id = BuiltInRegistries.BLOCK.getKey(block);
		return selectedBlocks.contains(id.toString())
			|| (id.getNamespace().equals("minecraft") && selectedBlocks.contains(id.getPath()));
	}

	public static void setBlockSelected(Block block, boolean selected) {
		var id = BuiltInRegistries.BLOCK.getKey(block);
		String key = id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
		if (selected) selectedBlocks.add(key);
		else selectedBlocks.remove(key);
	}

	private static Set<String> defaultBlocks() {
		Set<String> result = new HashSet<>(Set.of(
			"redstone_wire", "redstone_torch", "redstone_wall_torch", "redstone_block",
			"repeater", "comparator", "observer", "piston", "sticky_piston",
			"dispenser", "dropper", "hopper", "lever", "target", "crafter",
			"tripwire", "tripwire_hook", "chest", "trapped_chest", "barrel", "ender_chest",
			"furnace", "blast_furnace", "smoker", "crafting_table", "enchanting_table",
			"beacon", "conduit", "respawn_anchor", "lodestone", "anvil", "chipped_anvil",
			"damaged_anvil", "brewing_stand", "stonecutter", "smithing_table", "grindstone", "loom"
		));
		result.add("shulker_box");
		for (String color : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
			"pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
			result.add(color + "_shulker_box");
		}
		return result;
	}
}
