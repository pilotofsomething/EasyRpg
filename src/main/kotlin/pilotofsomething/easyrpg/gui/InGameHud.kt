package pilotofsomething.easyrpg.gui

import com.mojang.blaze3d.systems.RenderSystem
import io.github.cottonmc.cotton.gui.client.ScreenDrawing.colorAtOpacity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Util
import net.minecraft.util.math.MathHelper
import pilotofsomething.easyrpg.components.RPG_PLAYER
import pilotofsomething.easyrpg.config
import kotlin.math.max
import kotlin.math.min

object CustomInGameHud : DrawableHelper() {

	private val healthTracker = Tracker()
	private val mountHealthTracker = Tracker()

	private fun renderHealthBar(
		matrices: MatrixStack,
		x: Int,
		y: Int,
		maxHealth: Float,
		health: Float,
		absorption: Float
	) {
		healthTracker.update(health, maxHealth)

		val percent1 = min(health / maxHealth, 1.0f)
		val percent2 = min(healthTracker.prevHealthDisplay / maxHealth, 1.0f)

		fill(matrices, x, y, x + 110, y + 9, colorAtOpacity(0x1F0000, 1.0f))
		fill(matrices, x, y, (x + (110 * percent2)).toInt(), y + 9, colorAtOpacity(0xFF4F4F, 1.0f))
		fill(matrices, x, y, (x + (110 * percent1)).toInt(), y + 9, colorAtOpacity(0xDF0000, 1.0f))

		val tr = MinecraftClient.getInstance().textRenderer
		if(absorption > 0) {
			tr.draw(matrices, String.format("%,.0f (+%,.0f)", health, absorption), x + 1f, y + 1f, 0xFFFFFF)
		} else tr.draw(matrices, String.format("%,.0f", health), x + 1f, y + 1f, 0xFFFFFF)
	}


	fun renderStatusBars(matrices: MatrixStack, player: PlayerEntity, scaledWidth: Int, scaledHeight: Int) {
		renderHealthBar(
			matrices, scaledWidth / 2 - 91, scaledHeight - 39, player.maxHealth, player.health, player.absorptionAmount
		)

		matrices.push()

		val textRenderer = MinecraftClient.getInstance().textRenderer

		val hunger = player.hungerManager
		val hungerX = scaledWidth / 2 + 20
		val hungerY = scaledHeight - 39
		val hungerPercent = 1 - hunger.foodLevel / 20f

		fill(matrices, hungerX, hungerY, hungerX + 71, hungerY + 9, colorAtOpacity(0x1F0F00, 1.0f))
		val foodColor =
			if(hunger.saturationLevel > 0) colorAtOpacity(0x6F2F00, 1.0F) else colorAtOpacity(0x5F1F00, 1.0f)
		fill(matrices, hungerX + (71 * hungerPercent).toInt(), hungerY, hungerX + 71, hungerY + 9, foodColor)
		textRenderer.draw(
			matrices, String.format("%d", hunger.foodLevel),
			hungerX + 71f - textRenderer.getWidth(String.format("%d", hunger.foodLevel)), hungerY + 1f, 0xFFFFFF
		)

		val enchExpX = scaledWidth / 2 + 1
		val expY = hungerY + 10

		val plrExpX = scaledWidth / 2 - 91

		fill(matrices, enchExpX, expY, enchExpX + 90, expY + 5, colorAtOpacity(0x101F04, 1.0f))
		fill(
			matrices, enchExpX + ((1 - player.experienceProgress) * 90).toInt(), expY, enchExpX + 90, expY + 5,
			colorAtOpacity(0x608F18, 1.0f)
		)
		if(player.experienceLevel > 0) {
			textRenderer.draw(
				matrices, player.experienceLevel.toString(),
				enchExpX + 90f - textRenderer.getWidth(player.experienceLevel.toString()), expY - 1f, 0xFFFFFF
			)
		}

		val rpg = RPG_PLAYER.get(player)
		val expPercent = if(rpg.level < config.players.maxLevel) rpg.xpForLevel / rpg.xpReqForLevel.toDouble() else 1.0

		fill(matrices, plrExpX, expY, plrExpX + 90, expY + 5, colorAtOpacity(0x161F00, 1.0f))
		fill(matrices, plrExpX, expY, plrExpX + (90 * expPercent).toInt(), expY + 5, colorAtOpacity(0xB6FF00, 1.0f))

		val mounted = player.vehicle
		if(mounted is LivingEntity) {
			mountHealthTracker.update(mounted.health, mounted.maxHealth)

			val mountX = scaledWidth / 2 - 91
			val mountY = hungerY - 10

			val percent1 = mounted.health / mounted.maxHealth
			val percent2 = mountHealthTracker.prevHealthDisplay / mounted.maxHealth

			fill(matrices, mountX, mountY, mountX + 182, mountY + 9, colorAtOpacity(0x1F0000, 1.0f))
			fill(
				matrices, mountX, mountY, mountX + (182 * percent2).toInt(), mountY + 9, colorAtOpacity(0xFF4F4F, 1.0f)
			)
			fill(
				matrices, mountX, mountY, mountX + (182 * percent1).toInt(), mountY + 9, colorAtOpacity(0xDF0000, 1.0f)
			)
			textRenderer.draw(matrices, String.format("%,.0f", mounted.health), mountX + 1f, mountY + 1f, 0xFFFFFF)

			matrices.translate(0.0, -10.0, 0.0)

			if(MinecraftClient.getInstance().player?.hasJumpingMount() == true) {
				val jumpY = hungerY - 4
				val jumpX = scaledWidth / 2 - 91

				val percent = MinecraftClient.getInstance().player?.mountJumpStrength ?: 0f
				val bar1 = min(percent * 182, 161f)
				val bar2 = max((percent * 182) - bar1, 0f)

				fill(matrices, jumpX, jumpY, jumpX + 161, jumpY + 3, colorAtOpacity(0x2E2337, 1.0f))
				fill(matrices, jumpX + 161, jumpY, jumpX + 182, jumpY + 3, colorAtOpacity(0x2C351F, 1.0f))
				fill(matrices, jumpX, jumpY, jumpX + bar1.toInt(), jumpY + 3, colorAtOpacity(0x678DD9, 1.0f))
				fill(
					matrices, jumpX + 161, jumpY, jumpX + 161 + bar2.toInt(), jumpY + 3, colorAtOpacity(0xD09248, 1.0f)
				)

				matrices.translate(0.0, -4.0, 0.0)
			}
		} else {
			mountHealthTracker.update(0f, 0f)
		}

		if(player.air < player.maxAir) {
			val breathY = hungerY - 4
			val breathX = scaledWidth / 2 - 91
			val percent = max(player.air, 0) / player.maxAir.toFloat()
			fill(matrices, breathX, breathY, breathX + 182, breathY + 3, colorAtOpacity(0x00001F, 1.0f))
			fill(
				matrices, breathX, breathY, breathX + (182 * percent).toInt(), breathY + 3,
				colorAtOpacity(0x0000DF, 1.0f)
			)
			matrices.translate(0.0, -4.0, 0.0)
		}

		if(player.armor > 0) {
			RenderSystem.setShaderTexture(0, GUI_ICONS_TEXTURE)
			val armorY = hungerY - 10
			val armorX = scaledWidth / 2 - 91
			drawTexture(matrices, armorX, armorY, 34, 9, 9, 9)
			textRenderer.drawWithShadow(matrices, "${player.armor}", armorX + 11f, armorY + 1f, 0xFFFFFF)
		}

		matrices.pop()
	}

	/**
	 * Most of the code in this class was taken from ToroHealth Damage Indicators
	 * https://github.com/ToroCraft/ToroHealth
	 */
	private class Tracker {
		private var lastDmgDelay = 0f
		private var prevHealthDelay = 0f

		private var prevHealth = 0f
		var prevHealthDisplay = 0f

		private var lastDmg = 0
		private var lastDmgCumu = 0

		private var lastHealth = 0f
		private var animSpeed = 0f

		private val delay = 25f

		private var time = 0L

		fun update(value: Float, max: Float) {
			if(Util.getMeasuringTimeNano() - time > 5000000) { // Cap animation frame rate at 200 fps
				if(prevHealthDisplay > max) {
					resetHealth(value)
					prevHealth = value
					prevHealthDisplay = value
				}
				val delta = ((Util.getMeasuringTimeNano() - time) / 20000000.0).toFloat()
				incrTimers(delta)
				when {
					lastHealth < 0.1 || lastDmgDelay == 0f -> resetHealth(value)
					lastHealth != value -> handleHealthChange(value)
				}
				updateAnim(value, delta)
				time = Util.getMeasuringTimeNano()
			}
		}

		private fun incrTimers(delta: Float) {
			if(lastDmgDelay > 0) {
				lastDmgDelay -= 1 * delta
			}
			if(prevHealthDelay > 0) {
				prevHealthDelay -= 1 * delta
			}
		}

		private fun resetHealth(health: Float) {
			lastHealth = health
			lastDmg = 0
			lastDmgCumu = 0
		}

		private fun handleHealthChange(health: Float) {
			lastDmg = MathHelper.ceil(lastHealth) - MathHelper.ceil(health)
			lastDmgCumu += lastDmg
			lastDmgDelay = delay * 2
			lastHealth = health
		}

		private fun updateAnim(health: Float, delta: Float) {
			if(prevHealthDelay > 0) {
				val diff = prevHealthDisplay - health
				if(diff > 0) {
					animSpeed = diff / 25f
				}
			} else if(prevHealthDelay < 1 && prevHealthDisplay > health) {
				prevHealthDisplay = max(prevHealthDisplay - (animSpeed * delta), health)
			} else {
				prevHealthDisplay = health
				prevHealth = health
				prevHealthDelay = delay
			}
		}
	}

}