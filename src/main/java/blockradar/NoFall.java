package blockradar;

import net.minecraft.client.player.LocalPlayer;

/**
 * No fall damage. Each position update the game sends to the server says whether you are on
 * the ground. While you are falling, this makes that update say "on the ground", so the server
 * never builds up fall distance. See LocalPlayerMixin for where it is applied.
 */
public final class NoFall {
	private NoFall() {}

	/** True if the next position update should claim the player is on the ground. */
	public static boolean shouldSpoof(LocalPlayer player) {
		if (!Settings.noFall) return false;
		if (player.onGround() || player.isPassenger() || player.isFallFlying()) return false;
		if (player.getAbilities().flying) return false;
		return player.fallDistance > Settings.noFallDistance;
	}
}
