package pilotofsomething.easyrpg

import com.ezylang.evalex.Expression
import com.ezylang.evalex.config.ExpressionConfiguration
import draylar.omegaconfig.OmegaConfig
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents
import net.fabricmc.fabric.api.networking.v1.*
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.ClampedEntityAttribute
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import pilotofsomething.easyrpg.command.EasyRpgCommand
import pilotofsomething.easyrpg.components.IRpgPlayer
import pilotofsomething.easyrpg.components.RPG_MOB
import pilotofsomething.easyrpg.components.RPG_PLAYER
import pilotofsomething.easyrpg.gui.StatsGui
import pilotofsomething.easyrpg.gui.StatsScreen
import pilotofsomething.easyrpg.mixins.LivingEntityInvoker
import java.math.MathContext
import kotlin.math.max
import kotlin.math.min

val ADD_STAT_ID = Identifier("easy_rpg", "change_stat")
val SYNC_OTHER_PLAYER = Identifier("easy_rpg", "sync_other")

val EVALEX_CONFIG = ExpressionConfiguration.builder().mathContext(MathContext.DECIMAL64).build()

fun registerEntityAttribute(id: String, attribute: EntityAttribute): EntityAttribute {
	return Registry.register(Registries.ATTRIBUTE, "easy_rpg:$id", attribute)
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
		config = OmegaConfig.register(ModConfig::class.java)

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

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			EasyRpgCommand.register(dispatcher)
		}

		// Attack damage is set to be tracked so the WAILA plugin can estimate exp value
		EntityAttributes.GENERIC_ATTACK_DAMAGE.isTracked = true
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

	val mobID = Registries.ENTITY_TYPE.getId(killedEntity.type).toString()

	val baseHealth =
		killedEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)?.baseValue ?: 0.0
	val attack = killedEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)?.value ?: 0.0
	val armor = killedEntity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR)?.value ?: 0.0
	val vExp = (killedEntity as LivingEntityInvoker).invokeGetXpToDrop()

	val exp = Expression(config.entities.experience.expFormula, EVALEX_CONFIG)
		.with("hasValue", config.entities.experience.mobValues.containsKey(mobID))
		.and("value", config.entities.experience.mobValues[mobID] ?: 1)
		.and("health", baseHealth)
		.and("attack", attack)
		.and("armor", armor)
		.and("level", mRpg.level)
		.and("vexp", vExp).evaluate().numberValue.toDouble()

	val scaleFactor = Expression(config.entities.experience.scalingFormula, EVALEX_CONFIG)
		.with("plevel", pRpg.level)
		.and("elevel", mRpg.level).evaluate().numberValue.toDouble()

	return min(max((exp * scaleFactor).toLong(), 1), config.entities.experience.expCap)
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
