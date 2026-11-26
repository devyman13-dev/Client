package blockradar;

/** The highlight color of each block. Colors are ARGB. Edit these to taste. */
public final class Colors {
	private Colors() {}

	/** Longer names come first so "light_blue" is not mistaken for "blue". */
	private static final Object[][] DYES = {
		{"light_blue", 0x6EC6FF}, {"light_gray", 0xC4C4BC}, {"white", 0xF5F5F5}, {"orange", 0xFF8C1A},
		{"magenta", 0xE04FE0}, {"yellow", 0xFFD93D}, {"lime", 0x8BE32D}, {"pink", 0xFF8FB8},
		{"gray", 0x7A7F85}, {"cyan", 0x1FC7D4}, {"purple", 0xA64DFF}, {"blue", 0x4C6BFF},
		{"brown", 0xA8703F}, {"green", 0x5F9E2F}, {"red", 0xFF3B30}, {"black", 0x55555F},
	};

	/** @return the color for this block, or 0 to use its category color */
	public static int of(String block) {
		int rgb = rgb(block);
		return rgb == 0 ? 0 : 0xFF000000 | rgb;
	}

	private static int rgb(String b) {
		if (b.endsWith("_concrete") || b.endsWith("_concrete_powder") || b.endsWith("shulker_box")) {
			for (Object[] dye : DYES) if (b.startsWith(dye[0] + "_")) return (Integer) dye[1];
			return 0xB57EDC; // the undyed shulker box
		}

		// Redstone parts
		if (b.startsWith("redstone") || b.equals("repeater") || b.equals("comparator")) return 0xFF2020;
		if (b.equals("sticky_piston") || b.equals("slime_block")) return 0x6FE04A;
		if (b.contains("piston")) return 0xD9A86C;
		if (b.equals("honey_block")) return 0xFFB52E;
		if (b.equals("observer") || b.equals("dispenser") || b.equals("dropper") || b.equals("lever")
			|| b.equals("hopper") || b.startsWith("iron_")) return 0xB4BCC8;
		if (b.equals("tnt")) return 0xFF6A3D;
		if (b.endsWith("copper_bulb") || b.endsWith("lightning_rod") || b.endsWith("copper_chest")) return 0xE0854E;

		// Storage and special blocks
		if (b.equals("ender_chest")) return 0x9B5CFF;
		if (b.equals("trapped_chest")) return 0xFF7A5C;
		if (b.equals("chest")) return 0xFFB84D;
		if (b.equals("beacon") || b.equals("conduit")) return 0x7FFFF0;
		if (b.equals("netherite_block")) return 0x8D7580;
		if (b.equals("respawn_anchor")) return 0x7B2FF7;
		if (b.equals("enchanting_table")) return 0xE0407A;
		if (b.contains("resin")) return 0xFF8A1F;
		if (b.contains("sculk")) return 0x14C8C0;
		if (b.contains("deepslate")) return 0x8C93B0;

		// Nether and End
		if (b.startsWith("crimson")) return 0xD6336C;
		if (b.startsWith("warped")) return 0x20C9A6;
		if (b.startsWith("soul")) return 0xA98467;
		if (b.equals("glowstone") || b.equals("shroomlight")) return 0xFFD866;
		if (b.contains("basalt") || b.contains("blackstone")) return 0x8A8A96;
		if (b.startsWith("nether") || b.equals("ancient_debris")) return 0xC0392B;
		if (b.startsWith("end_") || b.equals("dragon_egg")) return 0xEDE9A3;
		if (b.startsWith("purpur") || b.startsWith("chorus")) return 0xC77DFF;

		// Biome blocks
		if (b.startsWith("cherry") || b.startsWith("pink")) return 0xFF9BD2;
		if (b.startsWith("pale") || b.equals("creaking_heart")) return 0xD8D8CC;
		if (b.startsWith("jungle") || b.startsWith("bamboo") || b.equals("cocoa")) return 0x9ACD32;
		if (b.startsWith("acacia")) return 0xFF8C42;
		if (b.contains("mangrove")) return 0xC4483C;
		if (b.startsWith("dark_oak")) return 0x9A6A3A;
		if (b.equals("cactus")) return 0x3FA34D;
		if (b.endsWith("ice")) return 0x9FD8FF;
		if (b.equals("mycelium")) return 0xB39BC8;
		if (b.equals("red_sand")) return 0xD9722E;
		return 0;
	}
}
