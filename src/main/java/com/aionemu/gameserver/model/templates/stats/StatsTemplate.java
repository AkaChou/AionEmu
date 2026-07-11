package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 属性模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "stats_template")
public abstract class StatsTemplate {
	@XmlAttribute(name = "maxHp")
	private int maxHp;
	@XmlAttribute(name = "hp_regen")
	private int hpregen;
	@XmlAttribute(name = "maxMp")
	private int maxMp;
	@XmlAttribute(name = "walk_speed")
	private float walkSpeed;
	@XmlAttribute(name = "run_speed")
	private float runSpeed;
	@XmlAttribute(name = "fly_speed")
	private float flySpeed;
	@XmlAttribute(name = "attack_speed")
	private float attackSpeed;
	@XmlAttribute(name = "evasion")
	private int evasion;
	@XmlAttribute(name = "block")
	private int block;
	@XmlAttribute(name = "parry")
	private int parry;
	@XmlAttribute(name = "mboost")
	private int mboost;
	@XmlAttribute(name = "main_hand_attack")
	private int mainHandAttack;
	@XmlAttribute(name = "main_hand_accuracy")
	private int mainHandAccuracy;
	@XmlAttribute(name = "main_hand_crit_rate")
	private int mainHandCritRate;
	@XmlAttribute(name = "magic_accuracy")
	private int magicAccuracy;
	@XmlAttribute(name = "crit_spell")
	private int critSpell;
	@XmlAttribute(name = "strike_resist")
	private int strikeResist;
	@XmlAttribute(name = "spell_resist")
	private int spellResist;
	@XmlAttribute(name = "mboost_resist")
	private int mboostresist;

	/* ======================================= */
	public int getMaxHp() {
		return maxHp;
	}

	/** 设置最大生命 / Sets the max hp*/
	public void setMaxHp(int maxHp) {
		this.maxHp = maxHp;
	}

	/** 返回 hp regen rate / Returns the hp regen rate */
	public int getHpRegenRate() {
		return hpregen;
	}

	/** 返回最大魔法 / Returns the max mp*/
	public int getMaxMp() {
		return maxMp;
	}

	/** 设置最大魔法 / Sets the max mp*/
	public void setMaxMp(int maxMp) {
		this.maxMp = maxMp;
	}

	/* ======================================= */
	public float getWalkSpeed() {
		return walkSpeed;
	}

	/** 返回 run speed / Returns the run speed */
	public float getRunSpeed() {
		return runSpeed;
	}

	/** 返回 fly speed / Returns the fly speed */
	public float getFlySpeed() {
		return flySpeed;
	}

	/** 返回 attack speed / Returns the attack speed */
	public float getAttackSpeed() {
		return attackSpeed;
	}

	/* ======================================= */
	public int getEvasion() {
		return evasion;
	}

	/** 设置 evasion / Sets the evasion */
	public void setEvasion(int evasion) {
		this.evasion = evasion;
	}

	/** 返回黑名单 / Returns the block */
	public int getBlock() {
		return block;
	}

	/** 设置 block / Sets the block */
	public void setBlock(int block) {
		this.block = block;
	}

	/** 返回 parry / Returns the parry */
	public int getParry() {
		return parry;
	}

	/** 返回 m boost / Returns the m boost */
	public int getMBoost() {
		return mboost;
	}

	/** 设置 parry / Sets the parry */
	public void setParry(int parry) {
		this.parry = parry;
	}

	/** 返回 strike resist / Returns the strike resist */
	public int getStrikeResist() {
		return strikeResist;
	}

	/** 设置 strike resist / Sets the strike resist */
	public void setStrikeResist(int resist) {
		this.strikeResist = resist;
	}

	/** 返回 spell resist / Returns the spell resist */
	public int getSpellResist() {
		return spellResist;
	}

	/** 设置 spell resist / Sets the spell resist */
	public void setSpellResist(int resist) {
		this.spellResist = resist;
	}

	/* ======================================= */
	public int getMainHandAttack() {
		return mainHandAttack;
	}

	/** 返回 main hand accuracy / Returns the main hand accuracy */
	public int getMainHandAccuracy() {
		return mainHandAccuracy;
	}

	/** 返回 main hand crit rate / Returns the main hand crit rate */
	public int getMainHandCritRate() {
		return mainHandCritRate;
	}

	/* ======================================= */
	public int getMagicAccuracy() {
		return magicAccuracy;
	}

	/** 返回 m critical / Returns the m critical */
	public int getMCritical() {
		return critSpell;
	}

	/** 返回 mb resist / Returns the mb resist */
	public int getMBResist() {
		return mboostresist;
	}

    /** 设置 main hand attack / Sets the main hand attack */
    public void setMainHandAttack(int mainHandAttack) {
       this.mainHandAttack = mainHandAttack;
    }

    /** 设置 main hand accuracy / Sets the main hand accuracy */
    public void setMainHandAccuracy(int mainHandAccuracy) {
       this.mainHandAccuracy = mainHandAccuracy;
    }

    /** 设置 main hand crit rate / Sets the main hand crit rate */
    public void setMainHandCritRate(int mainHandCritRate) {
       this.mainHandCritRate = mainHandCritRate;
    }

    /** 设置 hp regen rate / Sets the hp regen rate */
    public void setHpRegenRate(int hpregen) {
       this.hpregen = hpregen;
    }

    /** 设置 run speed / Sets the run speed */
    public void setRunSpeed(float runSpeed) {
       this.runSpeed = runSpeed;
    }

    /** 设置 walk speed / Sets the walk speed */
    public void setWalkSpeed(float walkSpeed) {
       this.walkSpeed = walkSpeed;
    }

    /** 设置 attack speed / Sets the attack speed */
    public void setAttackSpeed(float attackSpeed) {
       this.attackSpeed = attackSpeed;
    }
}
