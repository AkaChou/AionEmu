package com.aionemu.gameserver.geoEngine.collision;

import com.aionemu.gameserver.model.Race;

/**
 * 碰撞忽略属性，按种族与静态物体 id 标记在检测中应忽略的目标。
 * Collision ignore properties keyed by race and static object id for targets
 * that should be skipped during collision tests.
 */
public class IgnoreProperties {

	/** 天族预置忽略。 / Prefabricated Elyos ignore. */
	public static final IgnoreProperties ELYOS = new IgnoreProperties(Race.ELYOS, 0);
	/** 魔族预置忽略。 / Prefabricated Asmodian ignore. */
	public static final IgnoreProperties ASMODIANS = new IgnoreProperties(Race.ASMODIANS, 0);
	/** 龙族/龙族预置忽略。 / Prefabricated Balaur/Drakan ignore. */
	public static final IgnoreProperties BALAUR = new IgnoreProperties(Race.DRAKAN, 0);
	/** 任意种族预置忽略。 / Prefabricated any-race ignore. */
	public static final IgnoreProperties ANY_RACE = new IgnoreProperties(null, 0);

	/** 忽略的种族；{@code null} 表示无种族过滤 / Race to ignore; {@code null} means no race filter */
	private final Race race;
	/** 忽略的静态物体 id；0 表示不按 id 过滤。 / Static object id to ignore; 0 means no id filter. */
	private final int staticId;

	/**
	 * 私有构造。
	 * Private constructor.
	 *
	 * @param race 阵营 / race
	 * @param staticId 静态物体 id / static object id
	 */
	private IgnoreProperties(Race race, int staticId) {
		this.race = race;
		this.staticId = staticId;
	}

	/**
	 * 按种族与静态 id 创建；{@code staticId == 0} 时复用预置常量。
	 * Creates by race and static id; reuses prefabricated constants when {@code staticId == 0}.
	 *
	 * @param race 阵营 / race
	 * @param staticId 静态物体 id / static object id
	 *
	 * @return 忽略属性实例 / ignore properties instance
	 */
	public static IgnoreProperties of(Race race, int staticId) {
		if (staticId == 0) {
			if (race == Race.ELYOS) {
				return ELYOS;
			}
			if (race == Race.ASMODIANS) {
				return ASMODIANS;
			}
			if (race == Race.DRAKAN) {
				return BALAUR;
			}
		}
		return new IgnoreProperties(race, staticId);
	}

	/**
	 * 仅按种族创建。
	 * Creates by race only.
	 *
	 * @param race 阵营 / race
	 * @return 忽略属性实例 / ignore properties instance
	 */
	public static IgnoreProperties of(Race race) {
		return of(race, 0);
	}

	/**
	 * 仅按静态 id 创建。
	 * Creates by static id only.
	 *
	 * @param staticId 静态物体 id / static object id
	 * @return 忽略属性实例 / ignore properties instance
	 */
	public static IgnoreProperties of(int staticId) {
		return of(null, staticId);
	}

	/**
	 * 返回忽略的种族。
	 * Returns the race to ignore.
	 *
	 * @return 阵营 / race
	 */
	public Race getRace() {
		return race;
	}

	/**
	 * 返回忽略的静态物体 id。
	 * Returns the static object id to ignore.
	 *
	 * @return 静态物体 id / static object id
	 */
	public int getStaticId() {
		return staticId;
	}

	/**
	 * 调试用字符串。
	 * Debug string representation.
	 *
	 * @return 描述字符串 / description string
	 */
	@Override
	public String toString() {
		return "[IgnoreProperties] Race: " + race + " staticId: " + staticId;
	}
}
