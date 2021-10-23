package pilotofsomething.easyrpg.mixins;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow public abstract boolean hasNbt();

	@Shadow private NbtCompound nbt;

	@Inject(method = "getAttributeModifiers(Lnet/minecraft/entity/EquipmentSlot;)Lcom/google/common/collect/Multimap;", at = @At("RETURN"), cancellable = true)
	public void getAttributeModifiers(EquipmentSlot slot, CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir) {
		Multimap<EntityAttribute, EntityAttributeModifier> modifiers = HashMultimap.create();
		if (this.hasNbt() && this.nbt.contains("ItemBonuses", 9)) {
			NbtList nbtList = this.nbt.getList("ItemBonuses", 10);
			for(int i = 0; i < nbtList.size(); ++i) {
				NbtCompound nbtCompound = nbtList.getCompound(i);
				if (!nbtCompound.contains("Slot", 8) || nbtCompound.getString("Slot").equals(slot.getName())) {
					Optional<EntityAttribute> optional = Registry.ATTRIBUTE.getOrEmpty(Identifier.tryParse(nbtCompound.getString("AttributeName")));
					if (optional.isPresent()) {
						EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromNbt(nbtCompound);
						if (entityAttributeModifier != null && entityAttributeModifier.getId().getLeastSignificantBits() != 0L && entityAttributeModifier.getId().getMostSignificantBits() != 0L) {
							modifiers.put(optional.get(), entityAttributeModifier);
						}
					}
				}
			}
		}
		modifiers.putAll(cir.getReturnValue());
		cir.setReturnValue(modifiers);
	}
}
