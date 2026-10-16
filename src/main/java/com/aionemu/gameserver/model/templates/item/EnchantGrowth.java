package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 觉醒/强化表外无限成长数据，取自真端 client_item_authorizetable.xml 的 start_limitless 与 limitless_attr_list。
 * Retail limitless growth data taken from the client's start_limitless / limitless_attr_list.
 *
 * 两类条目 / two kinds of entries:
 * 一是表内数据与真端一致的表，只登记表外每级递增；
 * 二是表内数据与真端不符的表，按真端线性表整表覆盖（XML 表内数据保持原样、不参与计算）。
 * Tables whose XML data matches retail only register their per-level increments; the few whose XML data disagrees
 * with retail are fully overridden by the retail linear table, leaving their XML entries untouched and unused.
 */

public final class EnchantGrowth {

	/**
	 * 表外成长定义。 / Growth definition beyond the table.
	 *
	 * @param base      整表覆盖时的表内属性，null 表示沿用 XML 表内数据 / overriding stats, null to keep the XML table
	 * @param startLevel 无限成长起始等级 / level from which the increments apply
	 * @param increments 每级递增量 / stats added per level
	 */
	record Growth(List<StatFunction> base, int startLevel, List<StatFunction> increments) {

		/**
		 * 计算指定等级的表外属性。 / Resolves the stats of a level beyond the table.
		 *
		 * @param level      强化/觉醒等级 / enchant or authorize level
		 * @param tableStats 表内最高级属性 / the table's highest-level stats
		 * @param tableMax   表内最高等级 / the table's highest level
		 * @return 属性列表 / resolved stat list
		 */
		List<StatFunction> at(int level, List<StatFunction> tableStats, int tableMax) {
			List<StatFunction> stats = this.base != null ? this.base : tableStats;
			int maxLevel = this.base != null ? 1 : tableMax;
			int steps = level - this.startLevel + 1;
			if (stats == null || level <= maxLevel || steps <= 0) {
				return stats;
			}
			List<StatFunction> grown = new ArrayList<>(stats);
			for (StatFunction increment : this.increments) {
				int delta = increment.getValue() * steps;
				int index = indexOf(grown, increment.getName());
				if (index < 0) {
					grown.add(new StatAddFunction(increment.getName(), delta, false));
				} else if (grown.get(index) instanceof StatAddFunction present) {
					// shortcut: 表内末级目前只有 add 类型；其它类型不参与成长 / only add entries occur at table tops today
					grown.set(index, new StatAddFunction(present.getName(), present.getValue() + delta, present.isBonus()));
				}
			}
			return grown;
		}

		private static int indexOf(List<StatFunction> stats, StatEnum stat) {
			for (int i = 0; i < stats.size(); i++) {
				if (stats.get(i).getName() == stat) {
					return i;
				}
			}
			return -1;
		}
	}

	private static final Map<Integer, Growth> TABLES = new HashMap<>();

	/**
	 * 计算指定等级的属性；该表未登记成长数据时原样返回表内属性。
	 * Applies the growth of the table, returning the table stats unchanged when none is registered.
	 *
	 * @param id        表 ID / table id
	 * @param level     强化/觉醒等级 / enchant or authorize level
	 * @param tableStats 表内最高级属性 / the table's highest-level stats
	 * @param tableMax  表内最高等级 / the table's highest level
	 * @return 属性列表 / resolved stat list
	 */
	public static List<StatFunction> grow(int id, int level, List<StatFunction> tableStats, int tableMax) {
		Growth growth = TABLES.get(id);
		return growth == null ? tableStats : growth.at(level, tableStats, tableMax);
	}

	/** 登记表外每级递增 / registers per-level increments beyond the table */
	private static void add(int id, int startLevel, StatFunction... increments) {
		TABLES.put(id, new Growth(null, startLevel, List.of(increments)));
	}

	/** 真端线性表：1 级即等于每级值，2 级起逐级累加 / retail linear tables: level 1 equals the per-level value, accumulated from level 2 on */
	private static void override(int id, StatFunction... perLevel) {
		List<StatFunction> stats = List.of(perLevel);
		TABLES.put(id, new Growth(stats, 2, stats));
	}

	private static StatFunction inc(StatEnum stat, int value) {
		return new StatAddFunction(stat, value, false);
	}

	static {
		add(10000, 11, inc(StatEnum.PVE_DEFEND_RATIO_PHYSICAL, 10), inc(StatEnum.PVE_DEFEND_RATIO_MAGICAL, 10));
		add(10001, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10002, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10003, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10004, 11, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10005, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10006, 11, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10007, 11, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10008, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10009, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10010, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10011, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10012, 11, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10013, 11, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10014, 11, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10015, 11, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10016, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10017, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10018, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10019, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10020, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10021, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10022, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10023, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10024, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10025, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10026, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 1));
		add(10027, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10028, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10029, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10030, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10031, 11,
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1),
				inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10032, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10033, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10034, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10035, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10036, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 1));
		add(10042, 11, inc(StatEnum.PHYSICAL_ACCURACY, 15), inc(StatEnum.MAGICAL_ACCURACY, 10));
		add(10043, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10044, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10045, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10046, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		add(10047, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 6));
		add(10048, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		add(10049, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 6));
		add(10050, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		add(10053, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 6));
		add(10054, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 6));
		add(10055, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 6));
		add(10056, 2, inc(StatEnum.MAXHP, 150), inc(StatEnum.PHYSICAL_CRITICAL, 12));
		add(10057, 2, inc(StatEnum.MAXHP, 150), inc(StatEnum.MAGICAL_ACCURACY, 8));
		add(10058, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		add(10059, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 6));
		add(10060, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		add(10061, 11, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 6));
		add(10062, 11, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		add(10067, 21,
				inc(StatEnum.PHYSICAL_ATTACK, 4), inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 6),
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 6));
		add(10068, 21,
				inc(StatEnum.PHYSICAL_ATTACK, 6), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 6));
		add(10069, 21,
				inc(StatEnum.PHYSICAL_ATTACK, 4), inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 6),
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 6));
		add(10070, 21,
				inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10071, 21,
				inc(StatEnum.MAGICAL_ATTACK, 6), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10072, 21,
				inc(StatEnum.PHYSICAL_ATTACK, 8), inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10),
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10073, 21,
				inc(StatEnum.PHYSICAL_ATTACK, 8), inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10),
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10074, 21,
				inc(StatEnum.PHYSICAL_ATTACK, 6), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10075, 21,
				inc(StatEnum.PHYSICAL_ATTACK, 8), inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10),
				inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10076, 21,
				inc(StatEnum.MAGICAL_ATTACK, 4), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 6), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 6));
		add(10077, 21,
				inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10078, 21,
				inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10079, 21,
				inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40),
				inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 10), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 10));
		add(10080, 21,
				inc(StatEnum.MAXHP, 100), inc(StatEnum.PHYSICAL_DEFENSE, 50),
				inc(StatEnum.MAGIC_SKILL_BOOST_RESIST, 20), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 5),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 5));
		add(10081, 21,
				inc(StatEnum.MAXHP, 48), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 5),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 5));
		add(10082, 21,
				inc(StatEnum.MAXHP, 44), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 4),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 4));
		add(10083, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 2), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10084, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 2), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10085, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 2), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10086, 21,
				inc(StatEnum.MAXHP, 44), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 5),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 5));
		add(10087, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 4),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 4));
		add(10088, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10089, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10090, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10091, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 10), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 5),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 5));
		add(10092, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 4),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 4));
		add(10093, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10094, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10095, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10096, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 12), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 5),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 5));
		add(10097, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 10), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 4),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 4));
		add(10098, 21,
				inc(StatEnum.MAXHP, 28), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10099, 21,
				inc(StatEnum.MAXHP, 28), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10100, 21,
				inc(StatEnum.MAXHP, 28), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8), inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 3),
				inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 3));
		add(10113, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10114, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10115, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10116, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10117, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10118, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10119, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10120, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10121, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10122, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10123, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10124, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10125, 10, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10126, 10, inc(StatEnum.MAXHP, 1));
		add(10127, 8, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 1));
		add(10128, 8, inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 1));
		add(10129, 10, inc(StatEnum.MAXHP, 1));
		add(10130, 10, inc(StatEnum.MAXHP, 1));
		add(10131, 10, inc(StatEnum.MAXHP, 1));
		add(10132, 10, inc(StatEnum.MAXHP, 1));
		add(10133, 10, inc(StatEnum.MAXHP, 1));
		add(10134, 10, inc(StatEnum.MAXHP, 1));
		add(10135, 10, inc(StatEnum.MAXHP, 1));
		add(10136, 10, inc(StatEnum.MAXHP, 1));
		add(10137, 10, inc(StatEnum.MAXHP, 1));
		add(10138, 10, inc(StatEnum.MAXHP, 1));
		add(10139, 10, inc(StatEnum.MAXHP, 1));
		add(10140, 10, inc(StatEnum.MAXHP, 1));
		add(10141, 10, inc(StatEnum.MAXHP, 1));
		add(10142, 10, inc(StatEnum.MAXHP, 1));
		add(10143, 10, inc(StatEnum.MAXHP, 1));
		add(10144, 10, inc(StatEnum.MAXHP, 1));
		add(10145, 10, inc(StatEnum.MAXHP, 1));
		add(10146, 10, inc(StatEnum.MAXHP, 1));
		add(10147, 10, inc(StatEnum.MAXHP, 1));
		add(10148, 10, inc(StatEnum.MAXHP, 1));
		add(10149, 8, inc(StatEnum.MAXHP, 1));
		add(10150, 21, inc(StatEnum.PHYSICAL_ATTACK, 4));
		add(10151, 21, inc(StatEnum.PHYSICAL_ATTACK, 6), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10152, 21, inc(StatEnum.PHYSICAL_ATTACK, 4));
		add(10153, 21, inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10154, 21, inc(StatEnum.MAGICAL_ATTACK, 6), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10155, 21, inc(StatEnum.PHYSICAL_ATTACK, 8));
		add(10156, 21, inc(StatEnum.PHYSICAL_ATTACK, 8));
		add(10157, 21, inc(StatEnum.PHYSICAL_ATTACK, 6), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10158, 21, inc(StatEnum.PHYSICAL_ATTACK, 8));
		add(10159, 21, inc(StatEnum.MAGICAL_ATTACK, 4), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10160, 21, inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10161, 21, inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10162, 21, inc(StatEnum.MAGICAL_ATTACK, 8), inc(StatEnum.BOOST_MAGICAL_SKILL, 40));
		add(10163, 21,
				inc(StatEnum.MAXHP, 100), inc(StatEnum.PHYSICAL_DEFENSE, 50),
				inc(StatEnum.MAGIC_SKILL_BOOST_RESIST, 20));
		add(10164, 21,
				inc(StatEnum.MAXHP, 48), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10165, 21,
				inc(StatEnum.MAXHP, 44), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10166, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 2), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10167, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 2), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10168, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 2), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10169, 21,
				inc(StatEnum.MAXHP, 44), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10170, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10171, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10172, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10173, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 4), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10174, 21,
				inc(StatEnum.MAXHP, 40), inc(StatEnum.PHYSICAL_DEFENSE, 10), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10175, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10176, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10177, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10178, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 6), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10179, 21,
				inc(StatEnum.MAXHP, 36), inc(StatEnum.PHYSICAL_DEFENSE, 12), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 8),
				inc(StatEnum.MAGICAL_DEFEND, 6), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10180, 21,
				inc(StatEnum.MAXHP, 32), inc(StatEnum.PHYSICAL_DEFENSE, 10), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 6),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10181, 21,
				inc(StatEnum.MAXHP, 28), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10182, 21,
				inc(StatEnum.MAXHP, 28), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		add(10183, 21,
				inc(StatEnum.MAXHP, 28), inc(StatEnum.PHYSICAL_DEFENSE, 8), inc(StatEnum.PHYSICAL_CRITICAL_RESIST, 4),
				inc(StatEnum.MAGICAL_DEFEND, 4), inc(StatEnum.PHYSICAL_ATTACK, 2),
				inc(StatEnum.BOOST_MAGICAL_SKILL, 8));
		override(10037, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		override(10038, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 7), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 7));
		override(10039, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		override(10040, inc(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 5), inc(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 5));
		override(10041, inc(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 7), inc(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 7));
		override(10218, inc(StatEnum.PVE_ATTACK_RATIO_PHYSICAL, 6), inc(StatEnum.PVE_ATTACK_RATIO_MAGICAL, 6));
		override(10219, inc(StatEnum.PVE_DEFEND_RATIO_PHYSICAL, 8), inc(StatEnum.PVE_DEFEND_RATIO_MAGICAL, 8));
		override(10220, inc(StatEnum.PVE_ATTACK_RATIO_PHYSICAL, 6), inc(StatEnum.PVE_ATTACK_RATIO_MAGICAL, 6));
		override(10221, inc(StatEnum.PVE_ATTACK_RATIO_PHYSICAL, 6), inc(StatEnum.PVE_ATTACK_RATIO_MAGICAL, 6));
		override(10222, inc(StatEnum.PVE_DEFEND_RATIO_PHYSICAL, 8), inc(StatEnum.PVE_DEFEND_RATIO_MAGICAL, 8));
	}

	private EnchantGrowth() {
	}
}
