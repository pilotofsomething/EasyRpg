package pilotofsomething.easyrpg.components

import com.ezylang.evalex.Expression
import dev.onyxstudios.cca.api.v3.component.Component
import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import pilotofsomething.easyrpg.EVALEX_CONFIG
import pilotofsomething.easyrpg.EasyRpgAttributes
import pilotofsomething.easyrpg.SYNC_OTHER_PLAYER
import pilotofsomething.easyrpg.config
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

val RPG_PLAYER: ComponentKey<IRpgPlayer> =
	ComponentRegistry.getOrCreate(Identifier("easy_rpg", "player"), IRpgPlayer::class.java)

interface IRpgEntity {
	var level: Int
	var absorptionAmount: Int

	companion object {
		val HEALTH_MODIFIER = UUID.fromString("3682f2f3-78f3-412a-9b1b-63804d3bafb8")!!
		val HEALTH_MULTIPLIER = UUID.fromString("c533c125-348f-4776-bf0e-e12637a05573")!!
		val STRENGTH_MODIFIER = UUID.fromString("8290e5a2-bdcb-4470-a81b-f113bead54d8")!!
		val STRENGTH_MULTIPLIER = UUID.fromString("61fe7cb3-b8c3-4f57-9462-8d30a5a97fe5")!!
		val DEXTERITY_MODIFIER = UUID.fromString("488be99a-8a8c-4a82-8540-451f73129afd")!!
		val DEXTERITY_MULTIPLIER = UUID.fromString("12c466cb-6be6-445f-b284-b005501c445b")!!
		val INTELLIGENCE_MODIFIER = UUID.fromString("455326ca-2d2d-4b38-8340-407c8edccf6d")!!
		val INTELLIGENCE_MULTIPLIER = UUID.fromString("d411a5c0-f628-4939-9d03-f1a2b479c95e")!!
		val DEFENSE_MODIFIER = UUID.fromString("96381eb5-a2e4-4ce6-9a96-7d21efdae8c7")!!
		val DEFENSE_MULTIPLIER = UUID.fromString("cc8e80c1-1933-47c5-bacc-e85c2df7de3d")!!
		val BASE_TOUGHNESS = UUID.fromString("f98871c0-a885-43ec-9a94-7da489775329")!!
		val TOUGHNESS_MODIFIER = UUID.fromString("d1eea2f1-e79a-4c20-8e29-4fb924381565")!!
	}
}

interface IRpgPlayer : IRpgEntity, Component, ServerTickingComponent, AutoSyncedComponent {
	var xp: Long
	val xpForLevel: Long
	val xpReqTotal: Long
	val xpReqForLevel: Long
	val remainingSP: Int

	var timer: Long

	fun getStat(stat: Stats): Int
	fun addPoints(stat: Stats, amt: Int)
	fun getPoints(stat: Stats): Int
	fun addXP(amt: Long)

	enum class Stats(val statName: String, val attribute: EntityAttribute) {
		HEALTH("easyrpg.gui.health", EntityAttributes.GENERIC_MAX_HEALTH),
		STRENGTH("easyrpg.generic.strength", EasyRpgAttributes.STRENGTH),
		DEXTERITY("easyrpg.generic.dexterity", EasyRpgAttributes.DEXTERITY),
		INTELLIGENCE("easyrpg.generic.intelligence", EasyRpgAttributes.INTELLIGENCE),
		DEFENSE("easyrpg.generic.defense", EasyRpgAttributes.DEFENSE)
	}
}

class RpgPlayer(private val player: PlayerEntity) : IRpgPlayer {
	override var level = 1
		set(value) {
			field = value; syncFlags = syncFlags or SyncFlags.SYNC_LEVEL
		}
	override var xp = 0L
		set(value) {
			field = value; syncFlags = syncFlags or SyncFlags.SYNC_XP
		}

	override var absorptionAmount = 0

	override var timer = 0L // Used for scaling modes that scale over time

	override val xpForLevel
		get() = if(level < config.players.maxLevel) xp - expForLevel(level - 1) else 0L

	override val xpReqTotal
		get() = expForLevel(level)

	override val xpReqForLevel
		get() = if(level < config.players.maxLevel) xpReqTotal - expForLevel(level - 1) else 0L

	override val remainingSP
		get() = (level * config.players.spGain).toInt() - (spStr + spDex + spInt + spDef + spHealth)

	private val health
		get() = baseHealth + (spHealth * config.players.healthOptions.spGain).toInt()

	private val strength
		get() = baseStr + (spStr * config.players.strengthOptions.spGain).toInt()

	private val dexterity
		get() = baseDex + (spDex * config.players.dexterityOptions.spGain).toInt()

	private val intelligence
		get() = baseInt + (spInt * config.players.intelligenceOptions.spGain).toInt()

	private val defense
		get() = baseDef + (spDef * config.players.defenseOptions.spGain).toInt()

	private val healthMult
		get() = config.players.healthOptions.multGain * (level - 1) + config.players.healthOptions.multiSpGain * spHealth

	private val strMult
		get() = config.players.strengthOptions.multGain * (level - 1) + config.players.strengthOptions.multiSpGain * spStr

	private val dexMult
		get() = config.players.dexterityOptions.multGain * (level - 1) + config.players.dexterityOptions.multiSpGain * spDex

	private val intMult
		get() = config.players.intelligenceOptions.multGain * (level - 1) + config.players.intelligenceOptions.multiSpGain * spInt

	private val defMult
		get() = config.players.defenseOptions.multGain * (level - 1) + config.players.defenseOptions.multiSpGain * spDef

	private val baseHealth
		get() = config.players.healthOptions.base + (config.players.healthOptions.gain * (level - 1)).toInt()
	private val baseStr
		get() = config.players.strengthOptions.base + (config.players.strengthOptions.gain * (level - 1)).toInt()
	private val baseDex
		get() = config.players.dexterityOptions.base + (config.players.dexterityOptions.gain * (level - 1)).toInt()
	private val baseInt
		get() = config.players.intelligenceOptions.base + (config.players.intelligenceOptions.gain * (level - 1)).toInt()
	private val baseDef
		get() = config.players.defenseOptions.base + (config.players.defenseOptions.gain * (level - 1)).toInt()

	private var spHealth = 0
		set(value) {
			field = value; syncFlags = syncFlags or SyncFlags.SYNC_HEALTH
		}
	private var spStr = 0
		set(value) {
			field = value; syncFlags = syncFlags or SyncFlags.SYNC_STR
		}
	private var spDex = 0
		set(value) {
			field = value; syncFlags = syncFlags or SyncFlags.SYNC_DEX
		}
	private var spInt = 0
		set(value) {
			field = value; syncFlags = syncFlags or SyncFlags.SYNC_INT
		}
	private var spDef = 0
		set(value) {
			field = value; syncFlags = syncFlags or SyncFlags.SYNC_DEF
		}

	private var syncFlags = -1
	private var lastSync = 0 // Time last synced for players using WAILA

	private fun expCurve(lvl: Int): Long {
		return if(lvl > 0) {
			if(config.players.experience.advancedExpCurve.isBlank()) {
				(config.players.experience.base * lvl.toDouble().pow(config.players.experience.exponent)).toLong()
			} else {
				Expression(config.players.experience.advancedExpCurve, EVALEX_CONFIG).with("level", lvl).evaluate().numberValue.toLong()
			}
		} else 0
	}

	private fun expForLevel(lvl: Int): Long {
		return if(lvl > 0) {
			expCurve(lvl) - expCurve(config.players.experience.levelOffset)
		} else 0
	}

	override fun getStat(stat: IRpgPlayer.Stats): Int {
		return when(stat) {
			IRpgPlayer.Stats.HEALTH -> health
			IRpgPlayer.Stats.STRENGTH -> strength
			IRpgPlayer.Stats.DEXTERITY -> dexterity
			IRpgPlayer.Stats.INTELLIGENCE -> intelligence
			IRpgPlayer.Stats.DEFENSE -> defense
		}
	}

	override fun addPoints(stat: IRpgPlayer.Stats, amt: Int) {
		val change = if(amt > 0) min(amt, remainingSP) else -min(abs(amt), getPoints(stat))
		when(stat) {
			IRpgPlayer.Stats.HEALTH -> spHealth += change
			IRpgPlayer.Stats.STRENGTH -> spStr += change
			IRpgPlayer.Stats.DEXTERITY -> spDex += change
			IRpgPlayer.Stats.INTELLIGENCE -> spInt += change
			IRpgPlayer.Stats.DEFENSE -> spDef += change
		}
	}

	override fun getPoints(stat: IRpgPlayer.Stats): Int {
		return when(stat) {
			IRpgPlayer.Stats.HEALTH -> spHealth
			IRpgPlayer.Stats.STRENGTH -> spStr
			IRpgPlayer.Stats.DEXTERITY -> spDex
			IRpgPlayer.Stats.INTELLIGENCE -> spInt
			IRpgPlayer.Stats.DEFENSE -> spDef
		}
	}

	override fun readFromNbt(tag: NbtCompound) {
		level = tag.getInt("Level")
		xp = tag.getLong("Xp")
		spHealth = tag.getInt("SpHealth")
		spStr = tag.getInt("SpStr")
		spDex = tag.getInt("SpDex")
		spInt = tag.getInt("SpInt")
		spDef = tag.getInt("SpDef")
		timer = tag.getLong("Ticks")
		absorptionAmount = tag.getInt("AbsorptionValue")
	}

	override fun writeToNbt(tag: NbtCompound) {
		tag.putInt("Level", level)
		tag.putLong("Xp", xp)
		tag.putInt("SpHealth", spHealth)
		tag.putInt("SpStr", spStr)
		tag.putInt("SpDex", spDex)
		tag.putInt("SpInt", spInt)
		tag.putInt("SpDef", spDef)
		tag.putLong("Ticks", timer)
		tag.putInt("AbsorptionValue", absorptionAmount)
	}

	override fun serverTick() {
		// Prevent invalid exp values by resetting the player's level if exp is less than the requirements for the
		// previous level.
		if(xp < expForLevel(level - 1)) {
			level = 1
		}
		while(level < config.players.maxLevel && xp >= xpReqTotal) {
			++level
		}

		++timer

		val maxHealthInst = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
		val toughnessInst = player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)
		val strInst = player.getAttributeInstance(EasyRpgAttributes.STRENGTH)
		val dexInst = player.getAttributeInstance(EasyRpgAttributes.DEXTERITY)
		val intInst = player.getAttributeInstance(EasyRpgAttributes.INTELLIGENCE)
		val defInst = player.getAttributeInstance(EasyRpgAttributes.DEFENSE)

		strInst!!.baseValue = baseStr.toDouble()
		dexInst!!.baseValue = baseDex.toDouble()
		intInst!!.baseValue = baseInt.toDouble()
		defInst!!.baseValue = baseDef.toDouble()

		checkAttributeModifiers(maxHealthInst!!, IRpgEntity.HEALTH_MODIFIER, "Easy RPG Health", health.toDouble())
		checkAttributeModifiersMultiplier(maxHealthInst, IRpgEntity.HEALTH_MULTIPLIER, "Easy RPG Health Mult", healthMult)
		checkAttributeModifiers(toughnessInst!!, IRpgEntity.BASE_TOUGHNESS, "Easy RPG Base Toughness", 1.0)
		checkAttributeModifiersMultiplier(
			toughnessInst, IRpgEntity.TOUGHNESS_MODIFIER, "Easy RPG Toughness",
			(config.players.toughness.gain * (level - 1) + (config.players.toughness.base - 1)) * (1 + config.players.toughness.multGain * (level - 1))
		)
		checkAttributeModifiers(strInst, IRpgEntity.STRENGTH_MODIFIER, "Easy RPG Strength", strength.toDouble())
		checkAttributeModifiersMultiplier(strInst, IRpgEntity.STRENGTH_MULTIPLIER, "Easy RPG Strength Mult", strMult)
		checkAttributeModifiers(dexInst, IRpgEntity.DEXTERITY_MODIFIER, "Easy RPG Dexterity", dexterity.toDouble())
		checkAttributeModifiersMultiplier(dexInst, IRpgEntity.DEXTERITY_MULTIPLIER, "Easy RPG Dexterity Mult", dexMult)
		checkAttributeModifiers(
			intInst, IRpgEntity.INTELLIGENCE_MODIFIER, "Easy RPG Intelligence",
			intelligence.toDouble()
		)
		checkAttributeModifiersMultiplier(intInst, IRpgEntity.INTELLIGENCE_MULTIPLIER, "Easy RPG Intelligence Mult", intMult)
		checkAttributeModifiers(defInst, IRpgEntity.DEFENSE_MODIFIER, "Easy RPG Max Defence", defense.toDouble())
		checkAttributeModifiersMultiplier(defInst, IRpgEntity.DEFENSE_MULTIPLIER, "Easy RPG Defense Mult", defMult)

		if(syncFlags != 0) {
			RPG_PLAYER.sync(player)
			syncFlags = 0
		}
		if(Util.getMeasuringTimeMs() - lastSync > 500) {
			val buf = PacketByteBufs.create()
			buf.writeUuid(player.uuid)
			buf.writeVarInt(level)
			for(p in PlayerLookup.around(player.world as ServerWorld, player.pos, 32.0)) {
				if(p != player) ServerPlayNetworking.send(p, SYNC_OTHER_PLAYER, buf)
			}
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
							id, name, value - instance.baseValue,
							EntityAttributeModifier.Operation.ADDITION
						)
					)
				}
			}
		} else {
			if(value - instance.baseValue > 0) {
				instance.addPersistentModifier(
					EntityAttributeModifier(
						id, name, value - instance.baseValue,
						EntityAttributeModifier.Operation.ADDITION
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

	override fun writeSyncPacket(buf: PacketByteBuf, recipient: ServerPlayerEntity?) {
		buf.writeVarInt(syncFlags)

		if(syncFlags and SyncFlags.SYNC_LEVEL != 0) {
			buf.writeVarInt(level)
		}
		if(syncFlags and SyncFlags.SYNC_XP != 0) {
			buf.writeVarLong(xp)
		}
		if(syncFlags and SyncFlags.SYNC_HEALTH != 0) {
			buf.writeVarInt(spHealth)
		}
		if(syncFlags and SyncFlags.SYNC_STR != 0) {
			buf.writeVarInt(spStr)
		}
		if(syncFlags and SyncFlags.SYNC_DEX != 0) {
			buf.writeVarInt(spDex)
		}
		if(syncFlags and SyncFlags.SYNC_INT != 0) {
			buf.writeVarInt(spInt)
		}
		if(syncFlags and SyncFlags.SYNC_DEF != 0) {
			buf.writeVarInt(spDef)
		}
	}

	override fun applySyncPacket(buf: PacketByteBuf) {
		val flags = buf.readVarInt()
		if(flags and SyncFlags.SYNC_LEVEL != 0) {
			level = buf.readVarInt()
		}
		if(flags and SyncFlags.SYNC_XP != 0) {
			xp = buf.readVarLong()
		}
		if(flags and SyncFlags.SYNC_HEALTH != 0) {
			spHealth = buf.readVarInt()
		}
		if(flags and SyncFlags.SYNC_STR != 0) {
			spStr = buf.readVarInt()
		}
		if(flags and SyncFlags.SYNC_DEX != 0) {
			spDex = buf.readVarInt()
		}
		if(flags and SyncFlags.SYNC_INT != 0) {
			spInt = buf.readVarInt()
		}
		if(flags and SyncFlags.SYNC_DEF != 0) {
			spDef = buf.readVarInt()
		}
	}

	override fun addXP(amt: Long) {
		if(amt < 1) return
		if(Long.MAX_VALUE - amt > xp) {
			xp += amt
		} else xp = Long.MAX_VALUE
	}

	override fun shouldSyncWith(player: ServerPlayerEntity?): Boolean {
		return player == this.player
	}

	companion object {
		@JvmStatic
		fun registerEntityComponentFactories(registry: EntityComponentFactoryRegistry) {
			registry.registerForPlayers(RPG_PLAYER, ::RpgPlayer, RespawnCopyStrategy.ALWAYS_COPY)
		}
	}

	private object SyncFlags {
		const val SYNC_LEVEL = 0x1
		const val SYNC_XP = 0x2
		const val SYNC_STR = 0x4
		const val SYNC_DEX = 0x8
		const val SYNC_INT = 0x10
		const val SYNC_DEF = 0x20
		const val SYNC_HEALTH = 0x40
	}
}