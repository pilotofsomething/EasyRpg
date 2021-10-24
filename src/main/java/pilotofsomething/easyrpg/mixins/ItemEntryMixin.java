package pilotofsomething.easyrpg.mixins;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.entry.ItemEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.item.LootItemKt;

import java.util.function.Consumer;

@Mixin(ItemEntry.class)
public class ItemEntryMixin {

	@Shadow @Final private Item item;

	@Inject(method = "generateLoot(Ljava/util/function/Consumer;Lnet/minecraft/loot/context/LootContext;)V", at = @At("HEAD"), cancellable = true)
	private void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context, CallbackInfo ci) {
		if(ConfigKt.getConfig().getItems().getEnabled()) {
			ItemStack stack = new ItemStack(item);
			LootItemKt.addLootModifiers(context.get(LootContextParameters.THIS_ENTITY), context.get(LootContextParameters.ORIGIN),
					stack, context.getLuck(), false);
			lootConsumer.accept(stack);
			ci.cancel();
		}
	}
}
