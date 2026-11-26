package blockradar;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides which blocks are interesting. Uses only plain strings (block and biome
 * names without "minecraft:"), so you can edit these lists without knowing any
 * Minecraft code.
 */
public final class Rules {
	private Rules() {}

	private static Set<String> words(String list) {
		return Set.of(list.trim().split("\\s+"));
	}

	private static final Set<String> REDSTONE = words("""
		redstone_wire redstone_torch redstone_wall_torch redstone_block redstone_lamp
		piston sticky_piston piston_head moving_piston observer repeater comparator
		lever dispenser dropper hopper crafter note_block target daylight_detector
		tripwire tripwire_hook tnt trapped_chest powered_rail detector_rail activator_rail
		calibrated_sculk_sensor slime_block honey_block
		iron_door iron_trapdoor
		""");

	private static final List<String> REDSTONE_ENDINGS =
		List.of("_button", "_pressure_plate", "copper_bulb", "lightning_rod");

	private static final Set<String> NETHER = words("""
		netherrack nether_quartz_ore nether_gold_ore ancient_debris soul_sand soul_soil
		basalt blackstone gilded_blackstone nether_bricks crimson_nylium warped_nylium
		crimson_stem warped_stem crimson_hyphae warped_hyphae nether_wart_block
		warped_wart_block shroomlight glowstone crimson_fungus warped_fungus
		crimson_roots warped_roots nether_sprouts weeping_vines weeping_vines_plant
		twisting_vines twisting_vines_plant nether_wart soul_fire
		""");

	private static final Set<String> END = words("""
		end_stone end_stone_bricks purpur_block purpur_pillar purpur_stairs purpur_slab
		chorus_plant chorus_flower end_rod dragon_egg
		""");

	private static final Set<String> OVERWORLD = words("""
		grass_block dirt coarse_dirt podzol stone deepslate granite diorite andesite tuff
		calcite water sand red_sand sandstone clay mud moss_block snow snow_block ice
		packed_ice blue_ice mycelium coal_ore iron_ore copper_ore gold_ore diamond_ore
		emerald_ore lapis_ore deepslate_coal_ore deepslate_iron_ore deepslate_copper_ore
		deepslate_gold_ore deepslate_diamond_ore deepslate_emerald_ore deepslate_lapis_ore
		short_grass tall_grass fern dandelion poppy cactus sugar_cane
		""");

	private static final Set<String> SCULK = words(
		"sculk sculk_vein sculk_catalyst sculk_shrieker sculk_sensor");

	/** Block name -> biome name fragments where it is expected to appear. */
	private static final Map<String, List<String>> BIOME_BLOCKS = new java.util.HashMap<>();

	private static void biome(String blocks, String... biomes) {
		for (String block : words(blocks)) BIOME_BLOCKS.put(block, List.of(biomes));
	}

	static {
		biome("cherry_log cherry_leaves cherry_sapling pink_petals", "cherry_grove");
		biome("pale_oak_log pale_oak_leaves pale_moss_block pale_hanging_moss creaking_heart", "pale_garden");
		biome("acacia_log acacia_leaves acacia_sapling", "savanna");
		biome("jungle_log jungle_leaves jungle_sapling cocoa bamboo", "jungle");
		biome("mangrove_log mangrove_leaves mangrove_roots muddy_mangrove_roots mangrove_propagule", "mangrove_swamp");
		biome("dark_oak_log dark_oak_leaves dark_oak_sapling", "dark_forest");
		biome("cactus", "desert", "badlands");
		biome("red_sand", "badlands");
		biome("mycelium", "mushroom_fields");
		biome("blue_ice packed_ice", "frozen", "snowy", "ice_spikes", "jagged_peaks");
		biome("""
			tube_coral_block brain_coral_block bubble_coral_block fire_coral_block
			horn_coral_block""", "warm_ocean");
		biome("cave_vines cave_vines_plant spore_blossom big_dripleaf big_dripleaf_stem small_dripleaf", "lush_caves");
	}

	/** Blocks that world generation never places, so a player must have put them there. */
	private static final Set<String> NEVER_GENERATED = words("""
		beacon conduit netherite_block respawn_anchor lodestone enchanting_table
		bamboo_mosaic bamboo_mosaic_slab bamboo_mosaic_stairs
		resin_bricks resin_brick_slab resin_brick_stairs resin_brick_wall chiseled_resin_bricks
		""");

	private static final List<String> NEVER_GENERATED_ENDINGS =
		List.of("shulker_box", "_concrete", "_concrete_powder");

	/** Deepslate tiles and other variants that structures only use deep underground. */
	private static final Set<String> DEEPSLATE_BUILDING = words("""
		cobbled_deepslate cobbled_deepslate_slab cobbled_deepslate_stairs cobbled_deepslate_wall
		polished_deepslate polished_deepslate_slab polished_deepslate_stairs polished_deepslate_wall
		deepslate_bricks cracked_deepslate_bricks deepslate_brick_slab deepslate_brick_stairs
		deepslate_brick_wall deepslate_tiles cracked_deepslate_tiles deepslate_tile_slab
		deepslate_tile_stairs deepslate_tile_wall chiseled_deepslate
		""");

	/** Normal and copper chests lower than this are highlighted. */
	private static final int DEEP_CHEST_Y = -35;

	private static boolean isChest(String block) {
		return block.equals("chest") || block.endsWith("copper_chest");
	}

	private static boolean endsWithAny(String name, List<String> endings) {
		for (String ending : endings) if (name.endsWith(ending)) return true;
		return false;
	}

	private static boolean isRedstone(String block) {
		return REDSTONE.contains(block) || endsWithAny(block, REDSTONE_ENDINGS);
	}

	private static boolean isOverworld(String block) {
		// Nether "wood" uses _stem and _hyphae, so _log and _leaves are always overworld.
		return OVERWORLD.contains(block) || block.endsWith("_log") || block.endsWith("_leaves");
	}

	private static boolean isPlaced(String block, String dimension, String biome, int y) {
		// Ancient cities and trial chambers are built from these.
		if (DEEPSLATE_BUILDING.contains(block)) {
			return !(dimension.equals("overworld") && (biome.equals("deep_dark") || y < 20));
		}
		if (isChest(block)) return dimension.equals("overworld") && y < DEEP_CHEST_Y;
		// Ender chests generate in End cities.
		if (block.equals("ender_chest")) return !dimension.equals("the_end");
		return NEVER_GENERATED.contains(block) || endsWithAny(block, NEVER_GENERATED_ENDINGS);
	}

	/**
	 * Sculk grows naturally in the deep dark, which sits deep down and under mountains.
	 * Ancient cities can also stick out past the edge of the deep dark.
	 */
	private static boolean isNaturalSculkSpot(String biome, int y) {
		if (biome.equals("deep_dark") || y < 0) return true;
		boolean mountain = biome.endsWith("_peaks") || biome.endsWith("_slopes") || biome.equals("grove")
			|| biome.equals("meadow") || biome.startsWith("windswept_");
		return mountain && y < 64;
	}

	/** True if the answer for this block can change with the biome or height. */
	public static boolean dependsOnPlace(String block) {
		return BIOME_BLOCKS.containsKey(block) || SCULK.contains(block)
			|| DEEPSLATE_BUILDING.contains(block) || isChest(block);
	}

	/**
	 * @param dimension "overworld", "the_nether" or "the_end"
	 * @param biome     biome name such as "cherry_grove"
	 * @param y         height of the block
	 * @return why the block should be highlighted, or null if it looks normal here
	 */
	public static Category classify(String block, String dimension, String biome, int y) {
		if (isRedstone(block)) return Category.REDSTONE;

		boolean overworld = dimension.equals("overworld");
		boolean nether = dimension.equals("the_nether");
		boolean end = dimension.equals("the_end");

		if (SCULK.contains(block)) {
			if (overworld && isNaturalSculkSpot(biome, y)) return null;
			if (block.equals("sculk_sensor")) return Category.REDSTONE;
			if (overworld) return Category.WRONG_BIOME;
			return nether || end ? Category.WRONG_DIMENSION : null;
		}

		// Custom dimensions have no "native" list, so only vanilla ones are checked.
		if (overworld || nether || end) {
			if (NETHER.contains(block) && !nether) return Category.WRONG_DIMENSION;
			if (END.contains(block) && !end) return Category.WRONG_DIMENSION;
			if (isOverworld(block) && !overworld) return Category.WRONG_DIMENSION;
		}
		if (overworld) {
			List<String> homes = BIOME_BLOCKS.get(block);
			if (homes != null && homes.stream().noneMatch(biome::contains)) return Category.WRONG_BIOME;
		}
		return isPlaced(block, dimension, biome, y) ? Category.PLACED : null;
	}
}
