package com.aionemu.gameserver.model.templates.stats;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.utils.stats.ClassStats;

/**
 * Calculated 玩家属性模板（静态数据/XML）。
 * XML template.
 */

public class CalculatedPlayerStatsTemplate extends PlayerStatsTemplate {
	private PlayerClass playerClass;

	public CalculatedPlayerStatsTemplate(PlayerClass playerClass) {
		this.playerClass = playerClass;
	}

	/** 返回 accuracy / Returns the accuracy */
	@Override
	public int getAccuracy() {
		return ClassStats.getAccuracyFor(playerClass);
	}

	/** 返回 agility / Returns the agility */
	@Override
	public int getAgility() {
		return ClassStats.getAgilityFor(playerClass);
	}

	/** 返回攻击范围 / Returns the attack range*/
	public float getAttackRange() {
		return ClassStats.getAttackRangeFor(playerClass) / 1500f;
	}

	/** 返回 attack speed / Returns the attack speed */
	@Override
	public float getAttackSpeed() {
		return ClassStats.getAttackSpeedFor(playerClass) / 1000f;
	}

	/** 返回格挡 / Returns the block */
	@Override
	public int getBlock() {
		return ClassStats.getBlockFor(playerClass);
	}

	/** 返回 crit spell / Returns the crit spell */
	public int getCritSpell() {
		return ClassStats.getCritSpellFor(playerClass);
	}

	/** 返回 evasion / Returns the evasion */
	@Override
	public int getEvasion() {
		return ClassStats.getEvasionFor(playerClass);
	}

	/** 返回 fly speed / Returns the fly speed */
	@Override
	public float getFlySpeed() {
		return ClassStats.getFlySpeedFor(playerClass);
	}

	/** 返回 health / Returns the health */
	@Override
	public int getHealth() {
		return ClassStats.getHealthFor(playerClass);
	}

	/** 返回 knowledge / Returns the knowledge */
	@Override
	public int getKnowledge() {
		return ClassStats.getKnowledgeFor(playerClass);
	}

	/** 返回 magic accuracy / Returns the magic accuracy */
	@Override
	public int getMagicAccuracy() {
		return ClassStats.getMagicAccuracyFor(playerClass);
	}

	/** 返回 main hand accuracy / Returns the main hand accuracy */
	@Override
	public int getMainHandAccuracy() {
		return ClassStats.getMainHandAccuracyFor(playerClass);
	}

	/** 返回 main hand attack / Returns the main hand attack */
	@Override
	public int getMainHandAttack() {
		return ClassStats.getMainHandAttackFor(playerClass);
	}

	/** 返回 main hand crit rate / Returns the main hand crit rate */
	@Override
	public int getMainHandCritRate() {
		return ClassStats.getMainHandCritRateFor(playerClass);
	}

	/** 返回最大生命 / Returns the max hp*/
	@Override
	public int getMaxHp() {
		return ClassStats.getMaxHpFor(playerClass, 17); // 10
	}

	/** 返回最大魔法 / Returns the max mp*/
	@Override
	public int getMaxMp() {
		return 1000;
	}

	/** 返回 parry / Returns the parry */
	@Override
	public int getParry() {
		return ClassStats.getParryFor(playerClass);
	}

	/** 返回 power / Returns the power */
	@Override
	public int getPower() {
		return ClassStats.getPowerFor(playerClass);
	}

	/** 返回 speed / Returns the speed */
	public float getSpeed() {
		return ClassStats.getSpeedFor(playerClass);
	}

	/** 返回 spell resist / Returns the spell resist */
	@Override
	public int getSpellResist() {
		return ClassStats.getSpellResistFor(playerClass);
	}

	/** 返回 strike resist / Returns the strike resist */
	@Override
	public int getStrikeResist() {
		return ClassStats.getStrikeResistFor(playerClass);
	}

	/** 返回 will / Returns the will */
	@Override
	public int getWill() {
		return ClassStats.getWillFor(playerClass);
	}

	/** 返回 walk speed / Returns the walk speed */
	@Override
	public float getWalkSpeed() {
		return 1.5f;
	}
}
