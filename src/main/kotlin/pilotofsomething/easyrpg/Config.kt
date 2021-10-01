package pilotofsomething.easyrpg

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

lateinit var config: ModConfig

@Config(name = "easyrpg")
class ModConfig : ConfigData {

	@ConfigEntry.Category("Client")
	@ConfigEntry.Gui.TransitiveObject
	var client = ClientOptions()

	@ConfigEntry.Category("Players")
	@ConfigEntry.Gui.TransitiveObject
	var players = PlayerOptions()

	@ConfigEntry.Category("Entities")
	@ConfigEntry.Gui.TransitiveObject
	var entities = EntitiesOptions()

	class ClientOptions {
		var renderCustomHud = true
		var renderVanillaDamageParticle = false
	}

	class EntitiesOptions {
		@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
		var scaleMode = ScalingMode.LEVEL_DISTANCE

		@ConfigEntry.Gui.CollapsibleObject
		var scaleModeSettings = ScaleModeSettings()

		var maxLevel: Int = 999

		var spGain: Double = 6.0

		@ConfigEntry.Gui.Tooltip
		var damageScalingRatio = 0.5

		@ConfigEntry.Gui.Tooltip
		var healScalingRatio = 0.5

		@ConfigEntry.Gui.CollapsibleObject
		var expOptions = ExpOptions()

		@ConfigEntry.Gui.CollapsibleObject
		var damage = StatOptionsNoSp(1.0, 0.2, 1000.0)

		@ConfigEntry.Gui.CollapsibleObject
		var toughness = StatOptionsNoSp(1.0, 1.8, 2000.0)

		@ConfigEntry.Gui.CollapsibleObject
		var healthOptions = StatOptions(20, 4.0, 2.0, 999999)

		@ConfigEntry.Gui.CollapsibleObject
		var strengthOptions = StatOptions(10, 2.0, 1.0, 99999)

		@ConfigEntry.Gui.CollapsibleObject
		var dexterityOptions = StatOptions(10, 2.0, 1.0, 99999)

		@ConfigEntry.Gui.CollapsibleObject
		var intelligenceOptions = StatOptions(10, 2.0, 1.0, 99999)

		@ConfigEntry.Gui.CollapsibleObject
		var defenseOptions = StatOptions(10, 2.0, 1.0, 99999)

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
				var scalingMax = 3.0
				var scalingMin = 0.1
			}
		}

		class ScaleModeSettings {
			@ConfigEntry.Gui.Tooltip
			var distanceDivisor = 120.0

			@ConfigEntry.Gui.Tooltip
			var levelRatio = 0.5

			@ConfigEntry.Gui.CollapsibleObject
			var timeSettings = TimeSettings()

			@ConfigEntry.Gui.Excluded
			var dimensionSettings = hashMapOf(
				"minecraft:overworld" to arrayListOf(ScaleSettingOperation(ScaleSettingOperation.Operation.MINIMUM, 1.0)),
				"minecraft:the_nether" to arrayListOf(ScaleSettingOperation(ScaleSettingOperation.Operation.MULTIPLY_DISTANCE, 8.0), ScaleSettingOperation(ScaleSettingOperation.Operation.MINIMUM, 1.0)),
				"minecraft:the_end" to arrayListOf(ScaleSettingOperation(ScaleSettingOperation.Operation.ADD, 75.0), ScaleSettingOperation(ScaleSettingOperation.Operation.MINIMUM, 75.0))
			)

			class TimeSettings {
				@ConfigEntry.Gui.Tooltip
				var linear = 120000L
				@ConfigEntry.Gui.Tooltip
				var multiplier = 480000L
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
		var damage = StatOptionsNoSp(1.0, 0.2, 1000.0)

		@ConfigEntry.Gui.CollapsibleObject
		var toughness = StatOptionsNoSp(1.0, 1.8, 2000.0)

		@ConfigEntry.Gui.CollapsibleObject
		var healthOptions = StatOptions(20, 4.0, 2.0, 999999)

		@ConfigEntry.Gui.CollapsibleObject
		var strengthOptions = StatOptions(10, 2.0, 1.0, 99999)

		@ConfigEntry.Gui.CollapsibleObject
		var dexterityOptions = StatOptions(10, 2.0, 1.0, 99999)

		@ConfigEntry.Gui.CollapsibleObject
		var intelligenceOptions = StatOptions(10, 2.0, 1.0, 99999)

		@ConfigEntry.Gui.CollapsibleObject
		var defenseOptions = StatOptions(10, 2.0, 1.0, 99999)

		class ExperienceOptions {
			var base = 1000.0
			var exponent = 2.4
		}
	}

	class StatOptions(var base: Int, var gain: Double, var spGain: Double, var cap: Int)
	class StatOptionsNoSp(var base: Double, var gain: Double, var cap: Double)
}

class ScaleSettingOperation(var operation: Operation, var value: Double) {
	constructor() : this(Operation.ADD, 0.0)
	enum class Operation {
		ADD,
		MULTIPLY_DISTANCE,
		MULTIPLY_TOTAL,
		MINIMUM,
		MAXIMUM
	}
}

class ConfigScreen : ModMenuApi {
	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
		return ConfigScreenFactory { parent -> AutoConfig.getConfigScreen(ModConfig::class.java, parent).get() }
	}
}
