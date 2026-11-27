package blockradar;

import java.util.Map;

/** Some blocks, such as water, have no item. These items are used as their icons. */
public final class StandIns {
	private StandIns() {}

	private static final Map<String, String> ITEMS = Map.ofEntries(
		Map.entry("water", "water_bucket"),
		Map.entry("bubble_column", "water_bucket"),
		Map.entry("lava", "lava_bucket"),
		Map.entry("fire", "flint_and_steel"),
		Map.entry("soul_fire", "fire_charge"),
		Map.entry("piston_head", "piston"),
		Map.entry("moving_piston", "piston"),
		Map.entry("nether_portal", "obsidian"),
		Map.entry("end_portal", "ender_eye"),
		Map.entry("end_gateway", "ender_eye"),
		Map.entry("tall_seagrass", "seagrass"),
		Map.entry("kelp_plant", "kelp"),
		Map.entry("weeping_vines_plant", "weeping_vines"),
		Map.entry("twisting_vines_plant", "twisting_vines"),
		Map.entry("cave_vines", "glow_berries"),
		Map.entry("cave_vines_plant", "glow_berries"),
		Map.entry("big_dripleaf_stem", "big_dripleaf"),
		Map.entry("bamboo_sapling", "bamboo"),
		Map.entry("frosted_ice", "ice"),
		Map.entry("melon_stem", "melon_seeds"),
		Map.entry("attached_melon_stem", "melon_seeds"),
		Map.entry("pumpkin_stem", "pumpkin_seeds"),
		Map.entry("attached_pumpkin_stem", "pumpkin_seeds"));

	/**
	 * @param block block name without the "minecraft:" part
	 * @return the name of an item to show instead, or null if there is no good one
	 */
	public static String of(String block) {
		String item = ITEMS.get(block);
		if (item != null) return item;
		if (block.startsWith("potted_")) return "flower_pot";
		if (block.endsWith("_cauldron")) return "cauldron";
		if (block.endsWith("candle_cake")) return "cake";
		if (block.equals("wall_torch")) return "torch";
		// Wall versions look like the standing ones: oak_wall_sign becomes oak_sign.
		if (block.contains("_wall_")) return block.replace("_wall_", "_");
		return null;
	}
}
