package pilotofsomething.easyrpg

import draylar.omegaconfig.api.Config
import draylar.omegaconfig.api.Syncing

lateinit var config: ModConfig

class ModConfig : Config {

	var client = ClientOptions()

	var statCaps = StatCapOptions()

	@Syncing
	var players = PlayerOptions()

	@Syncing
	var entities = EntitiesOptions()

	@Syncing
	var damageTypeScaling = defaultDamageScaling()

	@Syncing
	var damageFormula = "attack * power / defense"

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

	@Syncing
	class EntitiesOptions {
		@Syncing
		var levelFormula = defaultLevelFormula()

		@Syncing
		var maxLevel: Int = 999

		@Syncing
		var spGain: Double = 6.0

		@Syncing
		var damageScalingRatio = 0.5

		var healScalingRatio = 0.5

		@Syncing
		var expOptions = ExpOptions()

		@Syncing
		var damage = StatOptionsNoSp(1.0, 0.2, 0.0)

		@Syncing
		var damageScaling = DamageScalingOptions(0.925, 0.25)

		@Syncing
		var toughness = StatOptionsNoSp(1.0, 1.8, 0.0)

		@Syncing
		var healthOptions = StatOptions(20, 4.0, 2.0, 0.0, 0.0)

		@Syncing
		var strengthOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		var dexterityOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		var intelligenceOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		var defenseOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		class ExpOptions {
			@Syncing
			var baseValue = 125L

			@Syncing
			var exponent = 1.2

			@Syncing
			var mobModifiers = MobValueOptions()

			@Syncing
			var scalingSettings = ScalingSettings()

			class MobValueOptions {

				@Syncing
				var useVanillaExpValue = false

				@Syncing
				var health = ValueOption(20.0, 0.25)

				@Syncing
				var attack = ValueOption(3.0, 0.75)

				@Syncing
				var armor = ValueOption(10.0, 0.5)

				@Syncing
				var mobValueOverrides = hashMapOf(
					"minecraft:ender_dragon" to 50.0,
					"minecraft:wither" to 80.0,
					"minecraft:creeper" to 1.2
				)

				@Syncing
				class ValueOption(var base: Double, var value: Double)
			}

			class ScalingSettings {
				@Syncing
				var scalingAmount = 0.1
				@Syncing
				var exponentialIncreaseAmount = 1.0
				@Syncing
				var exponentialDecreaseAmount = 1.0
				@Syncing
				var scalingMax = 3.0
				@Syncing
				var scalingMin = 0.1
				@Syncing
				var expCap = Long.MAX_VALUE
			}
		}
	}

	@Syncing
	class PlayerOptions {
		@Syncing
		var maxLevel: Int = 999

		@Syncing
		var spGain: Double = 6.0

		@Syncing
		var damageScalingRatio = 0.5

		@Syncing
		var healScalingRatio = 0.5

		@Syncing
		var experience = ExperienceOptions()

		@Syncing
		var damage = StatOptionsNoSp(1.0, 0.2, 0.0)

		@Syncing
		var damageScaling = DamageScalingOptions(0.925, 0.25)

		@Syncing
		var toughness = StatOptionsNoSp(1.0, 1.8, 0.0)

		@Syncing
		var healthOptions = StatOptions(20, 4.0, 2.0, 0.0, 0.0)

		@Syncing
		var strengthOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		var dexterityOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		var intelligenceOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		var defenseOptions = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		@Syncing
		class ExperienceOptions {
			@Syncing
			var base = 1000.0
			@Syncing
			var exponent = 2.4
			@Syncing
			var levelOffset = 0
			@Syncing
			var advancedExpCurve = ""
			@Syncing
			var useVanillaExp = false
		}
	}

	@Syncing
	class StatOptions(@Syncing var base: Int, @Syncing var gain: Double, @Syncing var spGain: Double, @Syncing var multGain: Double, @Syncing var multiSpGain: Double)
	@Syncing
	class StatOptionsNoSp(@Syncing var base: Double, @Syncing var gain: Double, @Syncing var multGain: Double)
	@Syncing
	class DamageScalingOptions(@Syncing var amount: Double, @Syncing var limit: Double)

	override fun getName(): String {
		return "easyrpg"
	}
}

private fun defaultLevelFormula(): HashMap<String, String> {
	return hashMapOf(
		"minecraft:overworld" to "MAX(0.5 * level + 0.5 * distance / 120, 1)",
		"minecraft:the_nether" to "MAX(0.5 * level + 0.5 * distance / 15, 1)",
		"minecraft:the_end" to "MAX((0.5 * level) + (0.5 * distance / 120) + 75, 75)",
		"default" to "MAX(0.5 * level + 0.5 * distance / 120, 1)"
	)
}

private fun defaultDamageScaling(): HashMap<String, HashMap<String, Double>> {
	return hashMapOf(
		"minecraft:generic" to hashMapOf("strength" to 0.33, "dexterity" to 0.33, "intelligence" to 0.33, "defense" to 1.0),
		"minecraft:indirect_magic" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:wither_skull" to hashMapOf("strength" to 0.0, "dexterity" to 0.8, "intelligence" to 0.2, "defense" to 1.0),
		"minecraft:cramming" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:trident" to hashMapOf("strength" to 0.4, "dexterity" to 0.6, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:sting" to hashMapOf("strength" to 0.1, "dexterity" to 0.9, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:thrown" to hashMapOf("strength" to 0.3, "dexterity" to 0.7, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:hot_floor" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:on_fire" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:lava" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:player_attack" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:sonic_boom" to hashMapOf("strength" to 0.0, "dexterity" to 0.7, "intelligence" to 0.3, "defense" to 0.33),
		"minecraft:mob_attack_no_aggro" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:fireball" to hashMapOf("strength" to 0.0, "dexterity" to 0.8, "intelligence" to 0.2, "defense" to 1.0),
		"minecraft:player_explosion" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:falling_stalactite" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:unattributed_fireball" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:stalagmite" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:in_fire" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:bad_respawn_point" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:generic_kill" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:magic" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:mob_attack" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:freeze" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:arrow" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:sweet_berry_bush" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:dragon_breath" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:fly_into_wall" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:cactus" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:out_of_world" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:starve" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:drown" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:fireworks" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:fall" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:falling_anvil" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:in_wall" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:dry_out" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:wither" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:lightning_bolt" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:explosion" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:falling_block" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:thorns" to hashMapOf("strength" to 0.0, "dexterity" to 0.0, "intelligence" to 1.0, "defense" to 1.0),
		"minecraft:mob_projectile" to hashMapOf("strength" to 0.0, "dexterity" to 1.0, "intelligence" to 0.0, "defense" to 1.0),
		"minecraft:outside_border" to hashMapOf("strength" to 1.0, "dexterity" to 0.0, "intelligence" to 0.0, "defense" to 1.0),
		"default" to hashMapOf("strength" to 0.33, "dexterity" to 0.33, "intelligence" to 0.33, "defense" to 1.0)
	)
}
