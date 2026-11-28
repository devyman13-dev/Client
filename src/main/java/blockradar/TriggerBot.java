package blockradar;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Attacks for you the moment your crosshair is on a player or mob. It sends the same left
 * click a real mouse press would, so you still aim and move yourself.
 */
public final class TriggerBot {
	private TriggerBot() {}

	/** Ticks since the last attack it sent. */
	private static int sinceAttack = 100;

	/** Called every game tick. */
	public static void tick(Minecraft client) {
		sinceAttack++;
		LocalPlayer player = client.player;
		if (!Settings.triggerBot || player == null || client.screen != null || Freecam.active()) return;
		if (player.isUsingItem() || player.isSpectator()) return;
		if (!(client.hitResult instanceof EntityHitResult hit) || hit.getType() != HitResult.Type.ENTITY) return;
		if (!wanted(hit.getEntity())) return;
		if (Settings.triggerCooldown && player.getAttackStrengthScale(0.5F) < 1.0F) return;
		if (sinceAttack < Settings.triggerDelay) return;

		// Queue a left click the way a real mouse press would; the game handles it next tick.
		KeyMapping attack = client.options.keyAttack;
		KeyMapping.click(InputConstants.getKey(attack.saveString()));
		sinceAttack = 0;
	}

	/** Whether this is something the settings allow it to hit. */
	private static boolean wanted(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
		if (entity instanceof Player) return Settings.triggerPlayers;
		if (entity instanceof Enemy) return Settings.triggerHostile;
		return Settings.triggerPassive;
	}
}
