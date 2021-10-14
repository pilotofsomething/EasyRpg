package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClampedEntityAttribute.class)
public class ClampedEntityAttributeMixin {

	@Shadow @Final private double minValue;

	@Inject(method = "clamp(D)D", at = @At(value = "HEAD"), cancellable = true)
	private void clamp(double value, CallbackInfoReturnable<Double> cir) {
		if((Object) this == EntityAttributes.GENERIC_MAX_HEALTH || (Object) this == EntityAttributes.GENERIC_ARMOR_TOUGHNESS) {
			cir.setReturnValue(MathHelper.clamp(value, minValue, Integer.MAX_VALUE));
		}
	}

	@Inject(method = "getMaxValue()D", at = @At("HEAD"), cancellable = true)
	private void getMaxValue(CallbackInfoReturnable<Double> cir) {
		if((Object) this == EntityAttributes.GENERIC_MAX_HEALTH || (Object) this == EntityAttributes.GENERIC_ARMOR_TOUGHNESS) {
			cir.setReturnValue((double) Integer.MAX_VALUE);
		}
	}

}
