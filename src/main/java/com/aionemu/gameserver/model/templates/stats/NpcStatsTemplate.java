package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * NPC 属性模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "npc_stats_template")
public class NpcStatsTemplate extends StatsTemplate {
	@XmlAttribute(name = "run_speed_fight")
	private float runSpeedFight;

	@XmlAttribute(name = "pdef")
	private int pdef;

	@XmlAttribute(name = "mdef")
	private int mdef;

	@XmlAttribute(name = "mresist")
	private int mresist;

	@XmlAttribute(name = "crit")
	private int crit;

	@XmlAttribute(name = "accuracy")
	private int accuracy;

	@XmlAttribute(name = "power")
	private int power;

	@XmlAttribute(name = "maxXp")
	private long maxXp;

	@XmlAttribute(name = "min_damage")
	private int minDamage;

	@XmlAttribute(name = "max_damage")
	private int maxDamage;
	@XmlTransient
	private boolean retailDamageRange;

	@XmlAttribute(name = "stat_ratio")
	private int statRatio = 1000;

	@XmlAttribute(name = "limit_attribute_reduce_value")
	private int limitAttributeReduceValue;

	/** 返回 run speed fight / Returns the run speed fight */
	public float getRunSpeedFight() {
		return runSpeedFight;
	}

	/** 返回 pdef / Returns the pdef */
	public int getPdef() {
		return pdef;
	}

	/** 返回 mdef / Returns the mdef */
	public int getMdef() {
		return mdef;
	}

	/** 返回 mresist / Returns the mresist */
	public int getMresist() {
		return mresist;
	}

	/** 返回 crit / Returns the crit */
	public float getCrit() {
		return crit;
	}

	/** 返回 accuracy / Returns the accuracy */
	public float getAccuracy() {
		return accuracy;
	}

	/** 返回 power / Returns the power */
	public int getPower() {
		return power;
	}

	/** 设置 power / Sets the power */
	public void setPower(int power) {
		this.power = power;
	}

	/** 返回 max xp / Returns the max xp */
	public long getMaxXp() {
		return maxXp;
	}

	/** 返回 NPC 普攻最小伤害。 / Returns the NPC minimum auto-attack damage. */
	public int getMinDamage() {
		return minDamage;
	}

	/** 返回 NPC 普攻最大伤害。 / Returns the NPC maximum auto-attack damage. */
	public int getMaxDamage() {
		return maxDamage;
	}

	public boolean hasRetailDamageRange() {
		return retailDamageRange;
	}

	/** 返回真实 stat ratio（1000 = 1.0）。 / Returns the retail stat ratio. */
	public int getStatRatio() {
		return statRatio;
	}

	/** 设置真实 stat ratio。 / Sets the retail stat ratio. */
	public void setStatRatio(int statRatio) {
		this.statRatio = statRatio;
	}

	/** 返回 limitAttr 抵消值。 / Returns the NPC limit-attribute reduction value. */
	public int getLimitAttributeReduceValue() {
		return limitAttributeReduceValue;
	}

	/** 设置 NPC 普攻范围。 / Sets the NPC auto-attack range. */
	public void setDamageRange(int minDamage, int maxDamage) {
		this.minDamage = minDamage;
		this.maxDamage = maxDamage;
		this.retailDamageRange = true;
	}

	/** 设置 limitAttr 抵消值。 / Sets the limit-attribute reduction value. */
	public void setLimitAttributeReduceValue(int value) {
		this.limitAttributeReduceValue = value;
	}

    /** 设置 pdef / Sets the pdef */
    public void setPdef(int pdef) {
       this.pdef = pdef;
    }

    /** 设置 mdef / Sets the mdef */
    public void setMdef(int mdef) {
       this.mdef = mdef;
    }

    /** 设置 mresist / Sets the mresist */
    public void setMresist(int mresist) {
       this.mresist = mresist;
    }
}
