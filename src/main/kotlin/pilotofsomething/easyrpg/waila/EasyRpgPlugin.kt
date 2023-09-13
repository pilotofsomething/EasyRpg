package pilotofsomething.easyrpg.waila

import mcp.mobius.waila.api.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.resource.language.I18n
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import pilotofsomething.easyrpg.calculateExpValue
import pilotofsomething.easyrpg.components.RPG_MOB
import pilotofsomething.easyrpg.components.RPG_PLAYER

object Settings {
	val showLevels = Identifier("easy_rpg_waila:show_levels")
	val showXpValue = Identifier("easy_rpg_waila:show_xp_value")
}

class EasyRpgPlugin : IWailaPlugin {
	override fun register(registrar: IRegistrar) {
		registrar.addConfig(Settings.showLevels, true)
		registrar.addConfig(Settings.showXpValue, true)
		registrar.addComponent(LivingEntityComponent(), TooltipPosition.BODY, LivingEntity::class.java)
	}
}

class LivingEntityComponent : IEntityComponentProvider {
	override fun appendBody(tooltip: ITooltip, accessor: IEntityAccessor, config: IPluginConfig) {
		val entity = accessor.getEntity<LivingEntity>()
		if(config.getBoolean(Settings.showLevels)) {
			val rpg = if(entity !is PlayerEntity) {
				RPG_MOB.get(entity)
			} else RPG_PLAYER.get(entity)
			tooltip.addLine(Text.translatable("easyrpg.waila.level").append(Text.literal(I18n.translate(" %,d", rpg.level))))
		}
		if(config.getBoolean(Settings.showXpValue) && entity !is PlayerEntity) {
			val xpV = calculateExpValue(MinecraftClient.getInstance().player, entity)
			if(xpV < 10000000000L) {
				tooltip.addLine(Text.translatable("easyrpg.waila.xp").append(Text.literal(I18n.translate(" %,d", xpV))))
			} else tooltip.addLine(Text.translatable("easyrpg.waila.xp").append(Text.literal(I18n.translate(" %.5E", xpV))))
		}
	}
}
