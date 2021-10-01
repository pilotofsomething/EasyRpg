package pilotofsomething.easyrpg.mixins;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pilotofsomething.easyrpg.gui.CustomInGameHud;
import pilotofsomething.easyrpg.ConfigKt;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(method = "renderStatusBars(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At(value = "HEAD"), cancellable = true)
	private void renderStatusBars(MatrixStack matrices, CallbackInfo ci) {
		if(ConfigKt.config.getClient().getRenderCustomHud()) {
			PlayerEntity player = getCameraPlayer();
			if(player != null) {
				CustomInGameHud.INSTANCE.renderStatusBars(matrices, player, scaledWidth, scaledHeight);
				ci.cancel();
			}
		}
	}

	@Inject(method = "renderMountHealth(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("HEAD"), cancellable = true)
	private void renderMountHealth(MatrixStack matrices, CallbackInfo ci) {
		if(ConfigKt.config.getClient().getRenderCustomHud()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderExperienceBar(Lnet/minecraft/client/util/math/MatrixStack;I)V", at = @At("HEAD"), cancellable = true)
	private void renderExperienceBar(MatrixStack matrices, int x, CallbackInfo ci) {
		if(ConfigKt.config.getClient().getRenderCustomHud()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderMountJumpBar(Lnet/minecraft/client/util/math/MatrixStack;I)V", at = @At("HEAD"), cancellable = true)
	private void renderMountJumpBar(MatrixStack matrices, int x, CallbackInfo ci) {
		if(ConfigKt.config.getClient().getRenderCustomHud()) {
			ci.cancel();
		}
	}

	@Shadow
	private PlayerEntity getCameraPlayer() { return null; }

	@Shadow
	private int scaledWidth;
	@Shadow
	private int scaledHeight;

}
