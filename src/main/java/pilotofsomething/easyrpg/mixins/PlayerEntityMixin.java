package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.components.IRpgPlayer;
import pilotofsomething.easyrpg.components.RpgPlayerKt;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin extends LivingEntityMixin {

	@ModifyVariable(method = "damageShield(F)V", at = @At("HEAD"), argsOnly = true)
	private float damageShield(float amount) {
		return amount * 20f / getMaxHealth();
	}

	@ModifyVariable(method = "damageArmor(Lnet/minecraft/entity/damage/DamageSource;F)V", at = @At("HEAD"), argsOnly = true)
	private float damageArmor(float val, DamageSource source, float amount) {
		return amount * 20f / getMaxHealth();
	}

	@ModifyVariable(method = "damageHelmet(Lnet/minecraft/entity/damage/DamageSource;F)V", at = @At("HEAD"), argsOnly = true)
	private float damageHelmet(float val, DamageSource source, float amount) {
		return amount * 20f / getMaxHealth();
	}

	@Inject(method = "addExperience(I)V", at = @At("HEAD"))
	private void addXp(int experience, CallbackInfo ci) {
		if(ConfigKt.getConfig().getPlayers().getExperience().getUseVanillaExp()) {
			IRpgPlayer rpg = RpgPlayerKt.getRPG_PLAYER().get(this);
			rpg.addXP(experience);
		}
	}

}
