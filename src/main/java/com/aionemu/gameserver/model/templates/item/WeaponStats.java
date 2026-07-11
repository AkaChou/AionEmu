package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Weapon 属性模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author ATracer
 */
public class WeaponStats {

	@XmlAttribute(name = "min_damage")
	protected int minDamage;
	@XmlAttribute(name = "max_damage")
	protected int maxDamage;
	@XmlAttribute(name = "attack_speed")
	protected int attackSpeed;
	@XmlAttribute(name = "physical_critical")
	protected int physicalCritical;
	@XmlAttribute(name = "physical_accuracy")
	protected int physicalAccuracy;
	@XmlAttribute
	protected int parry;
	@XmlAttribute(name = "magical_accuracy")
	protected int magicalAccuracy;
	@XmlAttribute(name = "boost_magical_skill")
	protected int boostMagicalSkill;
	@XmlAttribute(name = "attack_range")
	protected int attackRange;
	@XmlAttribute(name = "hit_count")
	protected int hitCount;
	@XmlAttribute(name = "reduce_max")
	protected int reduceMax;

	/** 返回最小伤害 / Returns the min damage*/
	public final int getMinDamage() {
		return minDamage;
	}

	/** 返回最大伤害 / Returns the max damage*/
	public final int getMaxDamage() {
		return maxDamage;
	}

	/** 返回 mean damage / Returns the mean damage */
	public final int getMeanDamage() {
		return (minDamage + maxDamage) / 2;
	}

	/** 返回 attack speed / Returns the attack speed */
	public final int getAttackSpeed() {
		return attackSpeed;
	}

	/** 返回 physical critical / Returns the physical critical */
	public final int getPhysicalCritical() {
		return physicalCritical;
	}

	/** 返回 physical accuracy / Returns the physical accuracy */
	public final int getPhysicalAccuracy() {
		return physicalAccuracy;
	}

	/** 返回 parry / Returns the parry */
	public final int getParry() {
		return parry;
	}

	/** 返回 magical accuracy / Returns the magical accuracy */
	public final int getMagicalAccuracy() {
		return magicalAccuracy;
	}

	/** 返回加速魔法技能 / Returns the boost magical skill*/
	public final int getBoostMagicalSkill() {
		return boostMagicalSkill;
	}

	/** 返回攻击范围 / Returns the attack range*/
	public final int getAttackRange() {
		return attackRange;
	}

	/** 返回 hit count / Returns the hit count */
	public final int getHitCount() {
		return hitCount;
	}

	/** 返回 reduce max / Returns the reduce max */
	public final int getReduceMax() {
		return reduceMax;
	}
}
