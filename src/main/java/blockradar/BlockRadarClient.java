package blockradar;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlockRadarClient implements ClientModInitializer {
	public static final Scanner SCANNER = new Scanner();

	/** Chunk color. */
	static final int CHUNK_BLUE = 0xFF2F7BFF;
	/** The "click to see" check runs once every this many ticks. */
	private static final int HINT_TICKS = 4;

	/** What a left click would open right now. */
	private record Target(String hint, Supplier<Screen> screen) {}

	private boolean hintShowing;
	private int ticks;

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
		KeyMapping chunkKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.blockradar.chunk", InputConstants.KEY_N, keys));

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("blockradar", "hud"), RadarHud::render);

		// Left click. Returning true stops the game from also breaking the block.
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (clickCount == 0 || !Settings.radarOn || client.screen != null) return false;
			Target target = target(client, player);
			if (target == null) return false;
			client.setScreen(target.screen().get());
			return true;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				Settings.radarOn = !Settings.radarOn;
				saveSettings();
				SCANNER.clear();
			}
			while (menuKey.consumeClick()) {
				client.setScreen(new RadarScreen());
			}
			while (chunkKey.consumeClick()) {
				if (Settings.radarOn && client.player != null) {
					Scanner.Chunk here = new Scanner.Chunk(
						client.player.getBlockX() >> 4, client.player.getBlockZ() >> 4);
					client.setScreen(new ChunkScreen(SCANNER.report(here)));
				}
			}
			if (Settings.radarOn) SCANNER.tick(client);

			if (Settings.radarOn) drawOutlines(client);
			RadarHud.tick();
			if (++ticks % HINT_TICKS == 0) showHint(client);
		});
	}

	/** Outlines drawn during a tick stay on screen until the next tick. */
	private void drawOutlines(Minecraft client) {
		try (var collection = client.collectPerTickGizmos()) {
			if (Settings.outlines) {
				for (Scanner.Hit hit : SCANNER.drawn()) {
					int color = hit.color();
					// The same color, mostly see-through, or nothing.
					int fill = Settings.fill ? Theme.alpha(color, 0x28) : 0;
					Gizmos.cuboid(new AABB(hit.pos()), GizmoStyle.strokeAndFill(color, 2.0F, fill))
						.setAlwaysOnTop();
				}
			}
			if (Settings.chunks != ChunkStyle.OFF && client.level != null && client.player != null) {
				for (Scanner.Chunk chunk : SCANNER.marked()) {
					AABB column = column(client.level, chunk);
					// The outline shows through walls.
					Gizmos.cuboid(column, GizmoStyle.strokeAndFill(CHUNK_BLUE, 3.0F, 0))
						.setAlwaysOnTop();
					// The solid fill is hidden by walls like a real block, and cleared when
					// you are in or near the chunk so you can see the blocks you dig.
					if (Settings.chunks.fills(chunk.x(), chunk.z(),
							client.player.getX(), client.player.getZ())) {
						Gizmos.cuboid(column, GizmoStyle.strokeAndFill(CHUNK_BLUE, 3.0F, CHUNK_BLUE));
					}
				}
			}
		}
	}

	/** The blue shape of a chunk, from the bottom of the world to the chosen height. */
	private static AABB column(Level level, Scanner.Chunk chunk) {
		int x = chunk.x() << 4;
		int z = chunk.z() << 4;
		return new AABB(x, level.getMinY(), z,
			x + 16, ChunkStyle.top(level.getMinY(), level.getMaxY(), Settings.chunkBlocks), z + 16);
	}

	/** Shows "click to see" above the hotbar while there is something to open. */
	private void showHint(Minecraft client) {
		String hint = null;
		if (Settings.hints && Settings.radarOn && client.screen == null && client.player != null) {
			Target target = target(client, client.player);
			if (target != null) {
				hint = target.hint();
			} else {
				Scanner.Hit aimed = aimedHit(client);
				if (aimed != null) hint = "Sneak + left click to see: " + aimed.block().getName().getString();
			}
		}
		if (hint != null) {
			client.gui.setOverlayMessage(Component.literal(hint), false);
		} else if (hintShowing) {
			client.gui.setOverlayMessage(Component.empty(), false);
		}
		hintShowing = hint != null;
	}

	/** The highlighted block the player could break right now, or null. */
	private static Scanner.Hit aimedHit(Minecraft client) {
		if (Settings.outlines && client.hitResult instanceof BlockHitResult block
				&& block.getType() == HitResult.Type.BLOCK) {
			return SCANNER.drawnAt(block.getBlockPos());
		}
		return null;
	}

	/**
	 * Finds what a left click would open: a highlighted block first, then a marked chunk.
	 * A normal click on a block in reach always breaks it, and mobs can still be hit.
	 * Sneak and click to see a highlighted block that is in reach or behind a block.
	 *
	 * @return null if a left click should do what it normally does
	 */
	private Target target(Minecraft client, LocalPlayer player) {
		if (client.level == null) return null;
		HitResult aim = client.hitResult;
		if (aim != null && aim.getType() == HitResult.Type.ENTITY) return null;
		boolean blockInReach = aim != null && aim.getType() == HitResult.Type.BLOCK;
		if (blockInReach && !player.isShiftKeyDown()) return null; // digging

		// Look for a highlight along the line of sight.
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(1024));

		Scanner.Hit best = null;
		double bestDistance = Double.MAX_VALUE;
		// Blocks can only be clicked while their boxes are showing.
		for (Scanner.Hit hit : Settings.outlines ? SCANNER.drawn() : List.<Scanner.Hit>of()) {
			Optional<Vec3> point = new AABB(hit.pos()).clip(eye, end);
			if (point.isEmpty()) continue;
			double distance = eye.distanceToSqr(point.get());
			if (distance < bestDistance) {
				best = hit;
				bestDistance = distance;
			}
		}
		if (best != null) return blockTarget(best);
		// Chunks only open when no block is in reach, so sneak-digging keeps working.
		if (blockInReach || Settings.chunks == ChunkStyle.OFF) return null;

		Scanner.Chunk nearest = null;
		for (Scanner.Chunk chunk : SCANNER.marked()) {
			// The chunk you stand in is opened with its own key.
			if (chunk.x() == player.getBlockX() >> 4 && chunk.z() == player.getBlockZ() >> 4) continue;
			Optional<Vec3> point = column(client.level, chunk).clip(eye, end);
			if (point.isEmpty()) continue;
			double distance = eye.distanceToSqr(point.get());
			if (distance < bestDistance) {
				nearest = chunk;
				bestDistance = distance;
			}
		}
		return nearest == null ? null : chunkTarget(nearest);
	}

	private static Target blockTarget(Scanner.Hit hit) {
		return new Target("Left click to see: " + hit.block().getName().getString(),
			() -> new InfoScreen(hit));
	}

	private static Target chunkTarget(Scanner.Chunk chunk) {
		return new Target("Left click to see chunk " + chunk.x() + ", " + chunk.z(),
			() -> new ChunkScreen(SCANNER.report(chunk)));
	}
}
