package pilotofsomething.easyrpg.item

import com.ezylang.evalex.Expression
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Wearable
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import pilotofsomething.easyrpg.components.IRpgPlayer
import pilotofsomething.easyrpg.components.RPG_MOB
import pilotofsomething.easyrpg.components.RPG_PLAYER
import pilotofsomething.easyrpg.config
import pilotofsomething.easyrpg.isItemTrinket
import kotlin.math.*
import kotlin.random.Random

private val qualities = ArrayList<Int>()

val qualityNames = arrayOf("easyrpg.qualities.common", "easyrpg.qualities.uncommon", "easyrpg.qualities.rare", "easyrpg.qualities.epic")

fun setupQualities() {
	qualities.clear()
	qualities.addAll(IntArray(config.items.rarities.epic.weight) { 4 }.asList())
	qualities.addAll(IntArray(config.items.rarities.rare.weight) { 3 }.asList())
	qualities.addAll(IntArray(config.items.rarities.uncommon.weight) { 2 }.asList())
	qualities.addAll(IntArray(config.items.rarities.common.weight) { 1 }.asList())
}

fun addLootModifiers(entity: Entity?, pos: Vec3d?, stack: ItemStack, luck: Float, craftMult: Double?) {
	if(stack.item !is Wearable && !isItemTrinket(stack)) return
	val nbt = stack.orCreateNbt
	if(nbt.contains("EasyRpgLoot")) {
		return
	}
	val level = when(entity) {
		is PlayerEntity -> if(craftMult == null) {
			getItemLevel(entity, pos ?: Vec3d.ZERO)
		} else {
			val rpg = RPG_PLAYER.get(entity)
			min(max((rpg.level * craftMult * Random.nextDouble(0.9, 1.1)).toInt(), 1), config.items.maxLevel)
		}
		is LivingEntity -> RPG_MOB.get(entity).level
		else -> 0
	}
	if(level == 0) return

	val quality = when {
		luck < 0 -> qualities[Random.nextInt(min(abs(luck.toDouble() * qualities.size * 0.02), qualities.size * 0.25).toInt(), qualities.size)]
		luck > 0 -> qualities[Random.nextInt(0, qualities.size - min(abs(luck.toDouble() * qualities.size * 0.02), qualities.size * 0.5).toInt())]
		else -> qualities.random()
	}

	val statPool = IRpgPlayer.Stats.values().toMutableList()
	val stats = ArrayList<IRpgPlayer.Stats>()
	val qMaxStats = when(quality) {
		1 -> config.items.rarities.common.maxStatCount
		2 -> config.items.rarities.uncommon.maxStatCount
		3 -> config.items.rarities.rare.maxStatCount
		4 -> config.items.rarities.epic.maxStatCount
		else -> 1
	}
	if(qMaxStats > 1) {
		val maxStats = Random.nextInt(1, qMaxStats + 1)
		while(stats.size < maxStats) {
			val stat = statPool.random()
			stats.add(stat)
			statPool.remove(stat)
		}
	} else stats.add(statPool.random())

	val bonuses = DoubleArray(stats.size) { Random.nextDouble(0.05, 1.0) }
	val total = bonuses.sum()
	val bonusesNorm = bonuses.map { it / total }

	val spMultiplier = when(quality) {
		1 -> config.items.rarities.common.spMultiplier
		2 -> config.items.rarities.uncommon.spMultiplier
		3 -> config.items.rarities.rare.spMultiplier
		4 -> config.items.rarities.epic.spMultiplier
		else -> 1.0
	}

	if(stack.item is Wearable) {
		val slot = MobEntity.getPreferredEquipmentSlot(stack)
		val list = NbtList()

		for(i in stats.indices) {
			list.add(getAttributeNBT(stats[i], (bonusesNorm[i] * level * config.items.spGain * spMultiplier).toInt(), slot))
		}

		nbt.put("ItemBonuses", list)
	}
	if(isItemTrinket(stack)) {
		val list = NbtList()

		for(i in stats.indices) {
			list.add(getAttributeNBT(stats[i], (bonusesNorm[i] * level * config.items.spGain * spMultiplier).toInt(), null))
		}

		nbt.put("TrinketAttributeModifiers", list)
	}
	putItemTooltip(stack, TranslatableText("easyrpg.items.tooltip.level", level).formatted(Formatting.WHITE))
	putItemTooltip(stack, TranslatableText(qualityNames[quality - 1]))
	nbt.putBoolean("EasyRpgLoot", true)
}

private fun getAttributeNBT(stat: IRpgPlayer.Stats, value: Int, slot: EquipmentSlot?): NbtCompound {
	val multiplier = when(stat) {
		IRpgPlayer.Stats.HEALTH -> config.items.healthGain
		IRpgPlayer.Stats.STRENGTH -> config.items.strengthGain
		IRpgPlayer.Stats.DEXTERITY -> config.items.dexterityGain
		IRpgPlayer.Stats.INTELLIGENCE -> config.items.intelligenceGain
		IRpgPlayer.Stats.DEFENSE -> config.items.defenseGain
	}

	val modifier = EntityAttributeModifier("${stat.name} item bonus", floor(value * multiplier), EntityAttributeModifier.Operation.ADDITION)
	val mNbt = modifier.toNbt()
	if(slot != null) mNbt.putString("Slot", slot.getName())
	mNbt.putString("AttributeName", Registry.ATTRIBUTE.getId(stat.attribute).toString())
	return mNbt
}

private fun putItemTooltip(stack: ItemStack, tooltip: Text) {
	val display = stack.getOrCreateSubNbt("display")
	val list = if(!display.contains("Lore", NbtElement.LIST_TYPE.toInt())) {
		NbtList()
	} else display.getList("Lore", NbtElement.STRING_TYPE.toInt())
	list.add(NbtString.of(Text.Serializer.toJson(tooltip)))
	display.put("Lore", list)
}

private fun getItemLevel(player: PlayerEntity, pos: Vec3d): Int {
	val rpg = RPG_PLAYER.get(player)

	val dist = sqrt(pos.squaredDistanceTo(0.0, pos.y, 0.0))
	val wTime = rpg.timer
	val level = rpg.level.toDouble()

	val dimensionId = player.world.registryKey.value.toString()
	val rules = config.items.levelFormula[dimensionId] ?: config.items.levelFormula["default"] ?: "1"

	return Expression(rules).with("distance", dist).and("time", wTime).and("level", level).evaluate().numberValue.toInt()
}
