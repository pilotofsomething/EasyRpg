package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.AbsorptionStatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import pilotofsomething.easyrpg.components.IRpgEntity;
import pilotofsomething.easyrpg.components.RpgMobKt;
import pilotofsomething.easyrpg.components.RpgPlayerKt;

@Mixin(AbsorptionStatusEffect.class)
public class AbsorptionStatusEffectMixin {

	@ModifyConstant(method = "onRemoved(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/attribute/AttributeContainer;I)V", constant = @Constant(intValue = 4))
	private int onRemoved(int a, LivingEntity entity, AttributeContainer attributes, int amplifier) {
		IRpgEntity rpg;
		if(entity instanceof PlayerEntity) {
			rpg = RpgPlayerKt.getRPG_PLAYER().get(entity);
		} else rpg = RpgMobKt.getRPG_MOB().get(entity);
		int value = rpg.getAbsorptionAmount();
		rpg.setAbsorptionAmount(0);
		return value;
	}

	@ModifyConstant(method = "onApplied(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/attribute/AttributeContainer;I)V", constant = @Constant(intValue = 4))
	private int onApplied(int a, LivingEntity entity, AttributeContainer attributes, int amplifier) {
		IRpgEntity rpg;
		if(entity instanceof PlayerEntity) {
			rpg = RpgPlayerKt.getRPG_PLAYER().get(entity);
		} else rpg = RpgMobKt.getRPG_MOB().get(entity);
		rpg.setAbsorptionAmount(Math.max((int) (entity.getMaxHealth() * 0.2), 4));
		return Math.max((int) (entity.getMaxHealth() * 0.2), 4);
	}

}
