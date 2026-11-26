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
	private static Scanner.Hit inspectedHit;

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
					drawChunkBorder(client, key);
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

	/** F3+G-style full-height edges plus a bright border around the player's 16-block section. */
	private static void drawChunkBorder(Minecraft client, long key) {
		int chunkX = (int) (key >> 32);
		int chunkZ = (int) key;
		double minX = chunkX << 4;
		double maxX = (chunkX + 1) << 4;
		double minZ = chunkZ << 4;
		double maxZ = (chunkZ + 1) << 4;
		double minY = client.level.getMinY();
		double maxY = client.level.getMaxY();
		int blue = 0xFF006BFF;
		Gizmos.cuboid(new AABB(minX, minY, minZ, maxX, maxY, maxZ), GizmoStyle.stroke(blue, 4.0F))
			.setAlwaysOnTop();

		int playerY = client.player == null ? (int) minY : client.player.getBlockY();
		double sectionBottom = Math.floorDiv(playerY, 16) * 16.0;
		drawChunkSection(client, minX, maxX, minZ, maxZ, sectionBottom, blue);
		drawChunkSection(client, minX, maxX, minZ, maxZ, sectionBottom + 16.0, blue);
	}

	private static void drawChunkSection(Minecraft client, double minX, double maxX, double minZ,
		double maxZ, double y, int color) {
		if (y < client.level.getMinY() || y > client.level.getMaxY()) return;
		Vec3 a = new Vec3(minX, y, minZ);
		Vec3 b = new Vec3(maxX, y, minZ);
		Vec3 c = new Vec3(maxX, y, maxZ);
		Vec3 d = new Vec3(minX, y, maxZ);
		Gizmos.line(a, b, color, 3.0F).setAlwaysOnTop();
		Gizmos.line(b, c, color, 3.0F).setAlwaysOnTop();
		Gizmos.line(c, d, color, 3.0F).setAlwaysOnTop();
		Gizmos.line(d, a, color, 3.0F).setAlwaysOnTop();
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
		if (inspectedHit != null) drawInspectHud(graphics, inspectedHit, stats);
	}

	/** Compact half-scale details card shown after middle-clicking a highlighted block. */
	private static void drawInspectHud(GuiGraphics graphics, Scanner.Hit hit, Scanner.Stats stats) {
		Minecraft client = Minecraft.getInstance();
		float scale = 0.5F;
		int boxWidth = 420;
		int boxHeight = 112;
		float x = graphics.guiWidth() - boxWidth * scale - 8;
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, 58);
		pose.scale(scale, scale);
		Theme.box(graphics, 0, 0, boxWidth, boxHeight, Theme.PANEL);
		Theme.outline(graphics, 0, 0, boxWidth, boxHeight, hit.category().color);
		Theme.text(graphics, client.font, hit.block().getName().getString(), 14, 13, Theme.TEXT);
		Theme.pill(graphics, client.font, hit.category().label.toUpperCase(), 270, 9, 132, hit.category().color);
		Theme.text(graphics, client.font, "Position  " + hit.pos().getX() + ", " + hit.pos().getY() + ", "
			hit.pos().getZ(), 14, 38, Theme.MUTED);
		int count = stats.byBlock().stream().filter(entry -> entry.getKey() == hit.block())
			.mapToInt(entry -> entry.getValue()).sum();
		Theme.text(graphics, client.font, "Scanned total  " + Format.count(count), 14, 58, Theme.TEXT);
		Theme.text(graphics, client.font, "Middle-click another highlighted block to inspect it", 14, 84, Theme.MUTED);
		pose.popMatrix();
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
		inspectedHit = best;
	}
}
