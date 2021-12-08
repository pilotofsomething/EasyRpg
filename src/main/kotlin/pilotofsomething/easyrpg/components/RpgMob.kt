package pilotofsomething.easyrpg.components

import dev.onyxstudios.cca.api.v3.component.Component
import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtList
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import pilotofsomething.easyrpg.ScaleSettingOperation
import pilotofsomething.easyrpg.ScalingMode
import pilotofsomething.easyrpg.EasyRpgAttributes
import pilotofsomething.easyrpg.config
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

val RPG_MOB: ComponentKey<IRpgMob> =
	ComponentRegistry.getOrCreate(Identifier("easy_rpg", "entity"), IRpgMob::class.java)

interface IRpgMob : IRpgEntity, Component, ServerTickingComponent, AutoSyncedComponent

class RpgMob(private val entity: LivingEntity) : IRpgMob {
	override var level = -1
	private val spDist = DoubleArray(5) { 0.0 }

	override var absorptionAmount = 0

	var lastSync = 0L

	override fun readFromNbt(tag: NbtCompound) {
		level = tag.getInt("Level")
		val sp = tag.get("SP") as NbtList
		for(i in sp.indices) {
			val dv = sp[i] as NbtDouble
			spDist[i] = dv.doubleValue()
		}
		absorptionAmount = tag.getInt("AbsorptionValue")
	}

	override fun writeToNbt(tag: NbtCompound) {
		tag.putInt("Level", level)
		val sp = NbtList()
		for(d in spDist) {
			sp.add(NbtDouble.of(d))
		}
		tag.put("SP", sp)
		tag.putInt("AbsorptionValue", absorptionAmount)
	}

	private fun getWeightedTime(players: List<PlayerEntity>): Long {
		return if(players.size == 1) RPG_PLAYER.get(players[0]).timer else {
			val distances = players.map { player -> Pair(RPG_PLAYER.get(player), player.distanceTo(entity).toDouble()) }
			val totalDistance = distances.sumOf { d -> d.second }
			distances.sumOf { (player, dist) -> (1 - dist / totalDistance) * player.timer }.toLong()
		}
	}

	private fun getEntityLevel(): Int {
		val scaleMode = config.entities.scaleMode

		val dist = if(scaleMode.distance) {
			1 + sqrt(entity.squaredDistanceTo(0.0, entity.y, 0.0)) / config.entities.scaleModeSettings.distanceDivisor
		} else 0.0

		val players = entity.world.players.filter { player -> player.distanceTo(entity) < 128f }
		val wTime = if(players.isEmpty() || !scaleMode.time) 0L else getWeightedTime(players)
		val time = if(scaleMode.time) {
			if(config.entities.scaleModeSettings.timeSettings.multiplier != -1L) {
				1 + wTime / config.entities.scaleModeSettings.timeSettings.multiplier.toDouble()
			} else 1.0
		} else 1.0
		val timeLinear = if(scaleMode.time) {
			if(config.entities.scaleModeSettings.timeSettings.multiplier != -1L) {
				wTime / config.entities.scaleModeSettings.timeSettings.linear.toDouble()
			} else 0.0
		} else 0.0

		val level = when {
			players.isEmpty() -> 0.0
			players.size == 1 -> {
				RPG_PLAYER.get(players[0]).level.toDouble()
			}
			else -> {
				val distances =
					players.map { player -> Pair(RPG_PLAYER.get(player), player.distanceTo(entity).toDouble()) }
				val totalDistance = distances.sumOf { d -> d.second }
				distances.sumOf { d -> (1 - d.second / totalDistance) * d.first.level }
			}
		}
		val levelRatio =
			if(scaleMode.level && (scaleMode.time || scaleMode.distance)) config.entities.scaleModeSettings.levelRatio else 1.0

		val dimensionId = entity.world.registryKey.value.toString()
		val rules = config.entities.scaleModeSettings.dimensionSettings[dimensionId]

		val base = rules?.sumOf { if(it.operation == ScaleSettingOperation.Operation.ADD) it.value else 0.0 } ?: 0.0
		val distMult =
			rules?.sumOf { if(it.operation == ScaleSettingOperation.Operation.MULTIPLY_DISTANCE) it.value else 1.0 }
				?: 1.0
		val totalMult =
			rules?.map { if(it.operation == ScaleSettingOperation.Operation.MULTIPLY_TOTAL) it.value else 1.0 }
				?.reduceOrNull { acc, v -> acc * v } ?: 1.0
		val minimum = rules?.maxOfOrNull {
			if(it.operation == ScaleSettingOperation.Operation.MINIMUM) max(
				it.value, 1.0
			) else Double.NEGATIVE_INFINITY
		} ?: 1.0
		val maximum = rules?.minOfOrNull {
			if(it.operation == ScaleSettingOperation.Operation.MAXIMUM) min(
				it.value, config.entities.maxLevel.toDouble()
			) else Double.POSITIVE_INFINITY
		} ?: config.entities.maxLevel.toDouble()

		return when {
			scaleMode.distance && !scaleMode.level && !scaleMode.time -> max(
				min(Random.nextDouble(0.9, 1.1) * (base + (dist * distMult * totalMult)), maximum), minimum
			).toInt()
			players.isEmpty() -> -1
			scaleMode == ScalingMode.LEVEL -> max(min(Random.nextDouble(0.9, 1.1) * level, maximum), minimum).toInt()
			else -> max(
				min(Random.nextDouble(0.9, 1.1) * (base + level * levelRatio + (timeLinear + (dist * distMult)) * time * totalMult), maximum), minimum
			).toInt()
		}
	}

	override fun serverTick() {
		if(level == -1) {
			level = getEntityLevel()
			if(level == -1) return
			level = min(max(level, 1), config.entities.maxLevel)
			val spDist = DoubleArray(5) { Random.Default.nextDouble() }
			val total = spDist.sum()
			val spDistNorm = spDist.map { s -> s / total }
			spDistNorm.forEachIndexed { i, d -> this.spDist[i] = d }

			val maxHealthInst = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
			val toughnessInst = entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)
			val strInst = entity.getAttributeInstance(EasyRpgAttributes.STRENGTH)
			val dexInst = entity.getAttributeInstance(EasyRpgAttributes.DEXTERITY)
			val intInst = entity.getAttributeInstance(EasyRpgAttributes.INTELLIGENCE)
			val defInst = entity.getAttributeInstance(EasyRpgAttributes.DEFENSE)

			strInst!!.baseValue = getStr(true).toDouble()
			dexInst!!.baseValue = getDex(true).toDouble()
			intInst!!.baseValue = getInt(true).toDouble()
			defInst!!.baseValue = getDef(true).toDouble()

			val healthMult = maxHealthInst!!.baseValue / 20.0

			checkAttributeModifiers(maxHealthInst, IRpgEntity.HEALTH_MODIFIER, "Easy RPG Health", getHealth(healthMult))
			checkAttributeModifiers(
				toughnessInst!!, IRpgEntity.BASE_TOUGHNESS, "Easy RPG Base Toughness", config.entities.toughness.base
			)
			checkAttributeModifiersMultiplier(
				toughnessInst, IRpgEntity.TOUGHNESS_MODIFIER, "Easy RPG Toughness", config.entities.toughness.gain * (level - 1)
			)
			checkAttributeModifiers(strInst, IRpgEntity.STRENGTH_MODIFIER, "Easy RPG Strength", getStr().toDouble())
			checkAttributeModifiers(dexInst, IRpgEntity.DEXTERITY_MODIFIER, "Easy RPG Dexterity", getDex().toDouble())
			checkAttributeModifiers(
				intInst, IRpgEntity.INTELLIGENCE_MODIFIER, "easy RPG Intelligence", getInt().toDouble()
			)
			checkAttributeModifiers(defInst, IRpgEntity.DEFENSE_MODIFIER, "Easy RPG Defense", getDef().toDouble())
			entity.health = entity.maxHealth
			RPG_MOB.sync(entity)
		}

		val oldHealth = entity.maxHealth

		val maxHealthInst = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
		val toughnessInst = entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)
		val strInst = entity.getAttributeInstance(EasyRpgAttributes.STRENGTH)
		val dexInst = entity.getAttributeInstance(EasyRpgAttributes.DEXTERITY)
		val intInst = entity.getAttributeInstance(EasyRpgAttributes.INTELLIGENCE)
		val defInst = entity.getAttributeInstance(EasyRpgAttributes.DEFENSE)

		strInst!!.baseValue = getStr(true).toDouble()
		dexInst!!.baseValue = getDex(true).toDouble()
		intInst!!.baseValue = getInt(true).toDouble()
		defInst!!.baseValue = getDef(true).toDouble()

		val healthMult = maxHealthInst!!.baseValue / 20.0

		checkAttributeModifiers(maxHealthInst, IRpgEntity.HEALTH_MODIFIER, "Easy RPG Health", getHealth(healthMult))
		checkAttributeModifiers(
			toughnessInst!!, IRpgEntity.BASE_TOUGHNESS, "Easy RPG Base Toughness", config.entities.toughness.base
		)
		checkAttributeModifiersMultiplier(
			toughnessInst, IRpgEntity.TOUGHNESS_MODIFIER, "Easy RPG Toughness", config.entities.toughness.gain * (level - 1)
		)
		checkAttributeModifiers(strInst, IRpgEntity.STRENGTH_MODIFIER, "Easy RPG Strength", getStr().toDouble())
		checkAttributeModifiers(dexInst, IRpgEntity.DEXTERITY_MODIFIER, "Easy RPG Dexterity", getDex().toDouble())
		checkAttributeModifiers(intInst, IRpgEntity.INTELLIGENCE_MODIFIER, "easy RPG Intelligence", getInt().toDouble())
		checkAttributeModifiers(defInst, IRpgEntity.DEFENSE_MODIFIER, "Easy RPG Defense", getDef().toDouble())

		entity.health += entity.maxHealth - oldHealth

		// Sync every 500ms for players the get near the mob after it spawns
		if(Util.getMeasuringTimeMs() - lastSync > 500) {
			RPG_MOB.sync(entity)
			lastSync = Util.getMeasuringTimeMs()
		}
	}

	private fun checkAttributeModifiers(instance: EntityAttributeInstance, id: UUID, name: String, value: Double) {
		val modifier = instance.getModifier(id)
		if(modifier != null) {
			if(modifier.value != value - instance.baseValue) {
				instance.removeModifier(id)
				if(value - instance.baseValue > 0) {
					instance.addPersistentModifier(
						EntityAttributeModifier(
							id, name, value - instance.baseValue, EntityAttributeModifier.Operation.ADDITION
						)
					)
				}
			}
		} else {
			if(value - instance.baseValue > 0) {
				instance.addPersistentModifier(
					EntityAttributeModifier(
						id, name, value - instance.baseValue, EntityAttributeModifier.Operation.ADDITION
					)
				)
			}
		}
	}

	private fun checkAttributeModifiersMultiplier(
		instance: EntityAttributeInstance,
		id: UUID,
		name: String,
		value: Double
	) {
		val modifier = instance.getModifier(id)
		if(modifier != null) {
			if(modifier.value != value) {
				instance.removeModifier(id)
				if(value > 0) {
					instance.addPersistentModifier(
						EntityAttributeModifier(id, name, value, EntityAttributeModifier.Operation.MULTIPLY_TOTAL)
					)
				}
			}
		} else {
			if(value > 0) {
				instance.addPersistentModifier(
					EntityAttributeModifier(id, name, value, EntityAttributeModifier.Operation.MULTIPLY_TOTAL)
				)
			}
		}
	}

	private fun getHealth(mult: Double, base: Boolean = false): Double {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.healthOptions.base +
					config.entities.healthOptions.gain * (level - 1) + if(!base)
				(config.entities.healthOptions.spGain * sp * this.spDist[0]).toInt() else 0) * mult
	}

	private fun getStr(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.strengthOptions.base +
					config.entities.strengthOptions.gain * (level - 1)).toInt() + if(!base)
				(config.entities.strengthOptions.spGain * sp * this.spDist[1]).toInt() else 0
	}

	private fun getDex(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.dexterityOptions.base +
					config.entities.dexterityOptions.gain * (level - 1)).toInt() + if(!base)
				(config.entities.dexterityOptions.spGain * sp * this.spDist[2]).toInt() else 0
	}

	private fun getInt(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.intelligenceOptions.base +
					config.entities.intelligenceOptions.gain * (level - 1)).toInt() + if(!base)
				(config.entities.intelligenceOptions.spGain * sp * this.spDist[3]).toInt() else 0
	}

	private fun getDef(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.defenseOptions.base +
					config.entities.defenseOptions.gain * (level - 1)).toInt() + if(!base)
				(config.entities.defenseOptions.spGain * sp * this.spDist[4]).toInt() else 0
	}

	override fun writeSyncPacket(buf: PacketByteBuf, recipient: ServerPlayerEntity) {
		buf.writeVarInt(level)
	}

	override fun applySyncPacket(buf: PacketByteBuf) {
		level = buf.readVarInt()
	}

	override fun shouldSyncWith(player: ServerPlayerEntity): Boolean {
		return player.squaredDistanceTo(entity) < 1024
	}

	companion object {
		@JvmStatic
		fun registerEntityComponentFactories(registry: EntityComponentFactoryRegistry) {
			registry.registerFor(
				{ c -> LivingEntity::class.java.isAssignableFrom(c) && !PlayerEntity::class.java.isAssignableFrom(c) },
				RPG_MOB
			) { entity -> RpgMob(entity as LivingEntity) }
		}
	}

}