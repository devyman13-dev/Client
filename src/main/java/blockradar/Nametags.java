package blockradar;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Names above the players and mobs that Entity ESP outlines. They are drawn on the screen
 * (not in the world), so they show through walls and stay the same size at any distance.
 */
public final class Nametags {
	private Nametags() {}

	/** An entity to label and the color of its outline. */
	public record Tag(Entity entity, int color) {}

	/** Picked once per tick by the ESP, drawn every frame. */
	private static List<Tag> tags = List.of();

	static void set(List<Tag> next) {
		tags = next;
	}

	public static void render(GuiGraphics g, Minecraft client, DeltaTracker delta) {
		LocalPlayer player = client.player;
		if (player == null || tags.isEmpty()) return;
		float partial = delta.getGameTimeDeltaPartialTick(false);

		Vec3 eye = Freecam.active() ? Freecam.eye(partial) : player.getEyePosition(partial);
		double yaw = Math.toRadians(player.getViewYRot(partial));
		double pitch = Math.toRadians(player.getViewXRot(partial));
		Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		Vec3 up = right.cross(forward);

		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		double tan = Math.tan(Math.toRadians(client.options.fov().get()) / 2.0);
		double aspect = (double) width / height;
		Font font = client.font;

		for (Tag tag : tags) {
			Entity entity = tag.entity();
			if (entity.isRemoved()) continue;
			// Above the head, smoothed between ticks.
			double x = Mth.lerp(partial, entity.xo, entity.getX());
			double y = Mth.lerp(partial, entity.yo, entity.getY()) + entity.getBbHeight() + 0.35;
			double z = Mth.lerp(partial, entity.zo, entity.getZ());
			double dx = x - eye.x;
			double dy = y - eye.y;
			double dz = z - eye.z;

			double depth = dx * forward.x + dy * forward.y + dz * forward.z;
			if (depth < 0.1) continue; // behind the camera
			double ndcX = (dx * right.x + dy * right.y + dz * right.z) / (depth * tan * aspect);
			double ndcY = (dx * up.x + dy * up.y + dz * up.z) / (depth * tan);
			if (Math.abs(ndcX) > 1.1 || Math.abs(ndcY) > 1.1) continue;
			int sx = (int) ((ndcX * 0.5 + 0.5) * width);
			int sy = (int) ((0.5 - ndcY * 0.5) * height);

			StringBuilder label = new StringBuilder(entity.getName().getString());
			if (Settings.espHealth && entity instanceof LivingEntity living) {
				label.append("  ").append((int) Math.ceil(living.getHealth())).append(" HP");
			}
			if (Settings.espDistance) {
				label.append("  ").append((int) Math.sqrt(dx * dx + dy * dy + dz * dz)).append("m");
			}
			String text = label.toString();
			int half = font.width(text) / 2;
			g.fill(sx - half - 2, sy - 2, sx + half + 2, sy + 10, Theme.HUD);
			Theme.centered(g, font, text, sx, sy, tag.color());

		}
	}
}
