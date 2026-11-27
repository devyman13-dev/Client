package blockradar;

/** Rules for the blocks the player chooses in the block list. */
public final class Picks {
	private Picks() {}

	/** Every non-air registered block can be selected in the block picker. */
	public static boolean allowed(String block) { return true; }

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
