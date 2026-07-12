package com.aionemu.gameserver.utils.stats;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.utils.stats.enums.ACCURACY;
import com.aionemu.gameserver.utils.stats.enums.AGILITY;
import com.aionemu.gameserver.utils.stats.enums.ATTACK_RANGE;
import com.aionemu.gameserver.utils.stats.enums.ATTACK_SPEED;
import com.aionemu.gameserver.utils.stats.enums.BLOCK;
import com.aionemu.gameserver.utils.stats.enums.CRIT_SPELL;
import com.aionemu.gameserver.utils.stats.enums.EARTH_RESIST;
import com.aionemu.gameserver.utils.stats.enums.EVASION;
import com.aionemu.gameserver.utils.stats.enums.FIRE_RESIST;
import com.aionemu.gameserver.utils.stats.enums.FLY_SPEED;
import com.aionemu.gameserver.utils.stats.enums.HEALTH;
import com.aionemu.gameserver.utils.stats.enums.KNOWLEDGE;
import com.aionemu.gameserver.utils.stats.enums.MAGIC_ACCURACY;
import com.aionemu.gameserver.utils.stats.enums.MAIN_HAND_ACCURACY;
import com.aionemu.gameserver.utils.stats.enums.MAIN_HAND_ATTACK;
import com.aionemu.gameserver.utils.stats.enums.MAIN_HAND_CRITRATE;
import com.aionemu.gameserver.utils.stats.enums.MAXHP;
import com.aionemu.gameserver.utils.stats.enums.PARRY;
import com.aionemu.gameserver.utils.stats.enums.POWER;
import com.aionemu.gameserver.utils.stats.enums.SPEED;
import com.aionemu.gameserver.utils.stats.enums.SPELL_RESIST;
import com.aionemu.gameserver.utils.stats.enums.STRIKE_RESIST;
import com.aionemu.gameserver.utils.stats.enums.WATER_RESIST;
import com.aionemu.gameserver.utils.stats.enums.WILL;
import com.aionemu.gameserver.utils.stats.enums.WIND_RESIST;

/**
 * 按职业从 enums 包查询基础属性
 * Looks up base stats for a player class from the enums package
 */
public class ClassStats {

	/**
	 * 获取指定职业与等级的最大生命值
	 * Get max HP for class and level
	 *
	 * Player class
	 * Level
	 * @return 最大生命值 / Max HP
	 */
	public static int getMaxHpFor(PlayerClass playerClass, int level) {
		return MAXHP.valueOf(playerClass.toString()).getMaxHpFor(level);
	}

	/**
	 * 获取职业力量
	 * Get power for class
	 *
	 * Player class
	 * Power
	 */
	public static int getPowerFor(PlayerClass playerClass) {
		return POWER.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业体质
	 * Get health for class
	 *
	 * Player class
	 * Health
	 */
	public static int getHealthFor(PlayerClass playerClass) {
		return HEALTH.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业敏捷
	 * Get agility for class
	 *
	 * Player class
	 * Agility
	 */
	public static int getAgilityFor(PlayerClass playerClass) {
		return AGILITY.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业精准
	 * Get accuracy for class
	 *
	 * Player class
	 * Accuracy
	 */
	public static int getAccuracyFor(PlayerClass playerClass) {
		return ACCURACY.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业智力
	 * Get knowledge for class
	 *
	 * Player class
	 * Knowledge
	 */
	public static int getKnowledgeFor(PlayerClass playerClass) {
		return KNOWLEDGE.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业意志
	 * Get will for class
	 *
	 * Player class
	 * Will
	 */
	public static int getWillFor(PlayerClass playerClass) {
		return WILL.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业主手攻击
	 * Get main-hand attack for class
	 *
	 * Player class
	 * Main-hand attack
	 */
	public static int getMainHandAttackFor(PlayerClass playerClass) {
		return MAIN_HAND_ATTACK.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业主手暴击率
	 * Get main-hand crit rate for class
	 *
	 * Player class
	 *
	 * @param playerClass
	 * @return 主手暴击率 / Main-hand crit rate
	 */
	public static int getMainHandCritRateFor(PlayerClass playerClass) {
		return MAIN_HAND_CRITRATE.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业主手命中
	 * Get main-hand accuracy for class
	 *
	 * Player class
	 * Main-hand accuracy
	 */
	public static int getMainHandAccuracyFor(PlayerClass playerClass) {
		return MAIN_HAND_ACCURACY.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业水抗
	 * Get water resistance for class
	 *
	 * Player class
	 * Water resist
	 */
	public static int getWaterResistFor(PlayerClass playerClass) {
		return WATER_RESIST.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业风抗
	 * Get wind resistance for class
	 *
	 * Player class
	 * Wind resist
	 */
	public static int getWindResistFor(PlayerClass playerClass) {
		return WIND_RESIST.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业地抗
	 * Get earth resistance for class
	 *
	 * Player class
	 * Earth resist
	 */
	public static int getEarthResistFor(PlayerClass playerClass) {
		return EARTH_RESIST.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业火抗
	 * Get fire resistance for class
	 *
	 * Player class
	 * Fire resist
	 */
	public static int getFireResistFor(PlayerClass playerClass) {
		return FIRE_RESIST.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业魔法命中
	 * Get magic accuracy for class
	 *
	 * Player class
	 * Magic accuracy
	 */
	public static int getMagicAccuracyFor(PlayerClass playerClass) {
		return MAGIC_ACCURACY.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业法术暴击
	 * Get spell crit for class
	 *
	 * Player class
	 * Spell crit
	 */
	public static int getCritSpellFor(PlayerClass playerClass) {
		return CRIT_SPELL.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业法术抗性
	 * Get spell resist for class
	 *
	 * Player class
	 * Spell resist
	 */
	public static int getSpellResistFor(PlayerClass playerClass) {
		return SPELL_RESIST.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业物理抗性
	 * Get strike resist for class
	 *
	 * Player class
	 * Strike resist
	 */
	public static int getStrikeResistFor(PlayerClass playerClass) {
		return STRIKE_RESIST.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业闪避
	 * Get evasion for class
	 *
	 * Player class
	 * Evasion
	 */
	public static int getEvasionFor(PlayerClass playerClass) {
		return EVASION.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业格挡
	 * Get block for class
	 *
	 * Player class
	 * Block
	 */
	public static int getBlockFor(PlayerClass playerClass) {
		return BLOCK.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业招架
	 * Get parry for class
	 *
	 * Player class
	 * Parry
	 */
	public static int getParryFor(PlayerClass playerClass) {
		return PARRY.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业攻击距离
	 * Get attack range for class
	 *
	 * Player class
	 * Attack range
	 */
	public static int getAttackRangeFor(PlayerClass playerClass) {
		return ATTACK_RANGE.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业攻击速度
	 * Get attack speed for class
	 *
	 * Player class
	 * Attack speed
	 */
	public static int getAttackSpeedFor(PlayerClass playerClass) {
		return ATTACK_SPEED.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业飞行速度
	 * Get fly speed for class
	 *
	 * Player class
	 * Fly speed
	 */
	public static int getFlySpeedFor(PlayerClass playerClass) {
		return FLY_SPEED.valueOf(playerClass.toString()).getValue();
	}

	/**
	 * 获取职业移动速度
	 * Get movement speed for class
	 *
	 * Player class
	 * Speed
	 */
	public static int getSpeedFor(PlayerClass playerClass) {
		return SPEED.valueOf(playerClass.toString()).getValue();
	}
}
