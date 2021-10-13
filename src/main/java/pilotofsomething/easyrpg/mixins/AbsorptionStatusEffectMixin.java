package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.AbsorptionStatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AbsorptionStatusEffect.class)
public class AbsorptionStatusEffectMixin {

	@ModifyConstant(method = "onRemoved(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/attribute/AttributeContainer;I)V", constant = @Constant(intValue = 4))
	private int onRemoved(int a, LivingEntity entity, AttributeContainer attributes, int amplifier) {
		return Math.max((int) (entity.getMaxHealth() * 0.2), 4);
	}

	@ModifyConstant(method = "onApplied(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/attribute/AttributeContainer;I)V", constant = @Constant(intValue = 4))
	private int onApplied(int a, LivingEntity entity, AttributeContainer attributes, int amplifier) {
		return Math.max((int) (entity.getMaxHealth() * 0.2), 4);
	}

}
