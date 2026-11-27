package blockradar;

import java.util.Set;

/** Decides whether the player has chosen not to see a block. */
public final class HiddenBlocks {
	private HiddenBlocks() {}

	/**
	 * Hiding a block also hides its other shape: the wall version of a torch, sign or
	 * banner, and the heads of pistons.
	 *
	 * @param hidden full names of the blocks the player hid
	 * @param id     full block name, such as "minecraft:redstone_wall_torch"
	 */
	public static boolean hides(Set<String> hidden, String id) {
		if (hidden.contains(id)) return true;
		int colon = id.indexOf(':') + 1;
		String owner = id.substring(0, colon);
		String name = id.substring(colon);
		if (name.equals("piston_head") || name.equals("moving_piston")) {
			return hidden.contains(owner + "piston") || hidden.contains(owner + "sticky_piston");
		}
		if (name.equals("wall_torch")) return hidden.contains(owner + "torch");
		// oak_wall_sign is the wall version of oak_sign.
		return name.contains("_wall_") && hidden.contains(owner + name.replace("_wall_", "_"));
	}
}
