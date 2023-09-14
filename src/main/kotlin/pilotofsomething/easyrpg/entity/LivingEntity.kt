package pilotofsomething.easyrpg.entity

import com.ezylang.evalex.EvaluationException
import com.ezylang.evalex.Expression
import com.ezylang.evalex.parser.ParseException
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import pilotofsomething.easyrpg.EVALEX_CONFIG
import pilotofsomething.easyrpg.EasyRpgAttributes.DEFENSE
import pilotofsomething.easyrpg.EasyRpgAttributes.DEXTERITY
import pilotofsomething.easyrpg.EasyRpgAttributes.INTELLIGENCE
import pilotofsomething.easyrpg.EasyRpgAttributes.STRENGTH
import pilotofsomething.easyrpg.components.RPG_MOB
import pilotofsomething.easyrpg.components.RPG_PLAYER
import pilotofsomething.easyrpg.config
import kotlin.math.pow

fun getDamagePowScaling(type: String): HashMap<String, Double> {
	if(!config.damageTypeScaling.containsKey(type)) println("Unknown damage type: $type, using defaults.")
	return config.damageTypeScaling[type] ?: config.damageTypeScaling["default"]!!
}

fun calculateDamage(entity: LivingEntity, amount: Float, source: DamageSource): Float {
	val attacker = source.attacker
	if (attacker is LivingEntity) {
		val rpg = if (attacker is PlayerEntity) {
			RPG_PLAYER[attacker]
		} else RPG_MOB[attacker]
		val thisRpg = if (entity is PlayerEntity) {
			RPG_PLAYER[entity]
		} else RPG_MOB[entity]

		var damage = amount

		if (attacker is PlayerEntity) {
			damage *= (config.players.damage.base + config.players.damage.gain * (rpg.level - 1)).toFloat()
			damage *= (1 + config.players.damage.multGain * (rpg.level - 1)).toFloat()
			damage *= config.players.damageScaling.amount.pow((thisRpg.level - rpg.level)
				.coerceAtLeast(0).toDouble())
				.coerceAtLeast(config.players.damageScaling.limit).toFloat()
		} else {
			damage *= (config.entities.damage.base + config.entities.damage.gain * (rpg.level - 1)).toFloat()
			damage *= (1 + config.entities.damage.multGain * (rpg.level - 1)).toFloat()
			damage *= config.entities.damageScaling.amount.pow((thisRpg.level - rpg.level)
				.coerceAtLeast(0).toDouble())
				.coerceAtLeast(config.entities.damageScaling.limit).toFloat()
		}

		val powScaling: Map<String, Double> = getDamagePowScaling(source.typeRegistryEntry.key.orElseThrow().value.toString())
		val strength: Double = attacker.getAttributeValue(STRENGTH)
		val dexterity: Double = attacker.getAttributeValue(DEXTERITY)
		val intelligence: Double = attacker.getAttributeValue(INTELLIGENCE)
		val defense: Double = entity.getAttributeValue(DEFENSE)

		val power = (strength * powScaling["strength"]!! + dexterity * powScaling["dexterity"]!! + intelligence * powScaling["intelligence"]!!)
			.coerceAtLeast(1.0)
		val def = (defense * powScaling["defense"]!!).coerceAtLeast(1.0)

		return try {
			Expression(config.damageFormula, EVALEX_CONFIG)
				.with("attack", damage)
				.and("power", power)
				.and("defense", def).evaluate().numberValue.toFloat()
				.coerceAtMost(config.statCaps.damageCap.toFloat())
		} catch (e: EvaluationException) {
			throw RuntimeException(e)
		} catch (e: ParseException) {
			throw RuntimeException(e)
		}
	}

	val healthScaling = (1 + (entity.maxHealth - entity.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH))
			* (if (entity is PlayerEntity) config.players.damageScalingRatio else config.entities.damageScalingRatio)
			/ entity.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH)).coerceAtLeast(1.0).toFloat()
	return amount * healthScaling
}

fun calculateHealing(entity: LivingEntity, amount: Float): Float {
	val healthScaling = (1 + (entity.maxHealth - entity.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH))
			* (if (entity is PlayerEntity) config.players.healScalingRatio else config.entities.healScalingRatio)
			/ entity.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH)).coerceAtLeast(1.0).toFloat()
	return amount * healthScaling
}
