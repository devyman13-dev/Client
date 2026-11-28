package blockradar.mixin;

import blockradar.NoFall;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the part of the player's tick that sends its position to the server. For that moment
 * only, the player is treated as standing on the ground when No fall asks for it, and then it
 * goes back to the real value, so your own movement is not affected.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Unique
	private boolean blockradar$spoofed;
	@Unique
	private boolean blockradar$realOnGround;

	@Inject(method = "sendPosition", at = @At("HEAD"))
	private void blockradar$claimOnGround(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		blockradar$spoofed = NoFall.shouldSpoof(self);
		if (blockradar$spoofed) {
			blockradar$realOnGround = self.onGround();
			self.setOnGround(true);
		}
	}

	@Inject(method = "sendPosition", at = @At("RETURN"))
	private void blockradar$restoreOnGround(CallbackInfo ci) {
		if (blockradar$spoofed) {
			((LocalPlayer) (Object) this).setOnGround(blockradar$realOnGround);
			blockradar$spoofed = false;
		}
	}
}
