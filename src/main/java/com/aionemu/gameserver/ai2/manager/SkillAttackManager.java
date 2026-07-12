package com.aionemu.gameserver.ai2.manager;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.model.skill.NpcSkillList;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillType;
import com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * NPC 技能攻击管理器：调度技能攻击、执行施法并选择下一个可用技能。
 * NPC skill-attack manager: schedules skill attacks, performs casting, and chooses the next ready skill.
 *
 * @modified Yon (Aion Reconstruction Project) -- removed extra delay from {@link #performAttack(NpcAI2, int)}
 */
public class SkillAttackManager {

	/**
	 * 执行技能攻击：校验射程后进入施法子状态，可延迟或立即释放。
	 * Performs a skill attack: validates range, enters CAST sub-state, then casts after optional delay.
	 *
	 * NPC AI instance
	 * @param delay 攻击延迟（毫秒） / attack delay in milliseconds
	 */
	public static void performAttack(NpcAI2 npcAI, int delay) {
		// 如果攻击范围为0，使用攻击范围进行检查（而不是仇恨范围）
		if (npcAI.getOwner().getObjectTemplate().getAttackRange() == 0) {
			if (npcAI.getOwner().getTarget() != null && !MathUtil.isInRange(npcAI.getOwner(),
					npcAI.getOwner().getTarget(), npcAI.getOwner().getGameStats().getAttackRange().getCurrent() / 1000f)) {
				npcAI.onGeneralEvent(AIEventType.TARGET_TOOFAR);
				npcAI.getOwner().getController().abortCast();
				return;
			}
		}
		// 设置施法子状态
		if (npcAI.setSubStateIfNot(AISubState.CAST)) {
			if (delay > 0) {
				// 延迟执行技能攻击
				GameThreadPoolServices.threadPoolManager().schedule(() -> skillAction(npcAI), delay);
			} else {
				skillAction(npcAI);
			}
		}
	}

	/**
	 * 执行技能攻击动作：BUFF 去重、使用技能或在目标无效时放弃。
	 * Executes the skill action: skips duplicate BUFF, uses skill, or gives up if the target is invalid.
	 *
	 * NPC AI instance
	 */
	protected static void skillAction(NpcAI2 npcAI) {
		Creature target = (Creature) npcAI.getOwner().getTarget();
		// 如果攻击范围为0，使用攻击范围进行检查（而不是仇恨范围）
		if (npcAI.getOwner().getObjectTemplate().getAttackRange() == 0) {
			if (npcAI.getOwner().getTarget() != null && !MathUtil.isInRange(npcAI.getOwner(),
					npcAI.getOwner().getTarget(), npcAI.getOwner().getGameStats().getAttackRange().getCurrent() / 1000f)) {
				npcAI.onGeneralEvent(AIEventType.TARGET_TOOFAR);
				npcAI.getOwner().getController().abortCast();
				return;
			}
		}
		if (target != null && !target.getLifeStats().isAlreadyDead()) {
			final int skillId = npcAI.getSkillId();
			final int skillLevel = npcAI.getSkillLevel();
			SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);
			int duration = template.getDuration();
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "Using skill " + skillId + " level: " + skillLevel + " duration: " + duration);
			}
			if (template.getSubType() == SkillSubType.BUFF) {
				if (template.getProperties().getFirstTarget() == FirstTargetAttribute.ME) {
					if (npcAI.getOwner().getEffectController().isAbnormalPresentBySkillId(skillId)) {
						afterUseSkill(npcAI);
						return;
					}
				} else {
					if (target.getEffectController().isAbnormalPresentBySkillId(skillId)) {
						afterUseSkill(npcAI);
						return;
					}
				}
			}
			boolean success = npcAI.getOwner().getController().useSkill(skillId, skillLevel);
			if (!success) {
				// 技能使用失败，结束技能
				afterUseSkill(npcAI);
			}
		} else {
			// 目标无效，放弃目标
			npcAI.setSubStateIfNot(AISubState.NONE);
			npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
		}

	}

	/**
	 * 技能使用后的处理：清除施法子状态并触发攻击完成事件。
	 * Post-skill handling: clears CAST sub-state and fires ATTACK_COMPLETE.
	 *
	 * NPC AI instance
	 */
	public static void afterUseSkill(NpcAI2 npcAI) {
		npcAI.setSubStateIfNot(AISubState.NONE);
		npcAI.onGeneralEvent(AIEventType.ATTACK_COMPLETE);
	}

	/**
	 * 选择下一个就绪技能；施法中、沉默/束缚/恐惧或 CD 未好时返回 {@code null}。
	 * Chooses the next ready skill; returns {@code null} while casting, silenced/bound/feared, or on cooldown.
	 *
	 * NPC AI instance
	 *
	 * @param npcAI
	 * @return 下一个技能条目，无可用时为 {@code null} / next skill entry, or {@code null} if none
	 */
	public static NpcSkillEntry chooseNextSkill(NpcAI2 npcAI) {
		// 如果正在施法，不选择技能
		if (npcAI.isInSubState(AISubState.CAST)) {
			return null;
		}
		Npc owner = npcAI.getOwner();
		NpcSkillList skillList = owner.getSkillList();
		if (skillList == null || skillList.size() == 0) {
			return null;
		}
		if (owner.getGameStats().canUseNextSkill()) {
			NpcSkillEntry npcSkill = skillList.getRandomSkill();
			if (npcSkill != null) {
				int currentHpPercent = owner.getLifeStats().getHpPercentage();
				if (npcSkill.isReady(currentHpPercent,
						System.currentTimeMillis() - owner.getGameStats().getFightStartingTime())) {
					SkillTemplate template = npcSkill.getSkillTemplate();
					// 检查技能使用条件
					if ((template.getType() == SkillType.MAGICAL
							&& owner.getEffectController().isAbnormalSet(AbnormalState.SILENCE))
							|| (template.getType() == SkillType.PHYSICAL
									&& owner.getEffectController().isAbnormalSet(AbnormalState.BIND))
							|| (owner.getEffectController().isUnderFear()))
						return null;
					npcSkill.setLastTimeUsed();
					return npcSkill;
				}
			}
		}
		return null;
	}
}
