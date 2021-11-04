package pilotofsomething.easyrpg.mixins;

import dev.emi.trinkets.api.Trinket;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Wearable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pilotofsomething.easyrpg.ConfigKt;
import pilotofsomething.easyrpg.EasyRpgKt;
import pilotofsomething.easyrpg.ModConfig;
import pilotofsomething.easyrpg.item.LootItemKt;

import java.util.Arrays;
import java.util.Map;

@Mixin(SmithingScreenHandler.class)
public class SmithingScreenHandlerMixin extends ForgingScreenHandler {
	public SmithingScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(type, syncId, playerInventory, context);
	}

	@Inject(method = "updateResult()V", at = @At("TAIL"))
	private void updateResult(CallbackInfo ci) {
		boolean levelItem = ConfigKt.getConfig().getItems().getEnabled();
		if((levelItem && (input.getStack(0).getItem() instanceof Wearable || EasyRpgKt.isItemTrinket(input.getStack(0))))) {
			Map<String, ModConfig.ItemOptions.RerollSettings> settings = ConfigKt.getConfig().getItems().getRerollers();
			if(settings.containsKey(Registry.ITEM.getId(input.getStack(1).getItem()).toString())) {
				ItemStack stack = input.getStack(0).copy();
				stack.getOrCreateNbt().remove("ItemBonuses");
				stack.getNbt().remove("EasyRpgLoot");
				NbtCompound display;
				if(stack.getNbt().contains("display")) {
					display = stack.getSubNbt("display");
					if(display.contains("Lore")) {
						NbtList list = display.getList("Lore", NbtElement.STRING_TYPE);
						list.removeIf(element -> {
							Text text = Text.Serializer.fromJson(element.asString());
							if(text instanceof TranslatableText) {
								if(Arrays.stream(LootItemKt.getQualityNames()).toList().contains(((TranslatableText) text).getKey())) {
									return true;
								}
								return ((TranslatableText) text).getKey().equals("easyrpg.items.tooltip.level");
							}
							return false;
						});
						display.put("Lore", list);
					}
					stack.getNbt().put("display", display);
				}
				stack.getNbt().putString("Reroll", Registry.ITEM.getId(input.getStack(1).getItem()).toString());
				this.output.setStack(0, stack);
			}
		}
	}

	@Inject(method = "canTakeOutput(Lnet/minecraft/entity/player/PlayerEntity;Z)Z", at = @At("HEAD"), cancellable = true)
	private void canTakeOutput(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
		boolean levelItem = ConfigKt.getConfig().getItems().getEnabled();
		if(levelItem && (input.getStack(0).getItem() instanceof Wearable || EasyRpgKt.isItemTrinket(input.getStack(0)))) {
			Map<String, ModConfig.ItemOptions.RerollSettings> settings = ConfigKt.getConfig().getItems().getRerollers();
			if(settings.containsKey(Registry.ITEM.getId(input.getStack(1).getItem()).toString())) {
				cir.setReturnValue(true);
			}
		}
	}

	@Shadow
	protected boolean canTakeOutput(PlayerEntity player, boolean present) {
		return false;
	}

	@Shadow
	protected void onTakeOutput(PlayerEntity player, ItemStack stack) {
	}

	@Shadow
	protected boolean canUse(BlockState state) {
		return false;
	}

	@Shadow
	public void updateResult() {
	}
}
