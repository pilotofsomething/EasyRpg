package pilotofsomething.easyrpg.item

import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

object EasyRpgItems {
	val IRON_REROLLER = RerollItem(FabricItemSettings().group(ItemGroup.MISC).rarity(Rarity.COMMON))
	val GOLD_REROLLER = RerollItem(FabricItemSettings().group(ItemGroup.MISC).rarity(Rarity.UNCOMMON))
	val DIAMOND_REROLLER = RerollItem(FabricItemSettings().group(ItemGroup.MISC).rarity(Rarity.RARE))
	val NETHERITE_REROLLER = RerollItem(FabricItemSettings().group(ItemGroup.MISC).rarity(Rarity.EPIC))

	fun registerItems() {
		Registry.register(Registry.ITEM, Identifier("easy_rpg", "iron_reroller"), IRON_REROLLER)
		Registry.register(Registry.ITEM, Identifier("easy_rpg", "gold_reroller"), GOLD_REROLLER)
		Registry.register(Registry.ITEM, Identifier("easy_rpg", "diamond_reroller"), DIAMOND_REROLLER)
		Registry.register(Registry.ITEM, Identifier("easy_rpg", "netherite_reroller"), NETHERITE_REROLLER)
	}
}

class RerollItem(settings: FabricItemSettings) : Item(settings) {
	override fun appendTooltip(
		stack: ItemStack,
		world: World?,
		tooltip: MutableList<Text>,
		context: TooltipContext
	) {
		tooltip.add(TranslatableText("item.easy_rpg.reroller.tooltip.line1"))
		tooltip.add(TranslatableText("item.easy_rpg.reroller.tooltip.line2"))
	}
}