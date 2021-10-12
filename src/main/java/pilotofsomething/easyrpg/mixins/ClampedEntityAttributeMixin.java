package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClampedEntityAttribute.class)
public class ClampedEntityAttributeMixin {

	@ModifyArg(method = "clamp(D)D", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D"), index = 2)
	private double injectClamp(double value) {
		if((Object) this == EntityAttributes.GENERIC_MAX_HEALTH || (Object) this == EntityAttributes.GENERIC_ARMOR_TOUGHNESS) {
			return Integer.MAX_VALUE;
		}
		return value;
	}

	@Inject(method = "getMaxValue()D", at = @At("HEAD"), cancellable = true)
	private void getMaxValue(CallbackInfoReturnable<Double> cir) {
		if((Object) this == EntityAttributes.GENERIC_MAX_HEALTH || (Object) this == EntityAttributes.GENERIC_ARMOR_TOUGHNESS) {
			cir.setReturnValue((double) Integer.MAX_VALUE);
		}
	}

}
