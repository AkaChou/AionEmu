package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import lombok.Getter;
import lombok.Setter;

/**
 * NPC 属性模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "npc_stats_template")
public class NpcStatsTemplate extends StatsTemplate {
	/** 返回 run speed fight / Returns the run speed fight */
	@Getter
	@XmlAttribute(name = "run_speed_fight")
	private float runSpeedFight;

	/** 返回 pdef / Returns the pdef */
	@Getter
	@Setter
	@XmlAttribute(name = "pdef")
	private int pdef;

	/** 返回 mdef / Returns the mdef */
	@Getter
	@Setter
	@XmlAttribute(name = "mdef")
	private int mdef;

	/** 返回 mresist / Returns the mresist */
	@Getter
	@Setter
	@XmlAttribute(name = "mresist")
	private int mresist;

	@XmlAttribute(name = "crit")
	private int crit;

	@XmlAttribute(name = "accuracy")
	private int accuracy;

	/** 返回 power / Returns the power */
	@Getter
	@Setter
	@XmlAttribute(name = "power")
	private int power;

	/** 返回 max xp / Returns the max xp */
	@Getter
	@XmlAttribute(name = "maxXp")
	private long maxXp;

	/** 返回 NPC 普攻最小伤害。 / Returns the NPC minimum auto-attack damage. */
	@Getter
	@XmlAttribute(name = "min_damage")
	private int minDamage;

	/** 返回 NPC 普攻最大伤害。 / Returns the NPC maximum auto-attack damage. */
	@Getter
	@XmlAttribute(name = "max_damage")
	private int maxDamage;
	@XmlTransient
	private boolean retailDamageRange;

	/** 返回真实 stat ratio（1000 = 1.0）。 / Returns the retail stat ratio. */
	@Getter
	@Setter
	@XmlAttribute(name = "stat_ratio")
	private int statRatio = 1000;

	/** 返回 limitAttr 抵消值。 / Returns the NPC limit-attribute reduction value. */
	@Getter
	@Setter
	@XmlAttribute(name = "limit_attribute_reduce_value")
	private int limitAttributeReduceValue;

	/** 返回 crit / Returns the crit */
	public float getCrit() {
		return crit;
	}

	/** 返回 accuracy / Returns the accuracy */
	public float getAccuracy() {
		return accuracy;
	}

	public boolean hasRetailDamageRange() {
		return retailDamageRange;
	}

	/** 设置 NPC 普攻范围。 / Sets the NPC auto-attack range. */
	public void setDamageRange(int minDamage, int maxDamage) {
		this.minDamage = minDamage;
		this.maxDamage = maxDamage;
		this.retailDamageRange = true;
	}
}
