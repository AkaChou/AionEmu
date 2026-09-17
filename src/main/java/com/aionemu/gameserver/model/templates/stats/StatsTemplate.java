package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * 属性模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "stats_template")
public abstract class StatsTemplate {
	/** 设置最大生命 / Sets the max hp*/
	@XmlAttribute(name = "maxHp")
	private int maxHp;
	@XmlAttribute(name = "hp_regen")
	private int hpregen;
	/** 返回最大魔法 / Returns the max mp*/
	@XmlAttribute(name = "maxMp")
	private int maxMp;
	/** 设置 walk speed / Sets the walk speed */
	@XmlAttribute(name = "walk_speed")
	private float walkSpeed;
	/** 返回 run speed / Returns the run speed */
	@XmlAttribute(name = "run_speed")
	private float runSpeed;
	/** 返回 fly speed / Returns the fly speed */
	@XmlAttribute(name = "fly_speed")
	private float flySpeed;
	/** 返回 attack speed / Returns the attack speed */
	@XmlAttribute(name = "attack_speed")
	private float attackSpeed;
	/** 设置 evasion / Sets the evasion */
	@XmlAttribute(name = "evasion")
	private int evasion;
	/** 返回格挡 / Returns the block */
	@XmlAttribute(name = "block")
	private int block;
	/** 返回 parry / Returns the parry */
	@XmlAttribute(name = "parry")
	private int parry;
	@XmlAttribute(name = "mboost")
	private int mboost;
	/** 设置 main hand attack / Sets the main hand attack */
	@XmlAttribute(name = "main_hand_attack")
	private int mainHandAttack;
	/** 返回 main hand accuracy / Returns the main hand accuracy */
	@XmlAttribute(name = "main_hand_accuracy")
	private int mainHandAccuracy;
	/** 返回 main hand crit rate / Returns the main hand crit rate */
	@XmlAttribute(name = "main_hand_crit_rate")
	private int mainHandCritRate;
	@XmlAttribute(name = "magic_accuracy")
	private int magicAccuracy;
	@XmlAttribute(name = "crit_spell")
	private int critSpell;
	/** 返回 strike resist / Returns the strike resist */
	@XmlAttribute(name = "strike_resist")
	private int strikeResist;
	/** 返回 spell resist / Returns the spell resist */
	@XmlAttribute(name = "spell_resist")
	private int spellResist;
	@XmlAttribute(name = "mboost_resist")
	private int mboostresist;

	/** 返回 hp regen rate / Returns the hp regen rate */
	public int getHpRegenRate() {
		return hpregen;
	}

	/** 返回 m boost / Returns the m boost */
	public int getMBoost() {
		return mboost;
	}

	/** 返回 m critical / Returns the m critical */
	public int getMCritical() {
		return critSpell;
	}

	/** 返回 mb resist / Returns the mb resist */
	public int getMBResist() {
		return mboostresist;
	}

    /** 设置 hp regen rate / Sets the hp regen rate */
    public void setHpRegenRate(int hpregen) {
       this.hpregen = hpregen;
    }
}
