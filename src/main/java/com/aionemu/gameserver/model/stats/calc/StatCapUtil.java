package com.aionemu.gameserver.model.stats.calc;

import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.stats.container.CombatMode;
import com.aionemu.gameserver.model.stats.container.RatioType;

/**
 * 属性 Cap 工具模型。
 * Stat Cap Util model.
 */

@Slf4j
public class StatCapUtil {
	static final int LOWER_CAP = Short.MIN_VALUE;
	static final int UPPER_CAP = Short.MAX_VALUE;

	static class StatLimits {
		public final int lowerCap;
		public final int upperCap;

		public StatLimits() {
			this.lowerCap = LOWER_CAP;
			this.upperCap = UPPER_CAP;
		}

		public StatLimits(int lowerCap, int upperCap) {
			this.lowerCap = lowerCap;
			this.upperCap = upperCap;
		}
	}

	static HashMap<StatEnum, Integer> minValues = new HashMap<StatEnum, Integer>();
	static HashMap<StatEnum, Integer> maxValues = new HashMap<StatEnum, Integer>();
	static HashMap<StatEnum, StatLimits> limits = new HashMap<StatEnum, StatLimits>();
	static {
		for (StatEnum stat : StatEnum.values()) {
			minValues.put(stat, getLowerCap(stat));
			maxValues.put(stat, getUpperCap(stat));
			limits.put(stat, new StatLimits(getLowerCap(stat), getUpperCap(stat)));
		}
	}

	/** 计算基础值。 / Calculate base value. */
	public static void calculateBaseValue(Stat2 stat, byte isPlayer) {
		int lowerCap = getLowerCap(stat.getStat());
		int upperCap = getUpperCap(stat.getStat());
		if (stat.getStat() == StatEnum.ATTACK_SPEED) {
			int base = stat.getBase() / 2;
			if (stat.getBonus() > 0 && base < stat.getBonus()) {
				stat.setBonus(base);
			} else if (stat.getBonus() < 0 && base < -stat.getBonus()) {
				stat.setBonus(-base);
			}
		} else if (stat.getStat() == StatEnum.SPEED || stat.getStat() == StatEnum.FLY_SPEED
				|| stat.getStat() == StatEnum.SOAR_SPEED) {
			if (isPlayer == 2) {
				upperCap = Integer.MAX_VALUE;
			}
		}
		calculate(stat, lowerCap, upperCap);
		if (isPlayer != 1) {
			int newValue = stat.getCurrent();
			if (newValue < LOWER_CAP) {
				minValues.put(stat.getStat(), newValue);
			}
			if (newValue > UPPER_CAP) {
				maxValues.put(stat.getStat(), newValue);
			}
		}
	}

	/** 返回最小值 / Returns the min value*/
	public static int getMinValue(StatEnum stat) {
		return minValues.get(stat);
	}

	/** 返回最大值 / Returns the max value*/
	public static int getMaxValue(StatEnum stat) {
		return maxValues.get(stat);
	}

	/** 返回 lower cap / Returns the lower cap */
	public static int getLowerCap(StatEnum stat) {
		if (limits.containsKey(stat)) {
			return limits.get(stat).lowerCap;
		}
		int value = LOWER_CAP;
		switch (stat) {
		case MAIN_HAND_POWER:
		case MAIN_HAND_ACCURACY:
		case MAIN_HAND_CRITICAL:
		case OFF_HAND_POWER:
		case OFF_HAND_ACCURACY:
		case OFF_HAND_CRITICAL:
		case MAGICAL_CRITICAL_RESIST:
		case PHYSICAL_CRITICAL_RESIST:
		case EVASION:
		case PHYSICAL_DEFENSE:
		case MAGICAL_DEFEND:
		case PHYSICAL_ACCURACY:
		case MAGICAL_ACCURACY:
		case SPEED:
		case FLY_SPEED:
		case SOAR_SPEED:
		case MAXHP:
		case MAXMP:
			value = 0;
			break;
		default:
			break;
		}
		return value;
	}

	/** 返回 upper cap / Returns the upper cap */
	public static int getUpperCap(StatEnum stat) {
		if (limits.containsKey(stat)) {
			return limits.get(stat).upperCap;
		}
		int value = UPPER_CAP;
		switch (stat) {
		case SPEED:
			value = 12000;
			break;
		case FLY_SPEED:
		case SOAR_SPEED:
			value = 16000;
			break;
		case PVP_ATTACK_RATIO:
		case PVP_ATTACK_RATIO_PHYSICAL:
		case PVP_ATTACK_RATIO_MAGICAL:
		case PVP_DEFEND_RATIO:
		case PVP_DEFEND_RATIO_PHYSICAL:
		case PVP_DEFEND_RATIO_MAGICAL:
			value = 900;
			break;
		case BOOST_MAGICAL_SKILL:
			value = 32767;
			break;
		case MAXHP:
		case MAXMP:
		case REGEN_HP:
		case REGEN_MP:
		case HEAL_BOOST:
		case HEAL_SKILL_BOOST:
		case PHYSICAL_ACCURACY:
		case PHYSICAL_ATTACK:
		case PHYSICAL_CRITICAL:
		case PHYSICAL_DEFENSE:
		case BOOST_DURATION_BUFF:
		case MAGIC_SKILL_BOOST_RESIST:
			value = Integer.MAX_VALUE;
			break;
		default:
			break;
		}
		return value;
	}

	/** Limit Value For Pvp Or Pve Stat / Limit Value For Pvp Or Pve Stat */
	public static int limitValueForPvpOrPveStat(CombatMode mode, RatioType type, int value) {
		int min;
		int max;
		if (mode == CombatMode.PVP) {
			min = type == RatioType.ATTACK ? -900 : -1000;
			max = type == RatioType.ATTACK ? 1000 : 900;
		} else {
			min = type == RatioType.ATTACK ? -900 : -5000;
			max = type == RatioType.ATTACK ? 5000 : 900;
		}
		return Math.max(min, Math.min(max, value));
	}

	private static void calculate(Stat2 stat2, int lowerCap, int upperCap) {
		if (stat2.getCurrent() > upperCap) {
			stat2.setBonus(upperCap - stat2.getBase());
		} else if (stat2.getCurrent() < lowerCap) {
			stat2.setBonus(lowerCap - stat2.getBase());
		}
	}
	
		/** Dump Wrong Stats / Dump Wrong Stats */
		public static void dumpWrongStats(String ownerInfo, Stat2... stats) {
		List<Stat2> wrongStats = null;
		for (Stat2 stat : stats) {
			Stat2 wrongStat = null;
			if (stat.getCurrent() < getLowerCap(stat.getStat())) {
				wrongStat = stat;
			}
			if (stat.getCurrent() > getUpperCap(stat.getStat())) {
				wrongStat = stat;
			}
			if (wrongStat != null) {
				if (wrongStats == null) {
					wrongStats = new ArrayList<Stat2>();
				}
				wrongStats.add(wrongStat);
			}
		}
		if (wrongStats == null) {
			return;
		}
		StringBuilder msg = new StringBuilder();
		msg.append(ownerInfo);
		msg.append('\n');
		for (Stat2 stat : wrongStats) {
			msg.append(I18n.get("log.78602a34d7c7", stat.getStat(), getMinValue(stat.getStat()),
					getMaxValue(stat.getStat()), stat.getBase(), stat.getBonus()));
		}
		log.error(msg.toString());
	}
}
