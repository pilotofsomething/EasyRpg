package pilotofsomething.easyrpg.mixins;

import net.minecraft.client.particle.DamageParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.particle.NoShowParticle;

@Mixin(DamageParticle.DefaultFactory.class)
public class DamageParticleFactoryMixin {

	@Inject(method = "createParticle(Lnet/minecraft/particle/DefaultParticleType;Lnet/minecraft/client/world/ClientWorld;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
	private void createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, CallbackInfoReturnable<Particle> cir) {
		if(!ConfigKt.config.getClient().getRenderVanillaDamageParticle()) {
			cir.setReturnValue(new NoShowParticle(clientWorld, d, e, f, g, h, i));
		}
	}

}
