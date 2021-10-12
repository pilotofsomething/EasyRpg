package pilotofsomething.easyrpg.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import net.minecraft.client.resource.language.I18n
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import pilotofsomething.easyrpg.components.IRpgPlayer
import pilotofsomething.easyrpg.components.RPG_PLAYER
import pilotofsomething.easyrpg.config

object EasyRpgCommand {

	fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
		dispatcher.register(
			literal("easyrpg").then(
				literal("stats").executes { context -> displayStats(context.source, context.source.player) }.then(
					argument("player", EntityArgumentType.player()).executes { context ->
						displayStats(
							context.source, EntityArgumentType.getPlayer(
								context, "player"
							)
						)
					})
			).then(
				literal("set").requires { source -> source.hasPermissionLevel(2) }.then(
					literal("level").then(
						argument(
							"value", IntegerArgumentType.integer(1, config.players.maxLevel)
						).executes { context ->
							setLevel(
								context.source, context.source.player, IntegerArgumentType.getInteger(context, "value")
							)
						}.then(
							argument("player", EntityArgumentType.player()).executes { context ->
								setLevel(
									context.source, EntityArgumentType.getPlayer(context, "player"),
									IntegerArgumentType.getInteger(context, "value")
								)
							})
					)
				).then(
					literal("xp").then(
						argument("value", LongArgumentType.longArg(0)).executes { context ->
							setXp(
								context.source, context.source.player, LongArgumentType.getLong(context, "value")
							)
						}.then(
							argument("player", EntityArgumentType.player()).executes { context ->
								setXp(
									context.source, EntityArgumentType.getPlayer(
										context, "player"
									), LongArgumentType.getLong(
										context, "value"
									)
								)
							})
					)
				)
			).then(
				literal("add").requires { source -> source.hasPermissionLevel(2) }.then(
					literal("level").then(
						argument("amount", IntegerArgumentType.integer(1)).executes { context ->
							addLevel(
								context.source, context.source.player, IntegerArgumentType.getInteger(context, "amount")
							)
						}.then(
							argument("player", EntityArgumentType.player()).executes { context ->
								addLevel(
									context.source, EntityArgumentType.getPlayer(context, "player"),
									IntegerArgumentType.getInteger(context, "amount")
								)
							})
					)
				).then(
					literal("xp").then(
						argument("amount", LongArgumentType.longArg(0)).executes { context ->
							addXp(
								context.source, context.source.player, LongArgumentType.getLong(context, "amount")
							)
						}.then(
							argument("player", EntityArgumentType.player()).executes { context ->
								addXp(
									context.source, EntityArgumentType.getPlayer(
										context, "player"
									), LongArgumentType.getLong(
										context, "amount"
									)
								)
							})
					)
				)
			)
		)
	}

	private fun displayStats(source: ServerCommandSource, player: PlayerEntity?): Int {
		if(player == null) {
			source.sendError(TranslatableText("easyrpg.command.player_null"))
			return 0
		}
		if(source.player != player) {
			source.sendFeedback(TranslatableText("easyrpg.command.player.stats", player.name), false)
		} else source.sendFeedback(TranslatableText("easyrpg.command.player.stats.self"), false)

		val rpg = RPG_PLAYER.get(player)

		source.sendFeedback(Text.of(I18n.translate("easyrpg.command.player.stats.level", rpg.level)), false)
		source.sendFeedback(Text.of(I18n.translate("easyrpg.command.player.stats.total_xp", rpg.xp)), false)
		if(rpg.level < config.players.maxLevel) {
			source.sendFeedback(
				Text.of(I18n.translate("easyrpg.command.player.stats.xp_level", rpg.xpForLevel, rpg.xpReqForLevel)),
				false
			)
			source.sendFeedback(
				Text.of(I18n.translate("easyrpg.command.player.stats.xp_tnl", rpg.xpReqForLevel - rpg.xpForLevel)),
				false
			)
		} else {
			source.sendFeedback(
				Text.of(I18n.translate("easyrpg.command.player.stats.xp_level.max", rpg.xpForLevel, rpg.xpReqForLevel)),
				false
			)
			source.sendFeedback(
				Text.of(I18n.translate("easyrpg.command.player.stats.xp_tnl.max", rpg.xpReqForLevel - rpg.xpForLevel)),
				false
			)
		}

		for(stat in IRpgPlayer.Stats.values()) {
			source.sendFeedback(
				Text.of("  ${I18n.translate(stat.statName)}: ${String.format("%,d", rpg.getStat(stat))}"), false
			)
		}

		return 1
	}

	private fun setLevel(source: ServerCommandSource, player: PlayerEntity?, level: Int): Int {
		if(player == null) {
			source.sendError(TranslatableText("easyrpg.command.player_null"))
			return 0
		}
		val rpg = RPG_PLAYER.get(player)
		rpg.level = level - 1
		rpg.xp = rpg.xpReqTotal
		source.sendFeedback(
			Text.of(I18n.translate("easyrpg.command.player.set_level", player.name.asString(), level)), true
		)
		return 1
	}

	private fun setXp(source: ServerCommandSource, player: PlayerEntity?, xp: Long): Int {
		if(player == null) {
			source.sendError(TranslatableText("easyrpg.command.player_null"))
			return 0
		}
		val rpg = RPG_PLAYER.get(player)
		rpg.level = 1
		rpg.xp = xp
		source.sendFeedback(Text.of(I18n.translate("easyrpg.command.player.set_xp", player.name.asString(), xp)), true)
		return 1
	}

	private fun addLevel(source: ServerCommandSource, player: PlayerEntity?, amount: Int): Int {
		if(player == null) {
			source.sendError(TranslatableText("easyrpg.command.player_null"))
			return 0
		}
		val rpg = RPG_PLAYER.get(player)
		rpg.level += amount - 1
		rpg.xp = rpg.xpReqTotal
		source.sendFeedback(
			Text.of(I18n.translate("easyrpg.command.player.add_level", player.name.asString(), amount)), true
		)
		return 1
	}

	private fun addXp(source: ServerCommandSource, player: PlayerEntity?, amount: Long): Int {
		if(player == null) {
			source.sendError(TranslatableText("easyrpg.command.player_null"))
			return 0
		}
		val rpg = RPG_PLAYER.get(player)
		rpg.addXP(amount)
		source.sendFeedback(
			Text.of(I18n.translate("easyrpg.command.player.add_xp", player.name.asString(), amount)), true
		)
		return 1
	}

}