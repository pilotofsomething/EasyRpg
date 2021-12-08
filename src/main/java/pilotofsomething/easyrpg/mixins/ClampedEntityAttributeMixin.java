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
import pilotofsomething.easyrpg.EasyRpgAttributes;
import pilotofsomething.easyrpg.EasyRpgKt;

@Mixin(ClampedEntityAttribute.class)
public class ClampedEntityAttributeMixin {

	@Shadow @Final private double minValue;

	@Shadow @Final private double maxValue;

	@Inject(method = "clamp(D)D", at = @At(value = "HEAD"), cancellable = true)
	private void clamp(double value, CallbackInfoReturnable<Double> cir) {
		double max = EasyRpgKt.getAttributeMax((ClampedEntityAttribute)(Object)this);
		if(max != maxValue) {
			cir.setReturnValue(MathHelper.clamp(value, minValue, max));
		}
	}

	@Inject(method = "getMaxValue()D", at = @At("HEAD"), cancellable = true)
	private void getMaxValue(CallbackInfoReturnable<Double> cir) {
		ClampedEntityAttribute att = (ClampedEntityAttribute)(Object)this;
		if(att == EntityAttributes.GENERIC_MAX_HEALTH || att == EntityAttributes.GENERIC_ARMOR_TOUGHNESS ||
				att == EasyRpgAttributes.INSTANCE.getSTRENGTH() || att == EasyRpgAttributes.INSTANCE.getDEXTERITY() ||
				att == EasyRpgAttributes.INSTANCE.getINTELLIGENCE() || att == EasyRpgAttributes.INSTANCE.getDEFENSE()) {
			cir.setReturnValue(EasyRpgKt.getAttributeMax(att));
		}
	}

}
