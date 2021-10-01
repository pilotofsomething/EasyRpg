package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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

}
