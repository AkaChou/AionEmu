package com.aionemu.gameserver.skillengine.properties;

import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 目标关系属性处理器：按敌友/队伍关系过滤受影响列表。
 * Target relation property handler: filters the effected list by enemy/friend/party relation.
 */
public class TargetRelationProperty {

	/**
	 * 按目标关系过滤受影响单位，必要时回退为自身。
	 * Filters effected creatures by relation; may fall back to self.
	 *
	 * @param skill 技能上下文 / skill context
	 * @param properties 目标筛选属性 / target filter properties
	 * @return 恒为 true / always true
	 */
	public static boolean set(Skill skill, Properties properties) {
		TargetRelationAttribute value = properties.getTargetRelation();
		final List<Creature> effectedList = skill.getEffectedList();
		boolean isMaterialSkill = DataManager.MATERIAL_DATA.isMaterialSkill(skill.getSkillId());
		Creature effector = skill.getEffector();
		switch (value) {
		case ALL:
			break;
		case ENEMY:
			for (Iterator<Creature> iter = effectedList.iterator(); iter.hasNext();) {
				Creature nextEffected = iter.next();
				if (effector.isEnemy(nextEffected) || isMaterialSkill) {
					continue;
				}
				iter.remove();
			}
			break;
		case FRIEND:
			for (Iterator<Creature> iter = effectedList.iterator(); iter.hasNext();) {
				Creature nextEffected = iter.next();
				if (!effector.isEnemy(nextEffected) && isBuffAllowed(nextEffected) || isMaterialSkill) {
					continue;
				}
				iter.remove();
			}
			if (effectedList.isEmpty()) {
				skill.setFirstTarget(skill.getEffector());
				effectedList.add(skill.getEffector());
			} else {
				skill.setFirstTarget(effectedList.get(0));
			}
			break;
		case MYPARTY:
			for (Iterator<Creature> iter = effectedList.iterator(); iter.hasNext();) {
				Creature nextEffected = iter.next();
				Player player = null;
				if (nextEffected instanceof Player) {
					player = (Player) nextEffected;
				} else if (nextEffected instanceof Summon) {
					Summon playerSummon = (Summon) nextEffected;
					if (playerSummon.getMaster() != null) {
						player = playerSummon.getMaster();
					}
				}
				if (player != null) {
					if (effector instanceof Servant) {
						effector = ((Servant) effector).getMaster();
					}
					Player playerEffector = (Player) effector;
					if (playerEffector.isInAlliance2() && player.isInAlliance2()) {
						if (playerEffector.getPlayerAlliance2().getObjectId()
								.equals(player.getPlayerAlliance2().getObjectId())) {
							continue;
						}
					} else if (playerEffector.isInGroup2() && player.isInGroup2()) {
						if (playerEffector.getPlayerGroup2().getTeamId().equals(player.getPlayerGroup2().getTeamId())) {
							continue;
						}
					}
				}
				iter.remove();
			}
			if (effectedList.isEmpty()) {
				skill.setFirstTarget(effector);
				effectedList.add(effector);
			} else {
				skill.setFirstTarget(effectedList.get(0));
			}
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * 判断目标是否允许被增益（排除部分攻城 NPC 类型）。
	 * Returns whether the target may receive buffs (excludes some siege NPC types).
	 *
	 * @param effected 被施法目标 / effected creature
	 * @return 是否允许增益 / true if buff is allowed
	 */
	public static boolean isBuffAllowed(Creature effected) {
		if (effected instanceof SiegeNpc) {
			switch (((SiegeNpc) effected).getObjectTemplate().getAbyssNpcType()) {
			case ARTIFACT:
			case ARTIFACT_EFFECT_CORE:
			case DOOR:
			case DOOR_REPAIR:
				return false;
			default:
				break;
			}
		}
		return true;
	}
}
