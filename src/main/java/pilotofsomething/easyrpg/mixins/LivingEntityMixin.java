package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import pilotofsomething.easyrpg.SimpleRpgAttributes;
import pilotofsomething.easyrpg.components.IRpgEntity;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.components.RpgMobKt;
import pilotofsomething.easyrpg.components.RpgPlayerKt;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
	private float damage(float val, DamageSource source, float amount) {
		if(source.getAttacker() instanceof LivingEntity attacker) {
			IRpgEntity rpg;
			if(attacker instanceof PlayerEntity) {
				rpg = RpgPlayerKt.getRPG_PLAYER().get(attacker);
			} else rpg = RpgMobKt.getRPG_MOB().get(attacker);

			float damage = amount;

			if(attacker instanceof PlayerEntity) {
				damage *= Math.min(ConfigKt.config.getPlayers().getDamage().getBase()
								+ ConfigKt.config.getPlayers().getDamage().getGain() * (rpg.getLevel() - 1),
						ConfigKt.getConfig().getPlayers().getDamage().getCap());
			} else {
				damage *= Math.min(ConfigKt.config.getEntities().getDamage().getBase()
								+ ConfigKt.config.getEntities().getDamage().getGain() * (rpg.getLevel() - 1),
						ConfigKt.getConfig().getEntities().getDamage().getCap());
			}

			double defense = getAttributeValue(SimpleRpgAttributes.INSTANCE.getDEFENSE());
			if(source.isMagic()) {
				double intelligence = attacker.getAttributeValue(SimpleRpgAttributes.INSTANCE.getINTELLIGENCE());
				return damage * (float) (intelligence / defense);
			} else if(source.isProjectile()) {
				double dexterity = attacker.getAttributeValue(SimpleRpgAttributes.INSTANCE.getDEXTERITY());
				return damage * (float) (dexterity / defense);
			} else {
				double strength = attacker.getAttributeValue(SimpleRpgAttributes.INSTANCE.getSTRENGTH());
				return damage * (float) (strength / defense);
			}
		}

		float healthScaling =
				(float) Math.max((1 + (getMaxHealth() - getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH)) *
						((Object) this instanceof PlayerEntity ? ConfigKt.config.getPlayers().getDamageScalingRatio()
								: ConfigKt.config.getEntities().getDamageScalingRatio()) / getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH)), 1.0);
		return amount * healthScaling;
	}

	@ModifyVariable(method = "heal(F)V", at = @At("HEAD"), argsOnly = true)
	private float heal(float amount) {
		float healthScaling =
				(float) Math.max((1 + (getMaxHealth() - getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH)) *
						((Object) this instanceof PlayerEntity ? ConfigKt.config.getPlayers().getHealScalingRatio()
								: ConfigKt.config.getEntities().getHealScalingRatio()) / getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH)), 1.0);
		return amount * healthScaling;
	}

	@Redirect(method = "createLivingAttributes()Lnet/minecraft/entity/attribute/DefaultAttributeContainer$Builder;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/attribute/DefaultAttributeContainer;builder()Lnet/minecraft/entity/attribute/DefaultAttributeContainer$Builder;"))
	private static DefaultAttributeContainer.Builder createLivingAttributes() {
		return new DefaultAttributeContainer.Builder()
				.add(SimpleRpgAttributes.INSTANCE.getSTRENGTH())
				.add(SimpleRpgAttributes.INSTANCE.getDEXTERITY())
				.add(SimpleRpgAttributes.INSTANCE.getINTELLIGENCE())
				.add(SimpleRpgAttributes.INSTANCE.getDEFENSE());
	}

	@Shadow
	public float getMaxHealth() {
		return 0f;
	}

	@Shadow
	public double getAttributeValue(EntityAttribute attribute) {
		return 0.0;
	}

	@Shadow
	public double getAttributeBaseValue(EntityAttribute attribute) {
		return 0.0;
	}

}
