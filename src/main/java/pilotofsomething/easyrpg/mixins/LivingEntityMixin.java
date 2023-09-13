package pilotofsomething.easyrpg.mixins;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pilotofsomething.easyrpg.EasyRpgAttributes;
import pilotofsomething.easyrpg.EasyRpgKt;
import pilotofsomething.easyrpg.components.IRpgEntity;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.components.RpgMobKt;
import pilotofsomething.easyrpg.components.RpgPlayerKt;

import java.util.Map;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
	private float damage(float val, DamageSource source, float amount) {
		if(source.getAttacker() instanceof LivingEntity attacker) {
			IRpgEntity rpg;
			if(attacker instanceof PlayerEntity) {
				rpg = RpgPlayerKt.getRPG_PLAYER().get(attacker);
			} else rpg = RpgMobKt.getRPG_MOB().get(attacker);
			IRpgEntity thisRpg;
			if((Object)this instanceof PlayerEntity) {
				thisRpg = RpgPlayerKt.getRPG_PLAYER().get(this);
			} else thisRpg = RpgMobKt.getRPG_MOB().get(this);

			float damage = amount;

			if(attacker instanceof PlayerEntity) {
				damage *= ConfigKt.getConfig().getPlayers().getDamage().getBase()
								+ ConfigKt.config.getPlayers().getDamage().getGain() * (rpg.getLevel() - 1);
				damage *= 1 + ConfigKt.getConfig().getPlayers().getDamage().getMultGain() * (rpg.getLevel() - 1);
				damage *= Math.max(
						Math.pow(ConfigKt.getConfig().getPlayers().getDamageScaling().getAmount(), Math.max(thisRpg.getLevel() - rpg.getLevel(), 0)),
						ConfigKt.getConfig().getPlayers().getDamageScaling().getLimit());
			} else {
				damage *= ConfigKt.config.getEntities().getDamage().getBase()
								+ ConfigKt.config.getEntities().getDamage().getGain() * (rpg.getLevel() - 1);
				damage *= 1 + ConfigKt.getConfig().getEntities().getDamage().getMultGain() * (rpg.getLevel() - 1);
				damage *= Math.max(
						Math.pow(ConfigKt.getConfig().getEntities().getDamageScaling().getAmount(), Math.max(thisRpg.getLevel() - rpg.getLevel(), 0)),
						ConfigKt.getConfig().getEntities().getDamageScaling().getLimit());
			}
			Map<String, Double> powScaling = EasyRpgKt.getDamagePowScaling(source.getTypeRegistryEntry().getKey().orElseThrow().getValue().toString());

			double strength = attacker.getAttributeValue(EasyRpgAttributes.INSTANCE.getSTRENGTH());
			double dexterity = attacker.getAttributeValue(EasyRpgAttributes.INSTANCE.getDEXTERITY());
			double intelligence = attacker.getAttributeValue(EasyRpgAttributes.INSTANCE.getINTELLIGENCE());
			double defense = getAttributeValue(EasyRpgAttributes.INSTANCE.getDEFENSE());

			double power = Math.max(strength * powScaling.get("strength")
					+ dexterity * powScaling.get("dexterity")
					+ intelligence * powScaling.get("intelligence"), 1);
			double def = Math.max(defense * powScaling.get("defense"), 1);

			try {
				return Math.min(new Expression(ConfigKt.getConfig().getDamageFormula()).with("attack", damage).and("power", power).and("defense", def).evaluate().getNumberValue().floatValue(), ConfigKt.getConfig().getStatCaps().getDamageCap());
			} catch (EvaluationException | ParseException e) {
				throw new RuntimeException(e);
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

	@Shadow
	public double getAttributeValue(EntityAttribute attribute) {
		return 0.0;
	}

	@Shadow
	public double getAttributeBaseValue(EntityAttribute attribute) {
		return 0.0;
	}

	@Shadow
	protected int getXpToDrop() { return 0; }
}
