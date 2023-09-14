package pilotofsomething.easyrpg.components

import com.ezylang.evalex.Expression
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
import pilotofsomething.easyrpg.EVALEX_CONFIG
import pilotofsomething.easyrpg.EasyRpgAttributes
import pilotofsomething.easyrpg.LevelFormula
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
		val players = entity.world.players.filter { player -> player.squaredDistanceTo(entity) < 16384f }

		if(players.isEmpty()) return -1

		val dist = 1 + sqrt(entity.squaredDistanceTo(0.0, entity.y, 0.0))
		val wTime = if(players.isEmpty()) 0L else getWeightedTime(players)

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

		val dimensionId = entity.world.registryKey.value.toString()
		val rules = config.entities.levelFormula[dimensionId] ?: config.entities.levelFormula["default"] ?: arrayListOf(LevelFormula("1"))

		for (rule in rules) {
			if (entity.y.toInt() in rule.minY until rule.maxY) {
				return Expression(rule.formula, EVALEX_CONFIG).with("distance", dist).and("time", wTime).and("level", level).evaluate().numberValue.toInt()
			}
		}

		return 1
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
			checkAttributeModifiersMultiplier(maxHealthInst, IRpgEntity.HEALTH_MULTIPLIER, "Easy RPG Health Mult", getHealthMult())
			checkAttributeModifiers(toughnessInst!!, IRpgEntity.BASE_TOUGHNESS, "Easy RPG Base Toughness", 1.0)
			checkAttributeModifiersMultiplier(
				toughnessInst, IRpgEntity.TOUGHNESS_MODIFIER, "Easy RPG Toughness",
				(config.entities.toughness.gain * (level - 1) + (config.entities.toughness.base - 1)) * (1 + config.entities.toughness.multGain * (level - 1))
			)
			checkAttributeModifiers(strInst, IRpgEntity.STRENGTH_MODIFIER, "Easy RPG Strength", getStr().toDouble())
			checkAttributeModifiersMultiplier(strInst, IRpgEntity.STRENGTH_MULTIPLIER, "Easy RPG Strength Mult", getStrMult())
			checkAttributeModifiers(dexInst, IRpgEntity.DEXTERITY_MODIFIER, "Easy RPG Dexterity", getDex().toDouble())
			checkAttributeModifiersMultiplier(dexInst, IRpgEntity.DEXTERITY_MULTIPLIER, "Easy RPG Dexterity Mult", getDexMult())
			checkAttributeModifiers(intInst, IRpgEntity.INTELLIGENCE_MODIFIER, "easy RPG Intelligence", getInt().toDouble())
			checkAttributeModifiersMultiplier(intInst, IRpgEntity.INTELLIGENCE_MULTIPLIER, "Easy RPG Intelligence Mult", getIntMult())
			checkAttributeModifiers(defInst, IRpgEntity.DEFENSE_MODIFIER, "Easy RPG Defense", getDef().toDouble())
			checkAttributeModifiersMultiplier(intInst, IRpgEntity.DEFENSE_MULTIPLIER, "Easy RPG Defense Mult", getDefMult())

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
		checkAttributeModifiersMultiplier(maxHealthInst, IRpgEntity.HEALTH_MULTIPLIER, "Easy RPG Health Mult", getHealthMult())
		checkAttributeModifiers(toughnessInst!!, IRpgEntity.BASE_TOUGHNESS, "Easy RPG Base Toughness", 1.0)
		checkAttributeModifiersMultiplier(
			toughnessInst, IRpgEntity.TOUGHNESS_MODIFIER, "Easy RPG Toughness",
			(config.entities.toughness.gain * (level - 1) + (config.entities.toughness.base - 1)) * (1 + config.entities.toughness.multGain * (level - 1))
		)
		checkAttributeModifiers(strInst, IRpgEntity.STRENGTH_MODIFIER, "Easy RPG Strength", getStr().toDouble())
		checkAttributeModifiersMultiplier(strInst, IRpgEntity.STRENGTH_MULTIPLIER, "Easy RPG Strength Mult", getStrMult())
		checkAttributeModifiers(dexInst, IRpgEntity.DEXTERITY_MODIFIER, "Easy RPG Dexterity", getDex().toDouble())
		checkAttributeModifiersMultiplier(dexInst, IRpgEntity.DEXTERITY_MULTIPLIER, "Easy RPG Dexterity Mult", getDexMult())
		checkAttributeModifiers(intInst, IRpgEntity.INTELLIGENCE_MODIFIER, "easy RPG Intelligence", getInt().toDouble())
		checkAttributeModifiersMultiplier(intInst, IRpgEntity.INTELLIGENCE_MULTIPLIER, "Easy RPG Intelligence Mult", getIntMult())
		checkAttributeModifiers(defInst, IRpgEntity.DEFENSE_MODIFIER, "Easy RPG Defense", getDef().toDouble())
		checkAttributeModifiersMultiplier(intInst, IRpgEntity.DEFENSE_MULTIPLIER, "Easy RPG Defense Mult", getDefMult())

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
		return (config.entities.health.base +
					config.entities.health.gain * (level - 1) + if(!base)
				(config.entities.health.spGain * sp * this.spDist[0]).toInt() else 0) * mult
	}

	private fun getHealthMult(): Double {
		val sp = (level * config.entities.spGain).toInt()
		return config.entities.health.multGain * (level - 1) + config.entities.health.multiSpGain * sp * spDist[0]
	}

	private fun getStr(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.strength.base +
					config.entities.strength.gain * (level - 1)).toInt() + if(!base)
				(config.entities.strength.spGain * sp * this.spDist[1]).toInt() else 0
	}

	private fun getStrMult(): Double {
		val sp = (level * config.entities.spGain).toInt()
		return config.entities.strength.multGain * (level - 1) + config.entities.strength.multiSpGain * sp * spDist[1]
	}

	private fun getDex(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.dexterity.base +
					config.entities.dexterity.gain * (level - 1)).toInt() + if(!base)
				(config.entities.dexterity.spGain * sp * this.spDist[2]).toInt() else 0
	}

	private fun getDexMult(): Double {
		val sp = (level * config.entities.spGain).toInt()
		return config.entities.dexterity.multGain * (level - 1) + config.entities.dexterity.multiSpGain * sp * spDist[2]
	}

	private fun getInt(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.intelligence.base +
					config.entities.intelligence.gain * (level - 1)).toInt() + if(!base)
				(config.entities.intelligence.spGain * sp * this.spDist[3]).toInt() else 0
	}

	private fun getIntMult(): Double {
		val sp = (level * config.entities.spGain).toInt()
		return config.entities.intelligence.multGain * (level - 1) + config.entities.intelligence.multiSpGain * sp * spDist[3]
	}

	private fun getDef(base: Boolean = false): Int {
		val sp = (level * config.entities.spGain).toInt()
		return (config.entities.defense.base +
					config.entities.defense.gain * (level - 1)).toInt() + if(!base)
				(config.entities.defense.spGain * sp * this.spDist[4]).toInt() else 0
	}

	private fun getDefMult(): Double {
		val sp = (level * config.entities.spGain).toInt()
		return config.entities.defense.multGain * (level - 1) + config.entities.defense.multiSpGain * sp * spDist[4]
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