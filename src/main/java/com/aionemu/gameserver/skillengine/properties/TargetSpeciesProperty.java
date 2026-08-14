package com.aionemu.gameserver.skillengine.properties;

import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 目标物种属性处理器：按 PC/NPC 过滤受影响列表。
 * Target species property handler: filters the effected list by PC/NPC.
 */
public class TargetSpeciesProperty {

	/**
	 * 按目标物种过滤受影响单位。
	 * Filters effected creatures by target species.
	 *
	 * @param skill 技能上下文 / skill context
	 * @param properties 目标筛选属性 / target filter properties
	 * @return 恒为 true / always true
	 */
	public static boolean set(final Skill skill, Properties properties) {
		TargetSpeciesAttribute value = properties.getTargetSpecies();
		final List<Creature> effectedList = skill.getEffectedList();
		switch (value) {
		case NPC:
			for (Iterator<Creature> iter = effectedList.iterator(); iter.hasNext();) {
				Creature nextEffected = iter.next();
				if (nextEffected instanceof Npc) {
					continue;
				}
				iter.remove();
			}
			break;
		case PC:
			for (Iterator<Creature> iter = effectedList.iterator(); iter.hasNext();) {
				Creature nextEffected = iter.next();
				if (nextEffected instanceof Player) {
					continue;
				}
				iter.remove();
			}
			break;
		default:
			break;
		}
		return true;
	}
}
