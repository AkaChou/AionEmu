package com.aionemu.gameserver.skillengine.properties;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 首要目标属性处理器：按 FirstTargetAttribute 设置技能的首要目标。
 * First-target property handler: sets the skill's primary target from FirstTargetAttribute.
 */
public class FirstTargetProperty {

	/**
	 * 根据属性设置技能首要目标，并将其加入受影响列表。
	 * Sets the skill first target from properties and adds it to the effected list.
	 *
	 * @param skill 技能上下文 / skill context
	 * @param properties 目标筛选属性 / target filter properties
	 * @return 设置是否成功 / true if first target was set successfully
	 */
	public static final boolean set(Skill skill, Properties properties) {
		FirstTargetAttribute value = properties.getFirstTarget();
		skill.setFirstTargetAttribute(value);
		switch (value) {
		case ME:
			skill.setFirstTargetRangeCheck(false);
			skill.setFirstTarget(skill.getEffector());
			break;
		case TARGETORME:
			boolean changeTargetToMe = false;
			if (skill.getFirstTarget() == null) {
				skill.setFirstTarget(skill.getEffector());
			} else if (skill.getFirstTarget().isAttackableNpc()) {
				Player playerEffector = (Player) skill.getEffector();
				if (skill.getFirstTarget().isEnemy(playerEffector)) {
					changeTargetToMe = true;
				}
			} else if ((skill.getFirstTarget() instanceof Player) && (skill.getEffector() instanceof Player)) {
				Player playerEffected = (Player) skill.getFirstTarget();
				Player playerEffector = (Player) skill.getEffector();
				if (playerEffected.isEnemy(playerEffector)) {
					changeTargetToMe = true;
				}
			} else if (skill.getFirstTarget() instanceof Npc) {
				Npc npcEffected = (Npc) skill.getFirstTarget();
				Player playerEffector = (Player) skill.getEffector();
				if (npcEffected.isEnemy(playerEffector)) {
					changeTargetToMe = true;
				}
			} else if ((skill.getFirstTarget() instanceof Summon) && (skill.getEffector() instanceof Player)) {
				Summon summon = (Summon) skill.getFirstTarget();
				Player playerEffected = summon.getMaster();
				Player playerEffector = (Player) skill.getEffector();
				if (playerEffected.isEnemy(playerEffector)) {
					changeTargetToMe = true;
				}
			}
			if (changeTargetToMe) {
				if (skill.getEffector() instanceof Player) {
					PacketSendUtility.sendPacket((Player) skill.getEffector(),
							SM_SYSTEM_MESSAGE.STR_SKILL_AUTO_CHANGE_TARGET_TO_MY);
				}
				skill.setFirstTarget(skill.getEffector());
			}
			break;
		case TARGET:
			if ((skill.getSkillId() <= 8217) || (skill.getSkillId() >= 9180)) { // 5.1
				if ((skill.getSkillTemplate().getDispelCategory() != DispelCategoryType.NPC_BUFF)
						&& (skill.getSkillTemplate().getDispelCategory() != DispelCategoryType.NPC_DEBUFF_PHYSICAL)) {
					if (((skill.getFirstTarget() == null) || (skill.getFirstTarget().equals(skill.getEffector())))
							&& ((skill.getEffector() instanceof Player))) {
						if (skill.getSkillTemplate().getProperties().getTargetType() == TargetRangeAttribute.AREA) {
							return skill.getFirstTarget() != null;
						}
						TargetRelationAttribute relation = skill.getSkillTemplate().getProperties().getTargetRelation();
						if (relation != TargetRelationAttribute.ALL) {
							PacketSendUtility.sendPacket((Player) skill.getEffector(),
									SM_SYSTEM_MESSAGE.STR_SKILL_TARGET_IS_NOT_VALID);
							return false;
						}
					}
				}
			}
			break;
		case MYPET:
			Creature effector = skill.getEffector();
			if (effector instanceof Player) {
				Summon summon = ((Player) effector).getSummon();
				if (summon != null) {
					skill.setFirstTarget(summon);
				} else {
					return false;
				}
			} else {
				return false;
			}
			break;
		case MYMASTER:
			Creature peteffector = skill.getEffector();
			if (peteffector instanceof Summon) {
				Player player = ((Summon) peteffector).getMaster();
				if (player != null) {
					skill.setFirstTarget(player);
				} else {
					return false;
				}
			} else {
				return false;
			}
			break;
		case PASSIVE:
			skill.setFirstTarget(skill.getEffector());
			break;
		case TARGET_MYPARTY_NONVISIBLE:
			Creature effected = skill.getFirstTarget();
			if ((effected == null) || (skill.getEffector() == null))
				return false;
			if ((!(effected instanceof Player)) || (!(skill.getEffector() instanceof Player))
					|| (!((Player) skill.getEffector()).isInGroup2()))
				return false;
			boolean myParty = false;
			for (Player member : ((Player) skill.getEffector()).getPlayerGroup2().getMembers()) {
				if (member != skill.getEffector()) {
					if (member == effected) {
						myParty = true;
						break;
					}
				}
			}
			if (!myParty) {
				return false;
			}
			skill.setFirstTargetRangeCheck(false);
			break;
		case POINT:
			skill.setFirstTarget(skill.getEffector());
			skill.setFirstTargetRangeCheck(false);
			return true;
		default:
			break;
		}
		if (skill.getFirstTarget() != null) {
			skill.getEffectedList().add(skill.getFirstTarget());
		}
		return true;
	}
}
