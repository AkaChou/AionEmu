package com.aionemu.gameserver.skillengine.properties;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.properties.Properties.CastState;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 首要目标距离属性：校验施法者与首要目标的攻击距离与视线。
 * First-target range property: validates attack range and line-of-sight to the first target.
 *
 * @author ATracer
 */
public class FirstTargetRangeProperty {

	/**
	 * 校验首要目标是否在允许距离与视线内。
	 * Validates that the first target is within allowed range and line of sight.
	 *
	 * @param skill 技能上下文 / skill context
	 * @param properties 目标筛选属性 / target filter properties
	 * @param castState 施法阶段（开始 / 结束，结束阶段可加修订距离） / cast phase (start/end; end may add revision distance)
	 * @return 距离与视线校验是否通过 / true if range and LoS checks pass
	 */
	public static boolean set(Skill skill, Properties properties, CastState castState) {
		float firstTargetRange = properties.getFirstTargetRange();
		if (!skill.isFirstTargetRangeCheck()) {
			return true;
		}

		Creature effector = skill.getEffector();
		Creature firstTarget = skill.getFirstTarget();

		if (firstTarget == null) {
			return false;
		}

		// 将武器射程加入距离 / Add Weapon Range to distance
		if (properties.isAddWeaponRange()) {
			firstTargetRange += (float) skill.getEffector().getGameStats().getAttackRange().getCurrent() / 1000f;
		}

		// 施法结束检查时添加修正距离值 / on end cast check add revision distance value
		if (!castState.isCastStart()) {
			firstTargetRange += properties.getRevisionDistance();
		}

		if (firstTarget.getObjectId() == effector.getObjectId()) {
			return true;
		}

		if (!MathUtil.isInAttackRange(effector, firstTarget, firstTargetRange + 2)) {
			if (effector instanceof Player) {
				PacketSendUtility.sendPacket((Player) effector, SM_SYSTEM_MESSAGE.STR_ATTACK_TOO_FAR_FROM_TARGET);
			}
			return false;
		}

		// 召唤小队成员异常 / Summon Group Member exception
		if (skill.getSkillTemplate().getSkillId() != 3777) { // 4.8
			if (!GameWorldServices.geoService().canSeeSkill(effector, firstTarget, skill.getSkillTemplate().getObstacle())) {
				if (effector instanceof Player) {
					PacketSendUtility.sendPacket((Player) effector, SM_SYSTEM_MESSAGE.STR_SKILL_OBSTACLE);
				}
				return false;
			}
		}
		return true;
	}
}
