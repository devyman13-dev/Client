package blockradar;

/** Rules for the blocks the player chooses in the block list. */
public final class Picks {
	private Picks() {}

	/**
	 * Ores are left out of the list on purpose: seeing them through walls is x-ray.
	 * Copper, netherrack and reinforced deepslate are left out per the player's request.
	 *
	 * @param block block name without the "minecraft:" part
	 */
	public static boolean allowed(String block) {
		return !block.endsWith("_ore") && !block.equals("ancient_debris")
			&& !block.equals("netherrack") && !block.equals("reinforced_deepslate")
			&& !Rules.isCopper(block);
	}

	/**
	 * Which category a block shows up under right now.
	 *
	 * @param rule   what the radar's own rules say about it, or null
	 * @param picked whether the player chose it
	 * @return null if it should not be highlighted
	 */
	public static Category shown(Category rule, boolean picked) {
		if (rule != null && rule.enabled) return rule;
		return picked && Category.PICKED.enabled ? Category.PICKED : null;
	}
}
