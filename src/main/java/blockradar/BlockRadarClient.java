package blockradar;

import java.nio.file.Path;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlockRadarClient implements ClientModInitializer {
	public static final Scanner SCANNER = new Scanner();

	/** Chunk outline color. */
	private static final int CHUNK_BLUE = 0xFF2F7BFF;

	private static Path settingsFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("blockradar.properties");
	}

	/** Call after the player changes any setting. */
	public static void saveSettings() {
		Settings.save(settingsFile());
	}

	@Override
	public void onInitializeClient() {
		Settings.load(settingsFile());
		SCANNER.restoreFocus(Settings.onlyBlock);

		KeyMapping.Category keys = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("blockradar", "main"));
		KeyMapping menuKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.blockradar.menu", InputConstants.KEY_J, keys));
		KeyMapping toggleKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.blockradar.toggle", InputConstants.KEY_K, keys));

		// Left click. Returning true stops the game from also breaking the block.
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) ->
			clickCount > 0 && Settings.radarOn && client.screen == null && inspect(client, player));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				Settings.radarOn = !Settings.radarOn;
				saveSettings();
				SCANNER.clear();
			}
			while (menuKey.consumeClick()) {
				client.setScreen(new RadarScreen());
			}
			if (Settings.radarOn) SCANNER.tick(client);

			if (Settings.radarOn) drawOutlines(client);
		});
	}

	/** Outlines drawn during a tick stay on screen until the next tick. */
	private void drawOutlines(Minecraft client) {
		try (var collection = client.collectPerTickGizmos()) {
			for (Scanner.Hit hit : SCANNER.drawn()) {
				int color = hit.color();
				int fill = (color & 0x00FFFFFF) | 0x28000000; // same color, mostly see-through
				Gizmos.cuboid(new AABB(hit.pos()), GizmoStyle.strokeAndFill(color, 2.0F, fill))
					.setAlwaysOnTop();
			}
			if (Settings.chunkOutlines && client.level != null) {
				int bottom = client.level.getMinY();
				int top = client.level.getMaxY() + 1;
				for (Scanner.Chunk chunk : SCANNER.marked()) {
					int x = chunk.x() << 4;
					int z = chunk.z() << 4;
					Gizmos.cuboid(new AABB(x, bottom, z, x + 16, top, z + 16),
						GizmoStyle.strokeAndFill(CHUNK_BLUE, 3.0F, 0)) // lines only
						.setAlwaysOnTop();
				}
			}
		}
	}

	/**
	 * Shows the counts for the highlighted block under the crosshair, if there is one.
	 * Mobs can still be hit, and normal blocks in reach can still be mined. Holding sneak
	 * skips past a normal block to the highlight behind it.
	 *
	 * @return true if the counts were opened
	 */
	private boolean inspect(Minecraft client, LocalPlayer player) {
		HitResult target = client.hitResult;
		if (target != null && target.getType() == HitResult.Type.ENTITY) return false;
		if (target instanceof BlockHitResult block && target.getType() == HitResult.Type.BLOCK) {
			for (Scanner.Hit hit : SCANNER.drawn()) {
				if (hit.pos().equals(block.getBlockPos())) return open(client, hit);
			}
			if (!player.isShiftKeyDown()) return false; // mining a normal block
		}

		// Nothing in the way: look for a highlight along the line of sight.
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(1024));

		Scanner.Hit best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Scanner.Hit hit : SCANNER.drawn()) {
			Optional<Vec3> point = new AABB(hit.pos()).clip(eye, end);
			if (point.isEmpty()) continue;
			double distance = eye.distanceToSqr(point.get());
			if (distance < bestDistance) {
				best = hit;
				bestDistance = distance;
			}
		}
		return best != null && open(client, best);
	}

	private static boolean open(Minecraft client, Scanner.Hit hit) {
		client.setScreen(new InfoScreen(hit));
		return true;
	}
}
