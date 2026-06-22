/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.ai2.manager;

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
import com.aionemu.gameserver.skillengine.model.SkillType;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.ThreadPoolManager;
/**
 * NPC技能攻击管理器
 * 负责处理NPC的技能攻击调度和攻击逻辑
 * 修复：使用正确的攻击范围进行检查，防止技能使用时范围错误
 * @modified Yon (Aion Reconstruction Project) -- removed extra delay from {@link #performAttack(NpcAI2, int)}
*/
public class SkillAttackManager {

	/**
	 * 执行技能攻击
	 * @param npcAI
	 * @param delay 攻击延迟时间（毫秒）
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
				ThreadPoolManager.getInstance().schedule(new SkillAction(npcAI), delay);
			} else {
				skillAction(npcAI);
			}
		}
	}

	/**
	 * 执行技能攻击动作
	 * @param npcAI
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
			switch (template.getSubType()) {
			case BUFF:
				switch (template.getProperties().getFirstTarget()) {
				case ME:
					if (npcAI.getOwner().getEffectController().isAbnormalPresentBySkillId(skillId)) {
						afterUseSkill(npcAI);
						return;
					}
					break;
				default:
					if (target.getEffectController().isAbnormalPresentBySkillId(skillId)) {
						afterUseSkill(npcAI);
						return;
					}
				}
				break;
			default:
				break;
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
	 * 技能使用后的处理
	 * @param npcAI
	 */
	public static void afterUseSkill(NpcAI2 npcAI) {
		npcAI.setSubStateIfNot(AISubState.NONE);
		npcAI.onGeneralEvent(AIEventType.ATTACK_COMPLETE);
	}

	/**
	 * 选择下一个技能
	 * @param npcAI
	 * @return 下一个技能，如果没有则返回null
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

	// 技能攻击动作：执行实际的技能攻击
	private final static class SkillAction implements Runnable {
		private NpcAI2 npcAI;

		SkillAction(NpcAI2 npcAI) {
			this.npcAI = npcAI;
		}

		@Override
		public void run() {
			skillAction(npcAI);
			npcAI = null;
		}
	}
}