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
	/** Boxes around highlighted blocks, and whether they are tinted inside. */
	public static boolean outlines = true;
	public static boolean fill = true;
	/** The small overlay shown while playing. */
	public static boolean hud = true;
	/** The "left click to see" message. */
	public static boolean hints = true;
	/** How chunks with highlighted blocks are shown. */
	public static ChunkStyle chunks = ChunkStyle.SOLID;
	/** How many blocks the blue chunk walls rise above sea level. */
	public static int chunkBlocks = 64;
	public static Accent accent = Accent.BLUE;

	/** Entity ESP: lines around players and mobs. */
	public static boolean entityEsp = false;
	public static boolean espPlayers = true;
	public static boolean espHostile = true;
	public static boolean espPassive = false;
	public static boolean espThroughWalls = true;
	/** Color players blue, hostile mobs red and other mobs green, instead of all blue. */
	public static boolean espByType = false;
	/** Names above entities, with their health and distance. */
	public static boolean espNames = true;
	public static boolean espHealth = true;
	public static boolean espDistance = true;
	public static int espRange = 64;
	public static int espWidth = 2;

	/** Jump reset: jump right after being hit on the ground (single player only). */
	public static boolean jumpReset = false;
	/** Ticks to wait after the hit before jumping (0 is the very next tick). */
	public static int jumpResetDelay = 0;

	/** Trigger bot: attacks when your crosshair is on a player or mob. */
	public static boolean triggerBot = false;
	public static boolean triggerPlayers = true;
	public static boolean triggerHostile = true;
	public static boolean triggerPassive = false;
	/** Wait until your attack is fully charged before hitting. */
	public static boolean triggerCooldown = true;
	/** Fewest game ticks (20 a second) between its attacks. */
	public static int triggerDelay = 0;

	/** No fall damage: spoofs "on the ground" while falling past this many blocks. */
	public static boolean noFall = false;
	public static int noFallDistance = 2;

	/** Freecam: speed in blocks per second, the sprint-key boost, and a few options. */
	public static int freecamSpeed = 12;
	public static int freecamBoost = 3;
	public static boolean freecamSmooth = true;
	/** Leave freecam when your character takes damage. */
	public static boolean freecamExitOnHurt = true;
	/** Turn your character back to where it faced when freecam was turned on. */
	public static boolean freecamRestoreView = true;

	/** Performance: the most boxes and chunks drawn, nearest first. */
	public static int maxOutlines = 600;
	public static int maxChunks = 64;
	/** Performance: time spent scanning per game tick, and the pause between full scans. */
	public static int scanMillis = 2;
	public static int rescanSeconds = 3;

	/** Full name of the only block to show, such as "minecraft:piston". Empty means all. */
	public static String onlyBlock = "";
	/** Full names of the blocks chosen in the block list. */
	public static final Set<String> picked = new LinkedHashSet<>();
	/** Full names of the blocks the player does not want to see. */
	public static final Set<String> hidden = new LinkedHashSet<>();
	/** Menu panel positions (-1 means not moved yet) and whether each panel is unfolded. */
	public static final int[] panelX = {-1, -1, -1, -1};
	public static final int[] panelY = {-1, -1, -1, -1};
	public static final boolean[] panelOpen = {true, true, true, true};

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
		outlines = flag(saved, "outlines", outlines);
		fill = flag(saved, "fill", fill);
		hud = flag(saved, "hud", hud);
		hints = flag(saved, "hints", hints);
		for (ChunkStyle style : ChunkStyle.values()) {
			if (style.name().equals(saved.getProperty("chunks", "").trim())) chunks = style;
		}
		for (Accent value : Accent.values()) {
			if (value.name().equals(saved.getProperty("accent", "").trim())) accent = value;
		}
		chunkBlocks = ranged(saved, "chunkBlocks", chunkBlocks, 0, ChunkStyle.MOST_BLOCKS);
		entityEsp = flag(saved, "entityEsp", entityEsp);
		espPlayers = flag(saved, "espPlayers", espPlayers);
		espHostile = flag(saved, "espHostile", espHostile);
		espPassive = flag(saved, "espPassive", espPassive);
		espThroughWalls = flag(saved, "espThroughWalls", espThroughWalls);
		espByType = flag(saved, "espByType", espByType);
		espNames = flag(saved, "espNames", espNames);
		espHealth = flag(saved, "espHealth", espHealth);
		espDistance = flag(saved, "espDistance", espDistance);
		espRange = ranged(saved, "espRange", espRange, 8, 256);
		espWidth = ranged(saved, "espWidth", espWidth, 1, 6);
		jumpReset = flag(saved, "jumpReset", jumpReset);
		jumpResetDelay = ranged(saved, "jumpResetDelay", jumpResetDelay, 0, 3);
		triggerBot = flag(saved, "triggerBot", triggerBot);
		triggerPlayers = flag(saved, "triggerPlayers", triggerPlayers);
		triggerHostile = flag(saved, "triggerHostile", triggerHostile);
		triggerPassive = flag(saved, "triggerPassive", triggerPassive);
		triggerCooldown = flag(saved, "triggerCooldown", triggerCooldown);
		triggerDelay = ranged(saved, "triggerDelay", triggerDelay, 0, 20);
		noFall = flag(saved, "noFall", noFall);
		noFallDistance = ranged(saved, "noFallDistance", noFallDistance, 1, 3);
		freecamSpeed = ranged(saved, "freecamSpeed", freecamSpeed, 1, 60);
		freecamBoost = ranged(saved, "freecamBoost", freecamBoost, 1, 8);
		freecamSmooth = flag(saved, "freecamSmooth", freecamSmooth);
		freecamExitOnHurt = flag(saved, "freecamExitOnHurt", freecamExitOnHurt);
		freecamRestoreView = flag(saved, "freecamRestoreView", freecamRestoreView);
		maxOutlines = ranged(saved, "maxOutlines", maxOutlines, 100, 2000);
		maxChunks = ranged(saved, "maxChunks", maxChunks, 8, 256);
		scanMillis = ranged(saved, "scanMillis", scanMillis, 1, 8);
		rescanSeconds = ranged(saved, "rescanSeconds", rescanSeconds, 1, 15);
		onlyBlock = saved.getProperty("onlyBlock", onlyBlock).trim();
		String chosen = saved.getProperty("picked");
		if (chosen != null) {
			picked.clear();
			for (String id : chosen.split(",")) if (!id.isBlank()) picked.add(id.trim());
		}
		String unwanted = saved.getProperty("hidden");
		if (unwanted != null) {
			hidden.clear();
			for (String id : unwanted.split(",")) if (!id.isBlank()) hidden.add(id.trim());
		}
		for (Category category : Category.values()) {
			category.enabled = flag(saved, "category." + category.name(), category.enabled);
		}
		for (int i = 0; i < panelOpen.length; i++) {
			panelX[i] = number(saved, "gui." + i + ".x", panelX[i]);
			panelY[i] = number(saved, "gui." + i + ".y", panelY[i]);
			panelOpen[i] = flag(saved, "gui." + i + ".open", panelOpen[i]);
		}
	}

	public static void save(Path file) {
		Properties saved = new Properties();
		saved.setProperty("radar", Boolean.toString(radarOn));
		saved.setProperty("outlines", Boolean.toString(outlines));
		saved.setProperty("fill", Boolean.toString(fill));
		saved.setProperty("hud", Boolean.toString(hud));
		saved.setProperty("hints", Boolean.toString(hints));
		saved.setProperty("chunks", chunks.name());
		saved.setProperty("accent", accent.name());
		saved.setProperty("chunkBlocks", Integer.toString(chunkBlocks));
		saved.setProperty("entityEsp", Boolean.toString(entityEsp));
		saved.setProperty("espPlayers", Boolean.toString(espPlayers));
		saved.setProperty("espHostile", Boolean.toString(espHostile));
		saved.setProperty("espPassive", Boolean.toString(espPassive));
		saved.setProperty("espThroughWalls", Boolean.toString(espThroughWalls));
		saved.setProperty("espByType", Boolean.toString(espByType));
		saved.setProperty("espNames", Boolean.toString(espNames));
		saved.setProperty("espHealth", Boolean.toString(espHealth));
		saved.setProperty("espDistance", Boolean.toString(espDistance));
		saved.setProperty("espRange", Integer.toString(espRange));
		saved.setProperty("espWidth", Integer.toString(espWidth));
		saved.setProperty("jumpReset", Boolean.toString(jumpReset));
		saved.setProperty("jumpResetDelay", Integer.toString(jumpResetDelay));
		saved.setProperty("triggerBot", Boolean.toString(triggerBot));
		saved.setProperty("triggerPlayers", Boolean.toString(triggerPlayers));
		saved.setProperty("triggerHostile", Boolean.toString(triggerHostile));
		saved.setProperty("triggerPassive", Boolean.toString(triggerPassive));
		saved.setProperty("triggerCooldown", Boolean.toString(triggerCooldown));
		saved.setProperty("triggerDelay", Integer.toString(triggerDelay));
		saved.setProperty("noFall", Boolean.toString(noFall));
		saved.setProperty("noFallDistance", Integer.toString(noFallDistance));
		saved.setProperty("freecamSpeed", Integer.toString(freecamSpeed));
		saved.setProperty("freecamBoost", Integer.toString(freecamBoost));
		saved.setProperty("freecamSmooth", Boolean.toString(freecamSmooth));
		saved.setProperty("freecamExitOnHurt", Boolean.toString(freecamExitOnHurt));
		saved.setProperty("freecamRestoreView", Boolean.toString(freecamRestoreView));
		saved.setProperty("maxOutlines", Integer.toString(maxOutlines));
		saved.setProperty("maxChunks", Integer.toString(maxChunks));
		saved.setProperty("scanMillis", Integer.toString(scanMillis));
		saved.setProperty("rescanSeconds", Integer.toString(rescanSeconds));
		saved.setProperty("onlyBlock", onlyBlock);
		saved.setProperty("picked", String.join(",", picked));
		saved.setProperty("hidden", String.join(",", hidden));
		for (Category category : Category.values()) {
			saved.setProperty("category." + category.name(), Boolean.toString(category.enabled));
		}
		for (int i = 0; i < panelOpen.length; i++) {
			saved.setProperty("gui." + i + ".x", Integer.toString(panelX[i]));
			saved.setProperty("gui." + i + ".y", Integer.toString(panelY[i]));
			saved.setProperty("gui." + i + ".open", Boolean.toString(panelOpen[i]));
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

	private static int ranged(Properties saved, String key, int fallback, int min, int max) {
		return Math.max(min, Math.min(max, number(saved, key, fallback)));
	}

	private static boolean flag(Properties saved, String key, boolean fallback) {
		String value = saved.getProperty(key, "").trim();
		if (value.equals("true")) return true;
		if (value.equals("false")) return false;
		return fallback;
	}
}
