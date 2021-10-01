package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(StatusEffect.class)
public class StatusEffectMixin {

	@ModifyConstant(method = "applyUpdateEffect(Lnet/minecraft/entity/LivingEntity;I)V", constant = @Constant(floatValue = 1.0f, ordinal = 1))
	private float applyUpdateEffect(float value, LivingEntity entity, int amplifier) {
		return (float)Math.max(Math.floor(entity.getMaxHealth() * 0.075f), 1.0);
	}

}
