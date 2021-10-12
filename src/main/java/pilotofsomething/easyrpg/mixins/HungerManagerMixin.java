package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(HungerManager.class)
public class HungerManagerMixin {

	@ModifyConstant(method = "update(Lnet/minecraft/entity/player/PlayerEntity;)V", constant = @Constant(floatValue = 10.0f))
	private float updateStarveEasy(float v, PlayerEntity player) {
		return Math.max(player.getMaxHealth() * 0.5f, 10.0f);
	}

	@ModifyConstant(method = "update(Lnet/minecraft/entity/player/PlayerEntity;)V", constant = @Constant(floatValue = 1.0f), slice = @Slice(
			from = @At(value = "FIELD", target = "Lnet/minecraft/world/Difficulty;HARD:Lnet/minecraft/world/Difficulty;", opcode = Opcodes.GETSTATIC),
			to = @At(value = "FIELD", target = "Lnet/minecraft/world/Difficulty;NORMAL:Lnet/minecraft/world/Difficulty;", opcode = Opcodes.GETSTATIC)))
	private float updateStarveNormal(float v, PlayerEntity player) {
		return (float) Math.max(Math.floor(player.getMaxHealth() * 0.075), 1.0);
	}

}
