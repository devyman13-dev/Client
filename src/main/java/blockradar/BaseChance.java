package blockradar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guesses how likely a chunk is to hold a base or something good, from the blocks the
 * radar flagged in it. This is a score made from the numbers below, not a measured
 * probability, so change the numbers if it guesses too high or too low.
 */
public final class BaseChance {
	private BaseChance() {}

	/** One reason behind the score. "strong" means it rarely appears without a player. */
	public record Sign(String label, int count, boolean strong, double points) {}
	/** The signs are listed with the biggest reason first. */
	public record Result(int percent, String verdict, List<Sign> signs) {}

	/** The score is never 100%, because blocks alone cannot prove there is a base. */
	private static final int HIGHEST = 95;
	/** Blocks that structures also contain can only add this many points together. */
	private static final double WEAK_LIMIT = 22;
	/** Points needed for about 63%. Raise it to make every chance lower. */
	private static final double SCALE = 45;

	private record Kind(String label, double weight, boolean strong) {}

	private static final Map<String, Kind> KINDS = new LinkedHashMap<>();

	private static void strong(String block, String label, double weight) {
		KINDS.put(block, new Kind(label, weight, true));
	}

	private static void weak(String block, String label, double weight) {
		KINDS.put(block, new Kind(label, weight, false));
	}

	static {
		strong("beacon", "Beacons", 70);
		strong("netherite_block", "Netherite blocks", 70);
		strong("shulker_box", "Shulker boxes", 60);
		strong("enchanting_table", "Enchanting tables", 45);
		strong("conduit", "Conduits", 40);
		strong("ender_chest", "Ender chests", 35);
		strong("respawn_anchor", "Respawn anchors", 20);
		strong("lodestone", "Lodestones", 20);
		strong("hopper", "Hoppers", 14);
		strong("observer", "Observers", 14);
		strong("concrete", "Concrete", 12);
		strong("daylight_detector", "Daylight detectors", 10);
		strong("slime_block", "Slime blocks", 10);
		strong("honey_block", "Honey blocks", 10);
		strong("bamboo_mosaic", "Bamboo mosaic", 8);
		strong("dropper", "Droppers", 8);
		strong("crafter", "Crafters", 6);
		strong("redstone_block", "Redstone blocks", 6);
		strong("redstone_lamp", "Redstone lamps", 6);
		strong("comparator", "Comparators", 6);

		weak("deepslate", "Deepslate building blocks", 6);
		weak("piston", "Pistons", 5);
		weak("sticky_piston", "Sticky pistons", 5);
		weak("powered_rail", "Powered rails", 5);
		weak("detector_rail", "Detector rails", 5);
		weak("activator_rail", "Activator rails", 5);
		weak("resin_bricks", "Resin bricks", 5);
		weak("chest", "Chests", 4);
		weak("trapped_chest", "Trapped chests", 4);
		weak("repeater", "Repeaters", 4);
		weak("note_block", "Note blocks", 4);
		weak("target", "Target blocks", 4);
		weak("redstone_wire", "Redstone dust", 3);
		weak("redstone_torch", "Redstone torches", 3);
		weak("dispenser", "Dispensers", 3);
		weak("iron_trapdoor", "Iron trapdoors", 3);
		weak("barrel", "Barrels", 2);
		weak("iron_door", "Iron doors", 2);
		weak("lever", "Levers", 2);
		weak("tnt", "TNT", 2);
		weak("tripwire", "Tripwire", 1);
		weak("tripwire_hook", "Tripwire hooks", 1);
		weak("button", "Buttons", 1);
		weak("pressure_plate", "Pressure plates", 1);
	}

	private static final Kind OTHER = new Kind("Other flagged blocks", 2, false);

	/**
	 * Groups colors and shapes together, so 16 colors of concrete count as one kind.
	 *
	 * @return the group, or null if the block should not be counted
	 */
	static String group(String block) {
		// The front of a piston is part of a piston that is already counted.
		if (block.equals("piston_head") || block.equals("moving_piston")) return null;
		if (block.equals("redstone_wall_torch")) return "redstone_torch";
		if (block.endsWith("shulker_box")) return "shulker_box";
		if (block.endsWith("_concrete") || block.endsWith("_concrete_powder")) return "concrete";
		if (block.endsWith("_button")) return "button";
		if (block.endsWith("_pressure_plate")) return "pressure_plate";
		if (block.startsWith("bamboo_mosaic")) return "bamboo_mosaic";
		if (block.contains("resin_brick")) return "resin_bricks";
		if (block.contains("deepslate")) return "deepslate";
		return block;
	}

	/** One of a kind scores its weight; 4 score double and 16 score triple. */
	private static double points(double weight, int count) {
		return weight * (1 + 0.5 * Math.log(count) / Math.log(2));
	}

	/** Lots of chests and barrels together is a sign of its own. */
	private static double storagePoints(int containers) {
		if (containers >= 30) return 50;
		if (containers >= 12) return 22;
		return 0;
	}

	/**
	 * @param flagged how many of each flagged block the chunk holds, by block name without
	 *                the "minecraft:" part
	 */
	public static Result of(Map<String, Integer> flagged) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		flagged.forEach((block, count) -> {
			String group = group(block);
			if (group != null && count > 0) counts.merge(group, count, Integer::sum);
		});

		List<Sign> signs = new ArrayList<>();
		double strong = 0;
		double weak = 0;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			Kind kind = KINDS.getOrDefault(entry.getKey(), OTHER);
			double points = points(kind.weight(), entry.getValue());
			if (kind.strong()) strong += points;
			else weak += points;
			signs.add(new Sign(kind.label(), entry.getValue(), kind.strong(), points));
		}

		int containers = counts.getOrDefault("chest", 0) + counts.getOrDefault("trapped_chest", 0)
			+ counts.getOrDefault("barrel", 0);
		double storage = storagePoints(containers);
		if (storage > 0) signs.add(new Sign("Lots of storage", containers, true, storage));

		double total = strong + storage + Math.min(weak, WEAK_LIMIT);
		int percent = 0;
		if (total > 0) {
			percent = (int) Math.round(100 * (1 - Math.exp(-total / SCALE)));
			percent = Math.max(1, Math.min(HIGHEST, percent));
		}
		signs.sort(Comparator.comparingDouble((Sign sign) -> -sign.points()));
		return new Result(percent, verdict(percent), List.copyOf(signs));
	}

	private static String verdict(int percent) {
		if (percent == 0) return "No signs";
		if (percent < 25) return "Unlikely";
		if (percent < 50) return "Possible";
		if (percent < 75) return "Likely";
		return "Very likely";
	}
}
