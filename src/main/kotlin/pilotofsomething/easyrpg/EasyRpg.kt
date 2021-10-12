package pilotofsomething.easyrpg

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.ClampedEntityAttribute
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

val ADD_STAT_ID = Identifier("easy_rpg", "change_stat")

fun registerEntityAttribute(id: String, attribute: EntityAttribute): EntityAttribute {
    return Registry.register(Registry.ATTRIBUTE, "easy_rpg:$id", attribute)
}

object SimpleRpgAttributes {
    val STRENGTH = registerEntityAttribute("generic.strength", ClampedEntityAttribute("easyrpg.generic.strength", 10.0, 1.0, Int.MAX_VALUE.toDouble()).setTracked(true))
    val DEXTERITY = registerEntityAttribute("generic.dexterity", ClampedEntityAttribute("easyrpg.generic.dexterity", 10.0, 1.0, Int.MAX_VALUE.toDouble()).setTracked(true))
    val INTELLIGENCE = registerEntityAttribute("generic.intelligence", ClampedEntityAttribute("easyrpg.generic.intelligence", 10.0, 1.0, Int.MAX_VALUE.toDouble()).setTracked(true))
    val DEFENSE = registerEntityAttribute("generic.defense", ClampedEntityAttribute("easyrpg.generic.defense", 10.0, 1.0, Int.MAX_VALUE.toDouble()).setTracked(true))
}

@Suppress("UNUSED")
object EasyRpg : ModInitializer, ClientModInitializer {
    private const val MOD_ID = "easy_rpg"

    override fun onInitialize() {
        AutoConfig.register(ModConfig::class.java, ::GsonConfigSerializer)
        config = AutoConfig.getConfigHolder(ModConfig::class.java).config

        ServerPlayNetworking.registerGlobalReceiver(ADD_STAT_ID) { server: MinecraftServer, player: ServerPlayerEntity, _: ServerPlayNetworkHandler, buf: PacketByteBuf, _: PacketSender ->
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
                val mRpg = RPG_MOB.get(killedEntity)

                val base = config.entities.expOptions.baseValue * mRpg.level.toDouble().pow(config.entities.expOptions.exponent)

                val baseHealth = killedEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)?.baseValue ?: 0.0
                val attack = killedEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)?.value ?: 0.0
                val armor = killedEntity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR)?.value ?: 0.0

                val mobID = Registry.ENTITY_TYPE.getId(killedEntity.type).toString()
                val worth = if(config.entities.expOptions.mobModifiers.mobValueOverrides.containsKey(mobID)) {
                    config.entities.expOptions.mobModifiers.mobValueOverrides[mobID]!!
                } else (baseHealth / config.entities.expOptions.mobModifiers.health.base * config.entities.expOptions.mobModifiers.health.value) +
                         (attack / config.entities.expOptions.mobModifiers.attack.base * config.entities.expOptions.mobModifiers.attack.value) +
                           (armor / config.entities.expOptions.mobModifiers.armor.base * config.entities.expOptions.mobModifiers.armor.value)

                val scaleFactor = min(max(
                    (1 + config.entities.expOptions.scalingSettings.scalingAmount * (mRpg.level - pRpg.level)) * if (mRpg.level - pRpg.level > 0)
                        { config.entities.expOptions.scalingSettings.exponentialIncreaseAmount.pow(mRpg.level - pRpg.level) } else config.entities.expOptions.scalingSettings.exponentialDecreaseAmount.pow(mRpg.level - pRpg.level),
                    config.entities.expOptions.scalingSettings.scalingMin), config.entities.expOptions.scalingSettings.scalingMax)
                pRpg.addXP(max((base * worth * scaleFactor).toLong(), 1))
            }
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            EasyRpgCommand.register(dispatcher)
        }
    }

    override fun onInitializeClient() {
        val openStats = KeyBindingHelper.registerKeyBinding(KeyBinding("easyrpg.key_bind.open_stats", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "easyrpg.key_bind.category"))

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if(openStats.isPressed) {
                client.setScreen(StatsScreen(StatsGui()))
            }
        }
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
