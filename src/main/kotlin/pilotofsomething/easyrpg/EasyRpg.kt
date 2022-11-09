package pilotofsomething.easyrpg

import com.google.gson.Gson
import dev.emi.trinkets.api.Trinket
import dev.emi.trinkets.api.TrinketsApi
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents
import net.fabricmc.fabric.api.networking.v1.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.ClampedEntityAttribute
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.lwjgl.glfw.GLFW
import pilotofsomething.easyrpg.command.EasyRpgCommand
import pilotofsomething.easyrpg.components.IRpgPlayer
import pilotofsomething.easyrpg.components.RPG_MOB
import pilotofsomething.easyrpg.components.RPG_PLAYER
import pilotofsomething.easyrpg.gui.StatsGui
import pilotofsomething.easyrpg.gui.StatsScreen
import pilotofsomething.easyrpg.item.EasyRpgItems
import pilotofsomething.easyrpg.item.setupQualities
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

val ADD_STAT_ID = Identifier("easy_rpg", "change_stat")
val SYNC_OTHER_PLAYER = Identifier("easy_rpg", "sync_other")
val SYNC_CONFIG = Identifier("easy_rpg", "sync_config")

fun registerEntityAttribute(id: String, attribute: EntityAttribute): EntityAttribute {
	return Registry.register(Registry.ATTRIBUTE, "easy_rpg:$id", attribute)
}

object EasyRpgAttributes {
	val STRENGTH = registerEntityAttribute("generic.strength",
	                                       ClampedEntityAttribute("easyrpg.generic.strength", 10.0, 1.0,
	                                                              Int.MAX_VALUE.toDouble()).setTracked(true))
	val DEXTERITY = registerEntityAttribute("generic.dexterity",
	                                        ClampedEntityAttribute("easyrpg.generic.dexterity", 10.0, 1.0,
	                                                               Int.MAX_VALUE.toDouble()).setTracked(true))
	val INTELLIGENCE = registerEntityAttribute("generic.intelligence",
	                                           ClampedEntityAttribute("easyrpg.generic.intelligence", 10.0, 1.0,
	                                                                  Int.MAX_VALUE.toDouble()).setTracked(true))
	val DEFENSE = registerEntityAttribute("generic.defense",
	                                      ClampedEntityAttribute("easyrpg.generic.defense", 10.0, 1.0,
	                                                             Int.MAX_VALUE.toDouble()).setTracked(true))
}

@Suppress("UNUSED")
object EasyRpg : ModInitializer, ClientModInitializer {
	private const val MOD_ID = "easy_rpg"

	override fun onInitialize() {
		AutoConfig.register(ModConfig::class.java, ::GsonConfigSerializer)
		config = AutoConfig.getConfigHolder(ModConfig::class.java).config
		serverConfig.client = config.client
		setupQualities()

		ServerPlayNetworking.registerGlobalReceiver(
			ADD_STAT_ID) { server: MinecraftServer, player: ServerPlayerEntity, _: ServerPlayNetworkHandler, buf: PacketByteBuf, _: PacketSender ->
			val stat = IRpgPlayer.Stats.values()[buf.readVarInt()]
			val amt = buf.readVarInt()
			server.execute {
				val rpg = RPG_PLAYER.get(player)
				rpg.addPoints(stat, amt)
			}
		}

		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register { _, entity, killedEntity ->
			if(entity is PlayerEntity && killedEntity !is PlayerEntity && killedEntity is LivingEntity) {
				val pRpg = RPG_PLAYER.get(entity)
				pRpg.addXP(calculateExpValue(entity, killedEntity))
			}
		}

		ServerPlayConnectionEvents.JOIN.register { _: ServerPlayNetworkHandler, sender: PacketSender, _: MinecraftServer ->
			var buf = PacketByteBufs.create()
			buf.writeVarInt(0)
			buf.writeString(Gson().toJson(config.players))
			sender.sendPacket(SYNC_CONFIG, buf)

			buf = PacketByteBufs.create()
			buf.writeVarInt(1)
			buf.writeString(Gson().toJson(config.statCaps))
			sender.sendPacket(SYNC_CONFIG, buf)

			buf = PacketByteBufs.create()
			buf.writeVarInt(2)
			buf.writeString(Gson().toJson(config.items))
			sender.sendPacket(SYNC_CONFIG, buf)

			buf = PacketByteBufs.create()
			buf.writeVarInt(3)
			buf.writeString(Gson().toJson(config.entities))
			sender.sendPacket(SYNC_CONFIG, buf)
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			EasyRpgCommand.register(dispatcher)
		}

		// Attack damage is set to be tracked so the WAILA plugin can estimate exp value
		EntityAttributes.GENERIC_ATTACK_DAMAGE.isTracked = true
		EasyRpgItems.registerItems()
	}

	override fun onInitializeClient() {
		val openStats = KeyBindingHelper.registerKeyBinding(
			KeyBinding("easyrpg.key_bind.open_stats", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z,
			           "easyrpg.key_bind.category"))

		ClientPlayNetworking.registerGlobalReceiver(SYNC_OTHER_PLAYER) { client, _, buf, _ ->
			val uuid = buf.readUuid()
			val level = buf.readVarInt()
			client.execute {
				val player = client.world?.getPlayerByUuid(uuid) ?: return@execute
				val rpg = RPG_PLAYER.get(player)
				rpg.level = level
			}
		}

		ClientPlayNetworking.registerGlobalReceiver(SYNC_CONFIG) { client: MinecraftClient, _: ClientPlayNetworkHandler, buf: PacketByteBuf, _: PacketSender ->
			val id = buf.readVarInt()
			val json = buf.readString()
			client.execute {
				when(id) {
					0 -> serverConfig.players = Gson().fromJson(json, ModConfig.PlayerOptions::class.java)
					1 -> serverConfig.statCaps = Gson().fromJson(json, ModConfig.StatCapOptions::class.java)
					2 -> serverConfig.items = Gson().fromJson(json, ModConfig.ItemOptions::class.java)
					3 -> serverConfig.entities = Gson().fromJson(json, ModConfig.EntitiesOptions::class.java)
				}
			}
		}

		ClientPlayConnectionEvents.JOIN.register { _: ClientPlayNetworkHandler, _: PacketSender, client: MinecraftClient ->
			client.execute {
				config = serverConfig
			}
		}

		ClientPlayConnectionEvents.DISCONNECT.register { _: ClientPlayNetworkHandler, client: MinecraftClient ->
			client.execute {
				val holder = AutoConfig.getConfigHolder(ModConfig::class.java)
				config = holder.get()
			}
		}

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			if(openStats.isPressed) {
				client.setScreen(StatsScreen(StatsGui()))
			}
		}
	}
}

fun calculateExpValue(entity: PlayerEntity?, killedEntity: LivingEntity): Long {
	if(entity == null || killedEntity is PlayerEntity) return 0
	if(config.players.experience.useVanillaExp) return 0
	val pRpg = RPG_PLAYER.get(entity)
	val mRpg = RPG_MOB.get(killedEntity)

	val base = config.entities.expOptions.baseValue * mRpg.level.toDouble()
		.pow(config.entities.expOptions.exponent)

	val baseHealth =
		killedEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)?.baseValue ?: 0.0
	val attack = killedEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)?.value ?: 0.0
	val armor = killedEntity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR)?.value ?: 0.0

	val mobID = Registry.ENTITY_TYPE.getId(killedEntity.type).toString()
	val worth = if (config.entities.expOptions.mobModifiers.mobValueOverrides.containsKey(mobID)) {
		config.entities.expOptions.mobModifiers.mobValueOverrides[mobID]!!
	} else (baseHealth / config.entities.expOptions.mobModifiers.health.base * config.entities.expOptions.mobModifiers.health.value) +
			(attack / config.entities.expOptions.mobModifiers.attack.base * config.entities.expOptions.mobModifiers.attack.value) +
			(armor / config.entities.expOptions.mobModifiers.armor.base * config.entities.expOptions.mobModifiers.armor.value)

	val scaleFactor = min(
		max(
			(1 + config.entities.expOptions.scalingSettings.scalingAmount * (mRpg.level - pRpg.level)) * if (mRpg.level - pRpg.level > 0) {
				config.entities.expOptions.scalingSettings.exponentialIncreaseAmount.pow(
					mRpg.level - pRpg.level
				)
			} else config.entities.expOptions.scalingSettings.exponentialDecreaseAmount.pow(
				mRpg.level - pRpg.level
			),
			config.entities.expOptions.scalingSettings.scalingMin
		),
		config.entities.expOptions.scalingSettings.scalingMax
	)
	return min(max((base * worth * scaleFactor).toLong(), 1), config.entities.expOptions.scalingSettings.expCap)
}

fun calculateVanillaExpValue(entity: PlayerEntity?, killedEntity: LivingEntity, xp: Int): Int {
	if(entity == null || killedEntity is PlayerEntity) return xp
	if(!config.entities.expOptions.scalingSettings.scaleVanillaExp) return xp

	val pRpg = RPG_PLAYER.get(entity)
	val mRpg = RPG_MOB.get(killedEntity)

	val scaleFactor = min(
		max(
			(1 + config.entities.expOptions.scalingSettings.scalingAmount * (mRpg.level - pRpg.level)) * if (mRpg.level - pRpg.level > 0) {
				config.entities.expOptions.scalingSettings.exponentialIncreaseAmount.pow(
					mRpg.level - pRpg.level
				)
			} else config.entities.expOptions.scalingSettings.exponentialDecreaseAmount.pow(
				mRpg.level - pRpg.level
			),
			config.entities.expOptions.scalingSettings.scalingMin
		),
		config.entities.expOptions.scalingSettings.scalingMax
	)
	return min(max((xp * scaleFactor).toLong(), 1), config.entities.expOptions.scalingSettings.expCap).toInt()
}

fun isItemTrinket(stack: ItemStack): Boolean {
	if(FabricLoader.getInstance().isModLoaded("trinkets")) {
		return stack.item is Trinket || TrinketsApi.getTrinket(stack.item) != TrinketsApi.getDefaultTrinket()
	}
	return false
}

fun getAttributeMax(attribute: ClampedEntityAttribute): Double? {
	return when(attribute) {
		EntityAttributes.GENERIC_MAX_HEALTH -> config.statCaps.hpCap.toDouble()
		EntityAttributes.GENERIC_ARMOR_TOUGHNESS -> config.statCaps.armorToughnessCap.toDouble()
		EasyRpgAttributes.STRENGTH -> config.statCaps.strCap.toDouble()
		EasyRpgAttributes.DEXTERITY -> config.statCaps.dexCap.toDouble()
		EasyRpgAttributes.INTELLIGENCE -> config.statCaps.intCap.toDouble()
		EasyRpgAttributes.DEFENSE -> config.statCaps.defCap.toDouble()
		else -> null
	}
}

@Suppress("UNUSED")
enum class ScalingMode(val distance: Boolean, val time: Boolean, val level: Boolean) {
	DISTANCE(true, false, false),
	DISTANCE_TIME(true, true, false),
	TIME(false, true, false),
	LEVEL(false, false, true),
	LEVEL_TIME(false, true, true),
	LEVEL_DISTANCE(true, false, true),
	LEVEL_DISTANCE_TIME(true, true, true)
}
