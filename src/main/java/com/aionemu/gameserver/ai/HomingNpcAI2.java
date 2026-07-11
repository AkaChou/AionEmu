package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AttackIntention;
import com.aionemu.gameserver.ai2.manager.AttackManager;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIAnswers;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Homing;

/**
 * 追踪弹/制导体 AI：朝目标移动并在命中或超时后处理。
 * Homing projectile AI that moves toward a target and resolves on hit or timeout.
 *
 * @author Encom
 */
@AIName("homing")
public class HomingNpcAI2 extends GeneralNpcAI2
{
	/**
	 * 执行 AI 思考循环（状态机 tick）。
	 * Run one AI think cycle (state-machine tick).
	 */
	@Override
	public void think() {
	}
	
	/**
	 * 选择下一次攻击意图（普攻/技能/换目标/结束）。
	 * Choose the next attack intention (simple/skill/switch/finish).
	 */
	@Override
	public AttackIntention chooseAttackIntention() {
		return AttackIntention.SIMPLE_ATTACK;
	}
	
	/**
	 * 处理单次攻击完成事件。
	 * Handle attack-complete.
	 */
	@Override
	protected void handleAttackComplete() {
		super.handleAttackComplete();
		Homing owner = (Homing) getOwner();
		if (owner.getActiveSkillId() != 0) {
			AttackManager.scheduleNextAttack(this);
		}
	}
	
	@Override
	protected AIAnswer pollInstance(AIQuestion question) {
		switch (question) {
			case SHOULD_DECAY:
				return AIAnswers.NEGATIVE;
			case SHOULD_RESPAWN:
				return AIAnswers.NEGATIVE;
			case SHOULD_REWARD:
				return AIAnswers.NEGATIVE;
			default:
				return null;
		}
	}
}
