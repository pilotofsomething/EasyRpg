package pilotofsomething.easyrpg.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.ModConfig;
import pilotofsomething.easyrpg.item.LootItemKt;

import java.util.Map;

@Mixin(Item.class)
public class ItemMixin {

	@Inject(method = "onCraft(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
	private void onCraft(ItemStack stack, World world, PlayerEntity player, CallbackInfo ci) {
		if(ConfigKt.getConfig().getItems().getEnabled()) {
			NbtCompound nbt = stack.getNbt();
			if (ConfigKt.getConfig().getItems().getCrafted()) {
				if (nbt == null || !nbt.contains("Reroll")) {
					LootItemKt.addLootModifiers(player, player.getPos(), stack, player.getLuck(), ConfigKt.getConfig().getItems().getCraftedLevelMult());
					return;
				}
			}
			String rerollItem = nbt.getString("Reroll");
			Map<String, ModConfig.ItemOptions.RerollSettings> settings = ConfigKt.getConfig().getItems().getRerollers();
			if (!settings.containsKey(rerollItem)) {
				LootItemKt.addLootModifiers(player, player.getPos(), stack, player.getLuck(), ConfigKt.getConfig().getItems().getCraftedLevelMult());
			} else {
				ModConfig.ItemOptions.RerollSettings rerollSettings = settings.get(rerollItem);
				LootItemKt.addLootModifiers(player, player.getPos(), stack, player.getLuck() + rerollSettings.getLuck(),
						rerollSettings.getLevelMult());
			}
		}
	}
}
