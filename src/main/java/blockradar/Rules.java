package blockradar;

import java.util.List;
import java.util.Set;

/** Rules for redstone, storage, and blocks world generation does not place. */
public final class Rules {
	private Rules() {}

	private static Set<String> words(String list) {
		return Set.of(list.trim().split("\\s+"));
	}

	private static final Set<String> REDSTONE = words("""
		redstone_wire redstone_torch redstone_wall_torch redstone_block redstone_lamp
		piston sticky_piston piston_head moving_piston observer repeater comparator
		lever dispenser dropper hopper crafter note_block target daylight_detector
		tripwire tripwire_hook tnt powered_rail detector_rail activator_rail
		calibrated_sculk_sensor slime_block honey_block iron_door iron_trapdoor
		""");
	private static final List<String> REDSTONE_ENDINGS = List.of("_button", "_pressure_plate");
	private static final Set<String> STORAGE = words("chest trapped_chest ender_chest barrel");
	private static final Set<String> NEVER_GENERATED = words("""
		beacon conduit netherite_block respawn_anchor lodestone enchanting_table
		bamboo_mosaic bamboo_mosaic_slab bamboo_mosaic_stairs
		resin_bricks resin_brick_slab resin_brick_stairs resin_brick_wall chiseled_resin_bricks
		""");
	private static final List<String> NEVER_GENERATED_ENDINGS = List.of("_concrete", "_concrete_powder");
	private static final Set<String> DEEPSLATE_BUILDING = words("""
		cobbled_deepslate cobbled_deepslate_slab cobbled_deepslate_stairs cobbled_deepslate_wall
		polished_deepslate polished_deepslate_slab polished_deepslate_stairs polished_deepslate_wall
		deepslate_bricks cracked_deepslate_bricks deepslate_brick_slab deepslate_brick_stairs
		deepslate_brick_wall deepslate_tiles cracked_deepslate_tiles deepslate_tile_slab
		deepslate_tile_stairs deepslate_tile_wall chiseled_deepslate
		""");

	/** These blocks are suspicious only when above their normal deep underground use. */
	public static boolean dependsOnHeight(String block) {
		return DEEPSLATE_BUILDING.contains(block);
	}

	/** Copper blocks (lightning rods included) are never highlighted. */
	public static boolean isCopper(String block) {
		return block.contains("copper") || block.endsWith("lightning_rod");
	}

	private static boolean endsWithAny(String name, List<String> endings) {
		for (String ending : endings) if (name.endsWith(ending)) return true;
		return false;
	}

	private static boolean isStorage(String block) {
		return STORAGE.contains(block) || block.endsWith("shulker_box");
	}

	/**
	 * Classifies blocks without checking their dimension or biome. The finder intentionally
	 * reports the same rules in every environment.
	 */
	public static Category classify(String block, String dimension, String biome, int y) {
		if (isCopper(block)) return null;
		if (isStorage(block)) return Category.STORAGE;
		if (REDSTONE.contains(block) || endsWithAny(block, REDSTONE_ENDINGS)) return Category.REDSTONE;
		if (DEEPSLATE_BUILDING.contains(block)) return y >= 20 ? Category.PLACED : null;
		return NEVER_GENERATED.contains(block) || endsWithAny(block, NEVER_GENERATED_ENDINGS)
			? Category.PLACED : null;
	}
}
