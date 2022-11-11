package pilotofsomething.easyrpg

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen
import pilotofsomething.easyrpg.item.setupQualities

lateinit var config: ModConfig

val serverConfig = ModConfig()

@Config(name = "easyrpg")
class ModConfig : ConfigData {

	@ConfigEntry.Category("Client")
	@ConfigEntry.Gui.TransitiveObject
	var client = ClientOptions()

	@ConfigEntry.Category("StatCaps")
	@ConfigEntry.Gui.TransitiveObject
	var statCaps = StatCapOptions()

	@ConfigEntry.Category("Players")
	@ConfigEntry.Gui.TransitiveObject
	var players = PlayerOptions()

	@ConfigEntry.Category("Entities")
	@ConfigEntry.Gui.TransitiveObject
	var entities = EntitiesOptions()

	@ConfigEntry.Category("Items")
	@ConfigEntry.Gui.TransitiveObject
	var items = ItemOptions()

	class ClientOptions {
		var renderCustomHud = true
		var renderVanillaDamageParticle = false
	}

	class StatCapOptions {
		var hpCap = 99999
		var strCap = 99999
		var dexCap = 99999
		var intCap = 99999
		var defCap = 99999
		var armorToughnessCap = 99999
		var damageCap = 9999
	}

	class EntitiesOptions {
		@ConfigEntry.Gui.Excluded
		var levelFormula = defaultLevelFormula()

		var maxLevel: Int = 999

		var spGain: Double = 6.0

		@ConfigEntry.Gui.Tooltip
		var damageScalingRatio = 0.5

		@ConfigEntry.Gui.Tooltip
		var healScalingRatio = 0.5

		@ConfigEntry.Gui.CollapsibleObject
		var expOptions = ExpOptions()

		@ConfigEntry.Gui.CollapsibleObject
		var damage = StatOptionsNoSp(1.0, 0.2, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		@ConfigEntry.Gui.Tooltip
		var damageScaling = DamageScalingOptions(0.925, 0.25)

		@ConfigEntry.Gui.CollapsibleObject
		var toughness = StatOptionsNoSp(1.0, 1.8, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var healthOptions = StatOptions(20, 4.0, 2.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var strengthOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var dexterityOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var intelligenceOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var defenseOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		class ExpOptions {

			var baseValue = 125L

			var exponent = 1.2

			@ConfigEntry.Gui.CollapsibleObject
			var mobModifiers = MobValueOptions()

			@ConfigEntry.Gui.CollapsibleObject
			var scalingSettings = ScalingSettings()

			class MobValueOptions {
				@ConfigEntry.Gui.CollapsibleObject
				var health = ValueOption(20.0, 0.25)

				@ConfigEntry.Gui.CollapsibleObject
				var attack = ValueOption(3.0, 0.75)

				@ConfigEntry.Gui.CollapsibleObject
				var armor = ValueOption(10.0, 0.5)

				@ConfigEntry.Gui.Excluded
				var mobValueOverrides = hashMapOf(
					"minecraft:ender_dragon" to 50.0,
					"minecraft:wither" to 80.0,
					"minecraft:creeper" to 1.2
				)

				class ValueOption(var base: Double, var value: Double)
			}

			class ScalingSettings {
				var scalingAmount = 0.1
				var exponentialIncreaseAmount = 1.0
				var exponentialDecreaseAmount = 1.0
				var scalingMax = 3.0
				var scalingMin = 0.1
				var expCap = Long.MAX_VALUE

				var scaleVanillaExp = false
			}
		}
	}

	class PlayerOptions {
		var maxLevel: Int = 999

		var spGain: Double = 6.0

		@ConfigEntry.Gui.Tooltip
		var damageScalingRatio = 0.5

		@ConfigEntry.Gui.Tooltip
		var healScalingRatio = 0.5

		@ConfigEntry.Gui.CollapsibleObject
		var experience = ExperienceOptions()

		@ConfigEntry.Gui.CollapsibleObject
		var damage = StatOptionsNoSp(1.0, 0.2, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		@ConfigEntry.Gui.Tooltip
		var damageScaling = DamageScalingOptions(0.925, 0.25)

		@ConfigEntry.Gui.CollapsibleObject
		var toughness = StatOptionsNoSp(1.0, 1.8, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var healthOptions = StatOptions(20, 4.0, 2.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var strengthOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var dexterityOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var intelligenceOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@ConfigEntry.Gui.CollapsibleObject
		var defenseOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		class ExperienceOptions {
			var base = 1000.0
			var exponent = 2.4

			@ConfigEntry.Gui.Tooltip
			var levelOffset = 0

			@ConfigEntry.Gui.Tooltip
			var advancedExpCurve = ""

			@ConfigEntry.Gui.Tooltip
			var useVanillaExp = false
		}
	}

	class ItemOptions {
		var enabled: Boolean = false
		var crafted: Boolean = false

		var craftedLevelMult: Double = 0.5

		@ConfigEntry.Gui.Excluded
		var levelFormula = defaultLevelFormula()

		var maxLevel: Int = 999

		var spGain: Double = 1.0

		var healthGain: Double = 2.0
		var strengthGain: Double = 1.0
		var dexterityGain: Double = 1.0
		var intelligenceGain: Double = 1.0
		var defenseGain: Double = 1.0

		@ConfigEntry.Gui.Excluded
		var rerollers = hashMapOf(
			"easy_rpg:iron_reroller" to RerollSettings(0.6, 0.0f),
			"easy_rpg:gold_reroller" to RerollSettings(0.8, 2.0f),
			"easy_rpg:diamond_reroller" to RerollSettings(1.0, 4.0f),
			"easy_rpg:netherite_reroller" to RerollSettings(1.0, 12.0f)
		)

		@ConfigEntry.Gui.CollapsibleObject
		var rarities = Rarities()

		class Rarities {
			@ConfigEntry.Gui.CollapsibleObject
			var common = RaritySettings(600, 1.0, 1)

			@ConfigEntry.Gui.CollapsibleObject
			var uncommon = RaritySettings(280, 1.5, 2)

			@ConfigEntry.Gui.CollapsibleObject
			var rare = RaritySettings(100, 2.0, 3)

			@ConfigEntry.Gui.CollapsibleObject
			var epic = RaritySettings(20, 2.5, 4)
		}

		class RaritySettings(var weight: Int, var spMultiplier: Double, var maxStatCount: Int)
		class RerollSettings(var levelMult: Double, var luck: Float)
	}

	class StatOptions(var base: Int, var gain: Double, var spGain: Double, var multGain: Double, var multiSpGain: Double)
	class StatOptionsNoSp(var base: Double, var gain: Double, var multGain: Double)
	class DamageScalingOptions(var amount: Double, var limit: Double)
}

private fun defaultLevelFormula(): HashMap<String, String> {
	return hashMapOf(
		"minecraft:overworld" to "MAX(0.5 * level + 0.5 * distance / 120, 1)",
		"minecraft:the_nether" to "MAX(0.5 * level + 0.5 * distance / 15, 1)",
		"minecraft:the_end" to "MAX((0.5 * level) + (0.5 * distance / 120) + 75, 75)",
		"default" to "MAX(0.5 * level + 0.5 * distance / 120, 1)"
	)
}

class ConfigScreen : ModMenuApi {
	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
		return ConfigScreenFactory { parent ->
			val screen = AutoConfig.getConfigScreen(ModConfig::class.java, parent).get()
			(screen as AbstractConfigScreen).setSavingRunnable { AutoConfig.getConfigHolder(ModConfig::class.java).save(); setupQualities() }
			screen
		}
	}
}
