package com.aionemu.gameserver.skillengine.properties;

import java.util.Comparator;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 最大目标数属性：对区域目标按距离截断到上限数量。
 * Max-count property: trims area targets to the configured maximum by distance.
 *
 * @author MrPoke
 */
public class MaxCountProperty {

	/**
	 * 按距离优先截断区域目标列表至最大数量。
	 * Trims the area effected list to max count, preferring nearer targets.
	 *
	 * @param skill 技能上下文 / skill context
	 * @param properties 目标筛选属性 / target filter properties
	 * @return 始终 true（首要目标缺失时为 false） / true, or false if first target is missing for AREA
	 */
	public static final boolean set(final Skill skill, Properties properties) {
		TargetRangeAttribute value = properties.getTargetType();
		int maxcount = properties.getTargetMaxCount();

		switch (value) {
		case AREA:
			final Creature firstTarget = skill.getFirstTarget();
			if (firstTarget == null) {
				return false;
			}
			if (maxcount > 0 && skill.getEffectedList().size() > maxcount) {
				skill.getEffectedList().sort(Comparator.comparingDouble(creature -> MathUtil.getDistance(firstTarget, creature)));
				skill.getEffectedList().subList(maxcount, skill.getEffectedList().size()).clear();
			}
		default:
			break;
		}
		return true;
	}
}
