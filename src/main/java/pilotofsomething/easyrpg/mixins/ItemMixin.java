package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.item.LootItemKt;

@Mixin(Item.class)
public class ItemMixin {

	@Inject(method = "onCraft(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
	private void onCraft(ItemStack stack, World world, PlayerEntity player, CallbackInfo ci) {
		if(ConfigKt.getConfig().getItems().getCrafted()) {
			LootItemKt.addLootModifiers(player, player.getPos(), stack, player.getLuck(), true);
		}
	}
}
