package com.aionemu.gameserver.skillengine.properties;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PositionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;

import java.util.List;

/**
 * 目标范围属性处理器：按范围类型填充技能受影响列表（AOE/队伍/坐标点等）。
 * Target range property handler: fills the skill effected list for AOE/party/point ranges.
 *
 * @author ATracer
 */
@Slf4j
public class TargetRangeProperty {


	/**
	 * 按目标范围类型收集受影响单位。
	 * Collects effected creatures according to the target range type.
	 *
	 * @param skill 技能上下文 / skill context
	 * @param properties 目标筛选属性 / target filter properties
	 * @return 收集是否成功 / true if targets were collected successfully
	 */
	public static final boolean set(final Skill skill, Properties properties) {

		TargetRangeAttribute value = properties.getTargetType();
		int distance = properties.getTargetDistance();
		int maxcount = properties.getTargetMaxCount();

		final List<Creature> effectedList = skill.getEffectedList();
		skill.setTargetRangeAttribute(value);
		switch (value) {
		case ONLYONE:
			break;
		case AREA:
			final Creature firstTarget = skill.getFirstTarget();

			if (firstTarget == null) {
				log.warn(I18n.get("log.586970ae9238", skill.getSkillTemplate().getSkillId()));
				return false;
			}

			// 【重要修复】使用施法者的已知对象列表，确保 AOE 技能能正确检测到附近的 NPC
			// 修复前：使用 firstTarget.getKnownList()，当 firstTarget != effector 时，可能导致 NPC 太贴近玩家反而不会被 AOE 打中
			// 修复后：使用 skill.getEffector().getKnownList()，确保始终使用施法者的已知对象列表
			List<VisibleObject> areaKnownObjects = skill.getEffector().getKnownList().getKnownObjectsSnapshot();
			for (VisibleObject nextCreature : areaKnownObjects) {
				if (((nextCreature instanceof Creature)) && (firstTarget != nextCreature)
						&& (((Creature) nextCreature).getLifeStats() != null)
						&& (!((Creature) nextCreature).getLifeStats().isAlreadyDead())
						&& ((!(skill.getEffector() instanceof Trap))
								|| (((Trap) skill.getEffector()).getCreator() != nextCreature))
						&& ((!(nextCreature instanceof Player)) || (!((Player) nextCreature).isProtectionActive()))) {
					if (skill.isPointSkill()) {
						float targetCollision = firstTarget.getObjectTemplate().getBoundRadius().getCollision();
						float creatureCollision = ((Creature) nextCreature).getObjectTemplate().getBoundRadius().getCollision();
						if (MathUtil.isIn3dRange(skill.getX(), skill.getY(), skill.getZ(), nextCreature.getX(),
								nextCreature.getY(), nextCreature.getZ(), distance + targetCollision + creatureCollision + 1)) {
							if (skill.shouldAffectTarget(nextCreature)) {
								skill.getEffectedList().add((Creature) nextCreature);
							}
						}
					} else if (properties.getEffectiveWidth() > 0) {
						float targetCollision = firstTarget.getObjectTemplate().getBoundRadius().getCollision();
						float creatureCollision = ((Creature) nextCreature).getObjectTemplate().getBoundRadius().getCollision();
						if (MathUtil.isInsideAttackCylinder(firstTarget, nextCreature,
								(int) (distance + targetCollision + creatureCollision),
								(int) (properties.getEffectiveWidth() + targetCollision + creatureCollision),
								!properties.isBackDirection())) {
							if (skill.shouldAffectTarget(nextCreature)) {
								skill.getEffectedList().add((Creature) nextCreature);
							}
						}
					} else if (properties.getEffectiveAngle() > 0) {
						float angle = properties.getEffectiveAngle() / 2.0F;
						if (properties.isBackDirection()) {
							angle = 180.0F - angle;
						}
						Range<Float> range = Range.of(angle, 360.0F - angle);
						if (range.contains(PositionUtil.getAngleToTarget(firstTarget, nextCreature))) {
							float targetCollision = firstTarget.getObjectTemplate().getBoundRadius().getCollision();
							float creatureCollision = ((Creature) nextCreature).getObjectTemplate().getBoundRadius().getCollision();
							if (MathUtil.isIn3dRange(firstTarget, nextCreature,
									distance + targetCollision + creatureCollision)) {
								if (skill.shouldAffectTarget(nextCreature)) {
									skill.getEffectedList().add((Creature) nextCreature);
								}
							}
						}
					} else {
						float targetCollision = firstTarget.getObjectTemplate().getBoundRadius().getCollision();
						float creatureCollision = ((Creature) nextCreature).getObjectTemplate().getBoundRadius().getCollision();
						if (MathUtil.isIn3dRange(firstTarget, nextCreature,
								distance + targetCollision + creatureCollision)) {
							if (skill.shouldAffectTarget(nextCreature)) {
								skill.getEffectedList().add((Creature) nextCreature);
							}
						}
					}
				}
			}
			break;
		case PARTY:
			// 保镖(417) 的修复 / fix for Bodyguard(417)
			if (maxcount == 1)
				break;
			int partyCount = 0;
			if (skill.getEffector() instanceof Player) {
				Player effector = (Player) skill.getEffector();
				if (effector.isInAlliance2()) {
					effectedList.clear();
					for (Player player : effector.getPlayerAllianceGroup2().getMembers()) {
						if (partyCount >= 6 || partyCount >= maxcount) {
							break;
						}
						if (!player.isOnline()) {
							continue;
						}
						if (MathUtil.isIn3dRange(effector, player, distance + 1)) {
							effectedList.add(player);
							partyCount++;
						}
					}
				} else if (effector.isInGroup2()) {
					effectedList.clear();
					for (Player member : effector.getPlayerGroup2().getMembers()) {
						if (partyCount >= maxcount) {
							break;
						}
						if (member != null && MathUtil.isIn3dRange(effector, member, distance + 1)) {
							effectedList.add(member);
							partyCount++;
						}
					}
				}
			}
			break;
		case PARTY_WITHPET:
			if (skill.getEffector() instanceof Player) {
				final Player effector = (Player) skill.getEffector();
				if (effector.isInAlliance2()) {
					effectedList.clear();
					for (Player player : effector.getPlayerAllianceGroup2().getMembers()) {
						if (!player.isOnline()) {
							continue;
						}
						if (player.getLifeStats().isAlreadyDead()) {
							continue;
						}
						if (MathUtil.isIn3dRange(effector, player, distance + 1)) {
							effectedList.add(player);
							Summon aMemberSummon = player.getSummon();
							if (aMemberSummon != null) {
								effectedList.add(aMemberSummon);
							}
						}
					}
				} else if (effector.isInGroup2()) {
					effectedList.clear();
					for (Player member : effector.getPlayerGroup2().getMembers()) {
						if (!member.isOnline()) {
							continue;
						}
						if (member.getLifeStats().isAlreadyDead()) {
							continue;
						}
						if (MathUtil.isIn3dRange(effector, member, distance + 1)) {
							effectedList.add(member);
							Summon aMemberSummon = member.getSummon();
							if (aMemberSummon != null) {
								effectedList.add(aMemberSummon);
							}
						}
					}
				}
			}
			break;
		case POINT:
			List<VisibleObject> pointKnownObjects = skill.getEffector().getKnownList().getKnownObjectsSnapshot();
			for (VisibleObject nextCreature : pointKnownObjects) {
				if (!(nextCreature instanceof Creature)) {
					continue;
				}
				if (((Creature) nextCreature).getLifeStats().isAlreadyDead()) {
					continue;
				}
				// 闪烁状态的玩家不计入 / Players in blinking state must not be counted
				if ((nextCreature instanceof Player) && (((Player) nextCreature).isProtectionActive())) {
					continue;
				}
				float creatureCollision = ((Creature) nextCreature).getObjectTemplate().getBoundRadius().getCollision();
				if (MathUtil.getDistance(skill.getX(), skill.getY(), skill.getZ(), nextCreature.getX(),
						nextCreature.getY(), nextCreature.getZ()) <= distance + creatureCollision + 1) {
					if (skill.shouldAffectTarget(nextCreature)) {
						effectedList.add((Creature) nextCreature);
					}
				}
			}
			break;
		}
		return true;
	}
}
