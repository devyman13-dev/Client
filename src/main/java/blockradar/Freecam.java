package blockradar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A camera that flies through walls and underground. Your character stays where it is.
 * The game's own camera is pointed at a stand-in entity that is moved here, and the
 * character's movement keys are switched off while it is on.
 */
public final class Freecam {
	private Freecam() {}

	/** An entity that only exists to be looked through. It is never added to the world. */
	private static final class CameraEntity extends ArmorStand {
		CameraEntity(Level level) {
			super(EntityType.ARMOR_STAND, level);
		}

		// Turning the mouse turns the player, so the camera simply looks where the player looks.
		public float getViewYRot(float partialTick) {
			LocalPlayer player = Minecraft.getInstance().player;
			return player != null ? player.getViewYRot(partialTick) : super.getViewYRot(partialTick);
		}

		public float getViewXRot(float partialTick) {
			LocalPlayer player = Minecraft.getInstance().player;
			return player != null ? player.getViewXRot(partialTick) : super.getViewXRot(partialTick);
		}
	}

	private static CameraEntity camera;
	private static LocalPlayer owner;
	private static ClientLevel world;
	private static ClientInput savedInput;
	private static float savedYaw;
	private static float savedPitch;
	/** Where the camera's eye is, and how fast it is moving (blocks per tick). */
	private static Vec3 eye = Vec3.ZERO;
	private static Vec3 lastEye = Vec3.ZERO;
	private static Vec3 velocity = Vec3.ZERO;

	public static boolean active() {
		return camera != null;
	}

	/** The stand-in entity while freecam is on, otherwise null. */
	public static net.minecraft.world.entity.Entity camera() {
		return camera;
	}

	/** Where the camera's eye is. Only meaningful while freecam is on. */
	public static Vec3 eye() {
		return eye;
	}

	/** Where the camera's eye is, smoothed between ticks. */
	public static Vec3 eye(float partialTick) {
		return lastEye.lerp(eye, partialTick);
	}

	/** The block the radar should work around: the camera while flying, else the player. */
	public static BlockPos anchor(LocalPlayer player) {
		return camera != null ? BlockPos.containing(eye) : player.blockPosition();
	}

	public static void toggle(Minecraft client) {
		if (camera != null) disable(client);
		else enable(client);
	}

	public static void enable(Minecraft client) {
		LocalPlayer player = client.player;
		if (camera != null || player == null || client.level == null) return;
		owner = player;
		world = client.level;
		savedYaw = player.getYRot();
		savedPitch = player.getXRot();
		eye = player.getEyePosition();
		lastEye = eye;
		velocity = Vec3.ZERO;

		camera = new CameraEntity(world);
		place(player);
		camera.xo = camera.getX();
		camera.yo = camera.getY();
		camera.zo = camera.getZ();

		// The character stops reacting to the movement keys until freecam is turned off.
		savedInput = player.input;
		player.input = new ClientInput() {};
		client.setCameraEntity(camera);
		player.displayClientMessage(Component.literal("Freecam on"), true);
	}

	public static void disable(Minecraft client) {
		LocalPlayer player = client.player;
		if (camera != null && player != null && player == owner) {
			player.input = savedInput;
			client.setCameraEntity(player);
			if (Settings.freecamRestoreView) {
				player.setYRot(savedYaw);
				player.setXRot(savedPitch);
				player.yRotO = savedYaw;
				player.xRotO = savedPitch;
			}
			player.displayClientMessage(Component.literal("Freecam off"), true);
		}
		forget();
	}

	/** Drops everything without touching the game, for when the world or player is gone. */
	private static void forget() {
		camera = null;
		owner = null;
		world = null;
		savedInput = null;
		velocity = Vec3.ZERO;
	}

	/** Called every game tick. */
	public static void tick(Minecraft client) {
		if (camera == null) return;
		LocalPlayer player = client.player;
		if (player == null || player != owner || client.level == null || client.level != world) {
			forget(); // joined another world or died: the game has already reset the camera
			return;
		}
		if (Settings.freecamExitOnHurt && player.hurtTime > 0) {
			disable(client);
			return;
		}

		Vec3 wish = Vec3.ZERO;
		if (client.screen == null) {
			Options keys = client.options;
			double yaw = Math.toRadians(player.getYRot());
			double pitch = Math.toRadians(player.getXRot());
			Vec3 look = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
			Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
			if (keys.keyUp.isDown()) wish = wish.add(look);
			if (keys.keyDown.isDown()) wish = wish.subtract(look);
			if (keys.keyRight.isDown()) wish = wish.add(right);
			if (keys.keyLeft.isDown()) wish = wish.subtract(right);
			if (keys.keyJump.isDown()) wish = wish.add(0, 1, 0);
			if (keys.keyShift.isDown()) wish = wish.add(0, -1, 0);
		}
		double perTick = Settings.freecamSpeed / 20.0
			* (client.options.keySprint.isDown() ? Settings.freecamBoost : 1);
		Vec3 target = wish.lengthSqr() > 1.0E-6 ? wish.normalize().scale(perTick) : Vec3.ZERO;
		velocity = Settings.freecamSmooth ? velocity.lerp(target, 0.4) : target;

		lastEye = eye;
		eye = eye.add(velocity);
		eye = new Vec3(eye.x, Mth.clamp(eye.y, world.getMinY() - 16, world.getMaxY() + 256), eye.z);
		camera.xo = camera.getX();
		camera.yo = camera.getY();
		camera.zo = camera.getZ();
		place(player);
	}

	/** Puts the stand-in entity so that its eye is at "eye", looking where the player looks. */
	private static void place(LocalPlayer player) {
		camera.setPos(eye.x, eye.y - camera.getEyeHeight(), eye.z);
		camera.setYRot(player.getYRot());
		camera.setXRot(player.getXRot());
		camera.yRotO = player.yRotO;
		camera.xRotO = player.xRotO;
	}
}
