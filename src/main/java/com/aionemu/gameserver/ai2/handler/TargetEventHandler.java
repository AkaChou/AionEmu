package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.AttackManager;
import com.aionemu.gameserver.ai2.manager.FollowManager;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcPathBehaviorData;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORCED_MOVE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 目标事件处理器，负责到达目标、目标过远、放弃目标与切换目标。
 * Handles target events: target reached, target too far, give-up, and target change.
 *
 * @author ATracer
 */
public class TargetEventHandler {

	/**
	 * 到达目标位置：按当前 AI 状态调度攻击、归家、行走或停止移动。
	 * On target reached: schedules attack, return-home, walking, or aborts move based on AI state.
	 *
	 * NPC AI instance
	 */
	public static void onTargetReached(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onTargetReached");
		}

		AIState currentState = npcAI.getState();
		switch (currentState) {
			case FIGHT:
				npcAI.getOwner().getMoveController().abortMove();
				AttackManager.scheduleNextAttack(npcAI);
				if (npcAI.getOwner().getMoveController().isFollowingTarget())
					npcAI.getOwner().getMoveController().storeStep();
				break;
			case RETURNING:
				boolean reachedReturnDestination = npcAI.getOwner().getMoveController().isHomeReturnDestinationReached();
				npcAI.getOwner().getMoveController().abortMove();
				if (reachedReturnDestination)
					npcAI.onGeneralEvent(AIEventType.BACK_HOME);
				else
					npcAI.onGeneralEvent(AIEventType.NOT_AT_HOME);
				break;
			case WALKING:
				WalkManager.targetReached(npcAI);
				checkAggro(npcAI);
				break;
			case FOLLOWING:
				npcAI.getOwner().getMoveController().abortMove();
				npcAI.getOwner().getMoveController().storeStep();
				break;
			case FEAR: //TO DO remove this state
				npcAI.getOwner().getMoveController().abortMove();
				npcAI.getOwner().getMoveController().storeStep();
			break;
		}
	}

	/**
	 * 目标过远：战斗时由攻击管理器处理，跟随时由跟随管理器处理。
	 * Target too far: attack manager for fight, follow manager for following.
	 *
	 * NPC AI instance
	 */
	public static void onTargetTooFar(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onTargetTooFar");
		}
		switch (npcAI.getState()) {
			case FIGHT:
				AttackManager.targetTooFar(npcAI);
				break;
			case FOLLOWING:
				FollowManager.targetTooFar(npcAI);
				break;
			case FEAR:
				break;
			default:

			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "default onTargetTooFar");
			}
		}
	}

	/**
	 * 放弃目标：停止仇恨、中止移动并重新思考。
	 * Gives up the target: stops hating, aborts move, and re-thinks.
	 *
	 * NPC AI instance
	 */
	public static void onTargetGiveup(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onTargetGiveup");
		}
		VisibleObject target = npcAI.getOwner().getTarget();
		if (target != null) {
			if (npcAI.isInSubState(AISubState.TARGET_LOST)) {
				npcAI.setSubStateIfNot(AISubState.NONE);
			}
			npcAI.getOwner().getAggroList().stopHating(target);
		}
		if (npcAI.isMoveSupported()) {
			npcAI.getOwner().getMoveController().abortMove();
		}
		if (!npcAI.isAlreadyDead()) {
			if (npcAI.getOwner().getMoveController().isReturningToWaypoint()) {
				npcAI.onGeneralEvent(AIEventType.ATTACK_FINISH);
				npcAI.onGeneralEvent(AIEventType.NOT_AT_HOME);
			} else {
				npcAI.think();
			}
		}
	}

	/** 按真端 NPC 数据处理连续寻路失败。 */
	public static void onPathFindFailed(NpcAI2 npcAI) {
		if (!npcAI.isInState(AIState.FIGHT)) {
			return;
		}
		Npc owner = npcAI.getOwner();
		Creature target = owner.getTarget() instanceof Creature creature ? creature : null;
		NpcPathBehaviorData.Behavior behavior = DataManager.NPC_PATH_BEHAVIOR_DATA == null ? null
				: DataManager.NPC_PATH_BEHAVIOR_DATA.get(owner.getNpcId());
		NpcPathBehaviorData.PathfindFailReaction reaction = behavior == null || behavior.pathfindFailReaction() == null
				? NpcPathBehaviorData.PathfindFailReaction.RETURN_TO_SP : behavior.pathfindFailReaction();
		switch (reaction) {
			case PULL_TARGET:
				if (target != null && owner.getMoveController().tryPathPull(target.getObjectId())) {
					pullTarget(owner, target);
					owner.getMoveController().clearPathFailureContext();
					return;
				}
				returnToSpawn(npcAI);
				break;
			case ABANDON_TARGET:
				owner.getMoveController().clearPathFailureContext();
				if (target != null) {
					owner.getAggroList().remove(target);
				}
				owner.getMoveController().abortMove();
				if (!npcAI.isAlreadyDead()) {
					npcAI.think();
				}
				break;
			case RETURN_TO_SP:
				returnToSpawn(npcAI);
				break;
		}
	}

	private static void returnToSpawn(NpcAI2 npcAI) {
		returnToSpawn(npcAI, true);
	}

	public static void returnToSpawn(NpcAI2 npcAI, boolean restoreHp) {
		Npc owner = npcAI.getOwner();
		owner.getMoveController().clearHomeReturn();
		if (restoreHp) {
			owner.getMoveController().requestFullHealOnHomeReturn();
		}
		owner.getAggroList().clear();
		owner.getMoveController().abortMove();
		if (!npcAI.isAlreadyDead()) {
			npcAI.onGeneralEvent(AIEventType.ATTACK_FINISH);
			npcAI.onGeneralEvent(owner.isAtSpawnLocation() ? AIEventType.BACK_HOME : AIEventType.NOT_AT_HOME);
		}
	}

	private static void pullTarget(Npc owner, Creature target) {
		double radians = Math.toRadians(MathUtil.convertHeadingToDegree(owner.getHeading()));
		float x = owner.getX() + (float) Math.cos(radians);
		float y = owner.getY() + (float) Math.sin(radians);
		float z = owner.getZ();
		if (!GameWorldServices.pathService().canReachWaypoint(owner, x, y, z)) {
			x = owner.getX();
			y = owner.getY();
		}
		byte heading = (byte) Math.floorMod(owner.getHeading() + 60, 120);
		target.getMoveController().abortMove();
		GameWorldBootstrapServices.world().updatePosition(target, x, y, z, heading);
		PacketSendUtility.broadcastPacketAndReceive(target, new SM_FORCED_MOVE(owner, target.getObjectId(), x, y, z));
	}

	/**
	 * 战斗中切换目标并调度下一次攻击。
	 * Changes target during fight and schedules the next attack.
	 *
	 * NPC AI instance
	 * new target
	 */
	public static void onTargetChange(NpcAI2 npcAI, Creature creature) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onTargetChange");
		}
		if (npcAI.isInState(AIState.FIGHT)) {
			if (!npcAI.getOwner().isTargeting(creature.getObjectId())) {
				npcAI.getOwner().getMoveController().clearPathFailureContext();
				npcAI.getOwner().getMoveController().clearPathPullAttempts();
			}
			npcAI.getOwner().setTarget(creature);
			AttackManager.scheduleNextAttack(npcAI);
		}
	}

	/**
	 * 行走到达后，对已知列表中的生物重新检查仇恨。
	 * After walking arrival, re-checks aggro against known creatures.
	 *
	 * NPC AI instance
	 */
	private static void checkAggro(NpcAI2 npcAI) {
		for (VisibleObject obj : npcAI.getOwner().getKnownList().getKnownObjectsSnapshot()) {
			if (obj instanceof Creature) {
				CreatureEventHandler.checkAggro(npcAI, (Creature) obj);
			}
		}
	}
}
