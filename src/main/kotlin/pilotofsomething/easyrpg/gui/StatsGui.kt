package pilotofsomething.easyrpg.gui

import io.github.cottonmc.cotton.gui.GuiDescription
import io.github.cottonmc.cotton.gui.client.CottonClientScreen
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import io.github.cottonmc.cotton.gui.widget.data.Texture
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.resource.language.I18n
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import pilotofsomething.easyrpg.ADD_STAT_ID
import pilotofsomething.easyrpg.components.IRpgPlayer
import pilotofsomething.easyrpg.components.RPG_PLAYER
import pilotofsomething.easyrpg.config

class StatsScreen(desc: GuiDescription) : CottonClientScreen(desc) {
	override fun shouldPause(): Boolean {
		return false
	}
}

class StatsGui : LightweightGuiDescription() {
	init {
		val mc = MinecraftClient.getInstance()
		val rpg = RPG_PLAYER.get(mc.player!!)
		val root = WPlainPanel()
		setRootPanel(root)
		root.setSize(264, 240)

		val level = WDynamicLabel { I18n.translate("easyrpg.gui.level", rpg.level) }
		root.add(level, 4, 4)

		val levelXp = WDynamicLabel {
			if(rpg.level < config.players.maxLevel) {
				String.format("%,d/%,d", rpg.xpForLevel, rpg.xpReqForLevel)
			} else I18n.translate("easyrpg.gui.max_level")
		}
		levelXp.setAlignment(HorizontalAlignment.RIGHT)
		root.add(levelXp, 243, 4)

		val xpBar = WColoredBar(
			{ if(rpg.xpReqForLevel > 0) rpg.xpForLevel / rpg.xpReqForLevel.toFloat() else 1.0f },
			WBar.Direction.RIGHT
		)
		xpBar.color = 0xB6FF00
		xpBar.opacity = 0.67f
		xpBar.backgroundOpacity = 0.33f
		root.add(xpBar, 4, 14, 256, 4)

		val playerStats = WListPanel(
			IRpgPlayer.Stats.values().toList(), { WPlainPanel() }) { stat: IRpgPlayer.Stats, p: WPlainPanel ->
			p.setSize(248, 18)
			val name = WLabel(Text.translatable(stat.statName))
			name.horizontalAlignment = HorizontalAlignment.CENTER
			p.add(name, p.width / 2, 0)
			val label =
				WDynamicLabel { String.format("%,d", mc.player?.getAttributeValue(stat.attribute)?.toInt() ?: 0) }
			label.setAlignment(HorizontalAlignment.CENTER)
			p.add(label, p.width / 2, 10)
		}
		playerStats.setListItemHeight(18)
		root.add(playerStats, 4, 22, 256, 92)

		val spLabel = WLabel(Text.translatable("easyrpg.gui.stat_points"))
		spLabel.horizontalAlignment = HorizontalAlignment.CENTER
		root.add(spLabel, 132, 120)
		val spLabelC =
			WDynamicLabel { String.format("%,d/%,d", rpg.remainingSP, (rpg.level * config.players.spGain).toInt()) }
		spLabelC.setAlignment(HorizontalAlignment.CENTER)
		root.add(spLabelC, 132, 130)

		val statList =
			WListPanel(IRpgPlayer.Stats.values().toList(), { StatPanel() }) { stat: IRpgPlayer.Stats, p: StatPanel ->
				p.configure(stat, rpg)
			}
		statList.setListItemHeight(18)
		root.add(statList, 4, 140, 256, 92)
	}
}

class StatPanel : WPlainPanel() {
	private val label = WLabel(Text.literal(""))

	init {
		setSize(248, 18)
		label.horizontalAlignment = HorizontalAlignment.CENTER
		add(label, width / 2, 2)
	}

	fun configure(stat: IRpgPlayer.Stats, rpg: IRpgPlayer) {
		label.text = Text.of(I18n.translate(stat.statName))
		val count = WDynamicLabel { String.format("%,d", rpg.getPoints(stat)) }
		count.setAlignment(HorizontalAlignment.CENTER)
		add(count, width / 2, 12)

		val subButton = WButton(Text.of("-"))
		subButton.onClick = Runnable {
			val buf = PacketByteBufs.create()
			buf.writeVarInt(stat.ordinal)
			buf.writeVarInt(
				-1 * (if(InputUtil.isKeyPressed(
						MinecraftClient.getInstance().window.handle, InputUtil.GLFW_KEY_LEFT_SHIFT
					)
				) 10 else 1)
						* (if(InputUtil.isKeyPressed(
						MinecraftClient.getInstance().window.handle, InputUtil.GLFW_KEY_LEFT_CONTROL
					)
				) 100 else 1)
			)
			ClientPlayNetworking.send(ADD_STAT_ID, buf)
		}
		add(subButton, 6, 0)

		val addButton = WButton(Text.of("+"))
		addButton.onClick = Runnable {
			val buf = PacketByteBufs.create()
			buf.writeVarInt(stat.ordinal)
			buf.writeVarInt(
				1 * (if(InputUtil.isKeyPressed(
						MinecraftClient.getInstance().window.handle, InputUtil.GLFW_KEY_LEFT_SHIFT
					)
				) 10 else 1)
						* (if(InputUtil.isKeyPressed(
						MinecraftClient.getInstance().window.handle, InputUtil.GLFW_KEY_LEFT_CONTROL
					)
				) 100 else 1)
			)
			ClientPlayNetworking.send(ADD_STAT_ID, buf)
		}
		add(addButton, width - 24, 0)
	}
}

class WColoredBar(val progress: () -> Float, direction: WBar.Direction) : WBar(null as Texture?, null, 0, 0, direction) {
	var color = 0xFFFFFF
	var backgroundColor = 0x000000
	var opacity = 1.0f
	var backgroundOpacity = 1.0f

	@Environment(EnvType.CLIENT)
	override fun paint(context: DrawContext?, x: Int, y: Int, mouseX: Int, mouseY: Int) {
		ScreenDrawing.coloredRect(
			context,
			x,
			y,
			getWidth(),
			getHeight(),
			ScreenDrawing.colorAtOpacity(backgroundColor, backgroundOpacity)
		)
		var percent = progress()
		if (percent < 0) percent = 0f
		if (percent > 1) percent = 1f
		var barMax = getWidth()
		if (direction == Direction.DOWN || direction == Direction.UP) barMax = getHeight()
		percent = (percent * barMax).toInt() / barMax.toFloat() //Quantize to bar size
		val barSize = (barMax * percent).toInt()
		if (barSize <= 0) return
		when (direction) {
			Direction.UP -> {
				var top = y + getHeight()
				top -= barSize
				ScreenDrawing.coloredRect(
					context,
					x,
					top,
					getWidth(),
					barSize,
					ScreenDrawing.colorAtOpacity(color, opacity)
				)
			}

			Direction.RIGHT -> ScreenDrawing.coloredRect(
				context,
				x,
				y,
				barSize,
				getHeight(),
				ScreenDrawing.colorAtOpacity(color, opacity)
			)

			Direction.DOWN -> ScreenDrawing.coloredRect(
				context,
				x,
				y,
				getWidth(),
				barSize,
				ScreenDrawing.colorAtOpacity(color, opacity)
			)

			Direction.LEFT -> {
				var left = x + getWidth()
				left -= barSize
				ScreenDrawing.coloredRect(
					context,
					left,
					y,
					barSize,
					getHeight(),
					ScreenDrawing.colorAtOpacity(color, opacity)
				)
			}
		}
	}

}
