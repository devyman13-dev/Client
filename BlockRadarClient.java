package blockradar;

import java.nio.file.Path;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockRadarClient implements ClientModInitializer {
	public static final Scanner SCANNER = new Scanner();

	private boolean clickWasDown;

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
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("blockradar", "activity_hud"),
			(graphics, delta) -> drawActivityHud(graphics));

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

			// "Pick block" is middle click by default.
			boolean clickDown = client.options.keyPickItem.isDown();
			if (Settings.radarOn && clickDown && !clickWasDown && client.screen == null) {
				inspect(client);
			}
			clickWasDown = clickDown;

			if (Settings.radarOn) drawOutlines(client);
		});
	}

	/** Outlines drawn during a tick stay on screen until the next tick. */
	private void drawOutlines(Minecraft client) {
		try (var collection = client.collectPerTickGizmos()) {
			if (client.level != null) {
				for (long key : SCANNER.drawnChunks()) {
					int chunkX = (int) (key >> 32);
					int chunkZ = (int) key;
					double minY = client.level.getMinY();
					double maxY = client.level.getMaxY();
					AABB border = new AABB(chunkX << 4, minY, chunkZ << 4,
						(chunkX + 1) << 4, maxY, (chunkZ + 1) << 4);
					Gizmos.cuboid(border, GizmoStyle.strokeAndFill(0xFF4B9EFF, 2.25F, 0x060A4CFF))
						.setAlwaysOnTop();
				}
			}
			for (Scanner.Hit hit : SCANNER.drawn()) {
				int color = hit.category().color;
				int fill = (color & 0x00FFFFFF) | 0x28000000; // same color, mostly see-through
				Gizmos.cuboid(new AABB(hit.pos()), GizmoStyle.strokeAndFill(color, 2.0F, fill))
					.setAlwaysOnTop();
			}
		}
	}

	private static void drawActivityHud(GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		if (!Settings.radarOn || client.options.hideGui || client.player == null) return;
		Scanner.Stats stats = SCANNER.cachedStats();
		String title = "BLOCK RADAR  ·  ACTIVE";
		String activity = stats.suspiciousChunks() + " suspicious chunks  ·  " + Format.count(stats.suspicious()) + " blocks";
		int effectiveRange = Math.min(Settings.scanRange, client.options.getEffectiveRenderDistance());
		String range = "Scan radius: " + effectiveRange + " chunks";
		int width = Math.max(client.font.width(title), Math.max(client.font.width(activity), client.font.width(range))) + 18;
		int x = graphics.guiWidth() - width - 8;
		Theme.box(graphics, x, 8, width, 43, Theme.PANEL);
		graphics.fill(x + 8, 8, x + width - 8, 10, Theme.ACCENT);
		Theme.text(graphics, client.font, title, x + 9, 14, Theme.TEXT);
		Theme.text(graphics, client.font, activity, x + 9, 25, 0xFF8FC7FF);
		Theme.text(graphics, client.font, range, x + 9, 36, Theme.MUTED);
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
