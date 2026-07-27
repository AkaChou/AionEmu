package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.AttackManager;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * 攻击事件处理器，负责 NPC 进入战斗、强制攻击、攻击完成与结束攻击。
 * Handles attack events: entering combat, forced attacks, attack completion, and finishing attacks.
 *
 * @author ATracer
 */
public class AttackEventHandler {

	/**
	 * 处理受到攻击：中断返回/行走，进入战斗并开始攻击目标。
	 * Handles being attacked: aborts returning/walking, enters fight, and starts attacking the target.
	 *
	 * NPC AI instance
	 * @param creature 攻击者生物 / attacking creature
	 */
	public static void onAttack(NpcAI2 npcAI, Creature creature) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onAttack");
		}
		if (creature == null || creature.getLifeStats().isAlreadyDead()) {
			return;
		}
		if (!npcAI.canThink()) {
			return;
		}
		if (npcAI.isInState(AIState.WALKING)) {
			WalkManager.stopWalking(npcAI);
		}
		npcAI.getOwner().getGameStats().renewLastAttackedTime();
		if (tryEnterFight(npcAI)) {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "onAttack() -> startAttacking");
			}
			npcAI.setSubStateIfNot(AISubState.NONE);
			npcAI.getOwner().setTarget(creature);
			AttackManager.startAttacking(npcAI);
			if (npcAI.poll(AIQuestion.CAN_SHOUT)) {
				ShoutEventHandler.onAttackBegin(npcAI, (Creature) npcAI.getOwner().getTarget());
			}
		}
	}

	static boolean tryEnterFight(NpcAI2 npcAI) {
		TargetEventHandler.clearTargetLostState(npcAI);
		return !npcAI.isInState(AIState.RETURNING) && npcAI.setStateIfNot(AIState.FIGHT);
	}

	/**
	 * 对当前目标强制发起攻击（复用 {@link #onAttack}）。
	 * Forces an attack against the current target (delegates to {@link #onAttack}).
	 *
	 * NPC AI instance
	 */
	public static void onForcedAttack(NpcAI2 npcAI) {
		onAttack(npcAI, (Creature) npcAI.getOwner().getTarget());
	}

	/**
	 * 单次攻击动作完成：刷新攻击时间并调度下一次攻击。
	 * Completes a single attack action: renews attack time and schedules the next attack.
	 *
	 * NPC AI instance
	 */
	public static void onAttackComplete(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onAttackComplete: " + npcAI.getOwner().getGameStats().getLastAttackTimeDelta());
		}
		npcAI.getOwner().getGameStats().renewLastAttackTime();
		AttackManager.scheduleNextAttack(npcAI);
	}

	/**
	 * 结束攻击流程：停止攻击表情、开始休息、清理仇恨与目标。
	 * Finishes the attack sequence: stops attack emotes, starts resting, and clears aggro/target.
	 *
	 * NPC AI instance
	 */
	public static void onFinishAttack(NpcAI2 npcAI) {
		if (!npcAI.canThink()) {
			return;
		}
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onFinishAttack");
		}
		Npc npc = npcAI.getOwner();
		npc.getMoveController().clearPathFailureContext();
		npc.getMoveController().clearPathPullAttempts();
		EmoteManager.emoteStopAttacking(npc);
		npc.getLifeStats().startResting();
		npc.getAggroList().clear();
		if (npcAI.poll(AIQuestion.CAN_SHOUT))
			ShoutEventHandler.onAttackEnd(npcAI);
		npc.setTarget(null);
		npc.setSkillNumber(0);
	}
}
