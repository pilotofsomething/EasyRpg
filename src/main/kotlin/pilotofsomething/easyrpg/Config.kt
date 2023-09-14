package pilotofsomething.easyrpg

import draylar.omegaconfig.api.Comment
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

	var damageTypeScaling = defaultDamageScaling()

	@Comment(""" The damage formula.
 The following variables are available:
     attack - The base damage of the attack
     power - The power of the attack based on the attacker's strength, dexterity and intelligence stats
     defense - The defender's defense stat multiplied by the attack's defense modifier""")
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

	class EntitiesOptions {
		@Comment(""" The list of formulas for each dimension that control the level of mobs
 The following variables are available:
     distance - The distance from the origin of the world
     time - The number of ticks the player has been online for
     level - The level of the player
 time and level are weighted based on distance if multiple players are nearby when the mob spawns""")
		var levelFormula = defaultLevelFormula()

		var maxLevel: Int = 999

		var spGain: Double = 6.0

		var damageScalingRatio = 0.5

		var healScalingRatio = 0.5

		@Syncing
		var experience = ExpOptions()

		var damage = StatOptionsNoSp(1.0, 0.2, 0.0)

		var damageScaling = DamageScalingOptions(0.925, 0.25)

		var toughness = StatOptionsNoSp(1.0, 1.8, 0.0)

		var health = StatOptions(20, 4.0, 2.0, 0.0, 0.0)

		var strength = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		var dexterity = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		var intelligence = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		var defense = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		class ExpOptions {
			@Syncing
			@Comment(""" The formula used to calculate the base exp value of a mob.
 The following variables are available:
     hasValue - true if the mob has a value defined in 'mobValues'
     value - The value from 'mobValues' or 1
     health - The base max health of the mob
     attack - The attack damage of the mob, including equipment
     armor - The armor of the mob, including equipment
     level - The level of the mob
     vexp - The amount of vanilla exp dropped by the mob""")
			var expFormula = "IF(hasValue, value, (0.25 * health / 20) + (0.75 * attack / 3) + (0.5 * armor / 10)) * 125 * level^1.2"

			@Syncing
			@Comment(""" The formula used to scale the exp value of a mob
 The following variables are available:
     plevel - The level of the player
	 elevel - The level of the mob""")
			var scalingFormula = "MIN(MAX(1 + (elevel - plevel) * 0.1, 0.1), 3)"

			@Syncing
			@Comment(" The maximum amount of exp a mob can give")
			var expCap = Long.MAX_VALUE

			@Syncing
			var mobValues = hashMapOf(
				"minecraft:ender_dragon" to 50.0,
				"minecraft:wither" to 80.0,
				"minecraft:creeper" to 1.2
			)
		}
	}

	class PlayerOptions {
		@Syncing
		var maxLevel: Int = 999

		var spGain: Double = 6.0

		var damageScalingRatio = 0.5

		var healScalingRatio = 0.5

		@Syncing
		var experience = ExperienceOptions()

		var damage = StatOptionsNoSp(1.0, 0.2, 0.0)

		var damageScaling = DamageScalingOptions(0.925, 0.25)

		var toughness = StatOptionsNoSp(1.0, 1.8, 0.0)

		var health = StatOptions(20, 4.0, 2.0, 0.0, 0.0)

		var strength = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		var dexterity = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		var intelligence = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		var defense = StatOptions(10, 2.0, 1.0, 0.0, 0.0)

		class ExperienceOptions {
			@Syncing
			@Comment(""" The formula for the exp curve
 The only variable is level, the player's level""")
			var expCurve = "1000 * level^2.4"

			@Syncing
			var levelOffset = 0

			@Syncing
			@Comment(""" If true then instead of killed mobs giving exp, picking up xp orbs will give exp""")
			var useVanillaExp = false
		}
	}

	class StatOptions(var base: Int, var gain: Double, var spGain: Double, var multGain: Double, var multiSpGain: Double)

	class StatOptionsNoSp(var base: Double, var gain: Double, var multGain: Double)

	class DamageScalingOptions(var amount: Double, var limit: Double)

	override fun getName(): String {
		return "easyrpg"
	}

	override fun getExtension(): String {
		return "json5"
	}
}

class LevelFormula(var formula: String, var minY: Int = -2048, var maxY: Int = 2048)

private fun defaultLevelFormula(): HashMap<String, ArrayList<LevelFormula>> {
	return hashMapOf(
		"minecraft:overworld" to arrayListOf(
			LevelFormula("MAX(0.5 * level + 0.5 * distance / 120, 1)", minY = 0),
			LevelFormula("MAX((0.5 * level) + (0.5 * distance / 120) + 25, 25)", maxY = 0)
		),
		"minecraft:the_nether" to arrayListOf(LevelFormula("MAX(0.5 * level + 0.5 * distance / 15, 1)")),
		"minecraft:the_end" to arrayListOf(LevelFormula("MAX((0.5 * level) + (0.5 * distance / 120) + 75, 75)")),
		"default" to arrayListOf(LevelFormula("MAX(0.5 * level + 0.5 * distance / 120, 1)"))
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
