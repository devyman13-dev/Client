package blockradar;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Jump reset: when you are hit while standing on the ground, jump on the very next tick.
 * Leaving the ground right as the knockback lands keeps you from sliding as far, and doing
 * it by hand needs perfect timing. Single player (and LAN host) worlds only.
 */
public final class JumpReset {
	private JumpReset() {}

	private static int lastHurt;
	/** Ticks left before the jump, or -1 when no jump is waiting. */
	private static int wait = -1;
	/** True while the jump key is being held down by this module. */
	private static boolean pressed;

	/** Fire (including lava) and fall damage never start a jump. Unknown damage still does. */
	private static boolean ignored(DamageSource source) {
		return source != null && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL));
	}

	/** Called every game tick. */
	public static void tick(Minecraft client) {
		KeyMapping jump = client.options.keyJump;
		if (pressed) {
			jump.setDown(false); // one tick of jump is enough
			pressed = false;
		}
		LocalPlayer player = client.player;
		if (player == null) {
			lastHurt = 0;
			wait = -1;
			return;
		}
		boolean fresh = player.hurtTime > lastHurt; // a new hit just landed
		lastHurt = player.hurtTime;

		boolean allowed = Settings.jumpReset && client.getSingleplayerServer() != null
			&& client.screen == null && !Freecam.active();
		if (!allowed) {
			wait = -1;
			return;
		}
		if (fresh && !ignored(player.getLastDamageSource())) wait = Settings.jumpResetDelay;
		if (wait < 0) return;
		if (wait > 0) {
			wait--;
			return;
		}
		wait = -1;
		if (player.onGround() && !player.isInWater() && !player.isPassenger()
				&& !player.getAbilities().flying) {
			jump.setDown(true);
			pressed = true;
		}
	}
}
