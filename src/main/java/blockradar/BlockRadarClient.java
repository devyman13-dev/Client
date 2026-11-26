package blockradar;

import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockRadarClient implements ClientModInitializer {
	public static final Scanner SCANNER = new Scanner();
	public static boolean active = true;

	private boolean clickWasDown;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category keys = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("blockradar", "main"));
		KeyMapping menuKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.blockradar.menu", InputConstants.KEY_J, keys));
		KeyMapping toggleKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.blockradar.toggle", InputConstants.KEY_K, keys));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				active = !active;
				SCANNER.clear();
			}
			while (menuKey.consumeClick()) {
				client.setScreen(new RadarScreen());
			}
			if (active) SCANNER.tick(client);

			// "Pick block" is middle click by default.
			boolean clickDown = client.options.keyPickItem.isDown();
			if (active && clickDown && !clickWasDown && client.screen == null) {
				inspect(client);
			}
			clickWasDown = clickDown;

			if (active) drawOutlines(client);
		});
	}

	/** Outlines drawn during a tick stay on screen until the next tick. */
	private void drawOutlines(Minecraft client) {
		try (var collection = client.collectPerTickGizmos()) {
			for (Scanner.Hit hit : SCANNER.drawn()) {
				Gizmos.cuboid(new AABB(hit.pos()), GizmoStyle.stroke(hit.category().color))
					.setAlwaysOnTop();
			}
		}
	}

	/** Finds the highlighted block under the crosshair, even through walls, and shows its counts. */
	private void inspect(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return;
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
		if (best != null) client.setScreen(new InfoScreen(best));
	}
}
