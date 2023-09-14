package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pilotofsomething.easyrpg.EasyRpgAttributes;
import pilotofsomething.easyrpg.entity.LivingEntityKt;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
	private float damage(float val, DamageSource source, float amount) {
		return LivingEntityKt.calculateDamage((LivingEntity)(Object)this, amount, source);
	}

	@ModifyVariable(method = "heal(F)V", at = @At("HEAD"), argsOnly = true)
	private float heal(float amount) {
		return LivingEntityKt.calculateHealing((LivingEntity)(Object)this, amount);
	}

	@Inject(method = "createLivingAttributes()Lnet/minecraft/entity/attribute/DefaultAttributeContainer$Builder;", at = @At("RETURN"))
	private static void createLivingAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
		cir.getReturnValue()
				.add(EasyRpgAttributes.INSTANCE.getSTRENGTH())
				.add(EasyRpgAttributes.INSTANCE.getDEXTERITY())
				.add(EasyRpgAttributes.INSTANCE.getINTELLIGENCE())
				.add(EasyRpgAttributes.INSTANCE.getDEFENSE());
	}

	@Shadow
	public float getMaxHealth() {
		return 0f;
	}
}
