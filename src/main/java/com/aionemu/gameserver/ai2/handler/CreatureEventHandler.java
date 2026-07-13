package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.AttackManager;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 生物感知事件处理器，负责生物移动/可见时的仇恨检查与任务距离触发。
 * Handles creature perception events: aggro checks and quest distance triggers on move/see.
 *
 * @author ATracer
 */
public class CreatureEventHandler {

	/**
	 * 生物在已知列表中移动时触发：检查仇恨，并对玩家触发距离任务。
	 * Fired when a creature moves in the known list: checks aggro and triggers distance quests for players.
	 *
	 * NPC AI instance
	 * @param creature 移动的生物 / moving creature
	 */
	public static void onCreatureMoved(NpcAI2 npcAI, Creature creature) {
		checkAggro(npcAI, creature);
		if (creature instanceof Player) {
			Player player = (Player) creature;
			GameEngineServices.questEngine().onAtDistance(new QuestEnv(npcAI.getOwner(), player, 0, 0));
		}
	}

	/**
	 * 生物进入视野时触发：恢复丢失目标、检查仇恨，并对玩家触发距离任务。
	 * Fired when a creature enters sight: recovers lost target, checks aggro, and triggers distance quests for players.
	 *
	 * NPC AI instance
	 * @param creature 进入视野的生物 / creature that became visible
	 */
	public static void onCreatureSee(NpcAI2 npcAI, Creature creature) {
		if (npcAI.isInSubState(AISubState.TARGET_LOST) && creature.equals(npcAI.getTarget())) {
			npcAI.setSubStateIfNot(AISubState.NONE);
			if (npcAI.isInState(AIState.FIGHT)) {
				AttackManager.scheduleNextAttack(npcAI);
				return;
			}
		}
		checkAggro(npcAI, creature);
		if (creature instanceof Player) {
			Player player = (Player) creature;
			GameEngineServices.questEngine().onAtDistance(new QuestEnv(npcAI.getOwner(), player, 0, 0));
		}
	}

	/**
	 * 检查生物是否触发仇恨（喊话范围、仇恨范围、敌对与视线）。
	 * Checks whether the creature should trigger aggro (shout range, aggro range, hostility, and line of sight).
	 *
	 * NPC AI instance
	 * @param creature 待检查的生物 / creature to evaluate
	 */
	protected static void checkAggro(NpcAI2 ai, Creature creature) {
		Npc owner = ai.getOwner();

		if (ai.isInState(AIState.FIGHT)) {
			return;
		}
		if (creature.getLifeStats().isAlreadyDead()) {
			return;
		}
		if (!owner.canSee(creature)) {
			return;
		}
		if (!owner.getActiveRegion().isMapRegionActive()) {
			return;
		}

		boolean isInAggroRange = false;
		if (ai.poll(AIQuestion.CAN_SHOUT)) {
			int shoutRange = owner.getObjectTemplate().getMinimumShoutRange();
			double distance = MathUtil.getDistance(owner, creature);
			if (distance <= shoutRange) {
				ShoutEventHandler.onSee(ai, creature);
				isInAggroRange = shoutRange <= owner.getAggroRange();
			}
		}

		if (!ai.isInState(AIState.FIGHT) && (isInAggroRange
				|| MathUtil.isIn3dRange(owner, creature, owner.getAggroRange() * 1.6f))) { // 1.6 is for aggro range correction
			if (owner.isAggressiveTo(creature) && GameWorldServices.geoService().canSee(owner, creature)) {
				if (!ai.isInState(AIState.RETURNING)) {
					ai.getOwner().getMoveController().storeStep();
				}
				if (ai.canThink()) {
					ai.onCreatureEvent(AIEventType.CREATURE_AGGRO, creature);
				}
			}
		}
	}
}
