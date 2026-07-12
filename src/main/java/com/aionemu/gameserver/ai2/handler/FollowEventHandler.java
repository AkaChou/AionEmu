package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 跟随事件处理器，负责 NPC 跟随、距离校验与停止跟随。
 * Handles follow events: start following, range validation, and stop following.
 *
 * @author ATracer
 */
public class FollowEventHandler {

	/**
	 * 开始跟随指定生物：切换到 FOLLOWING 状态并播放跟随表情。
	 * Starts following a creature: switches to FOLLOWING state and plays the follow emote.
	 *
	 * NPC AI instance
	 * creature to follow
	 */
	public static void follow(NpcAI2 npcAI, Creature creature) {
		if (npcAI.setStateIfNot(AIState.FOLLOWING)) {
			npcAI.getOwner().setTarget(creature);
			EmoteManager.emoteStartFollowing(npcAI.getOwner());
		}
	}

	/**
	 * 跟随目标移动时触发：若仍在跟随该目标则校验距离。
	 * Fired when the follow target moves: validates range while still targeting that creature.
	 *
	 * NPC AI instance
	 * @param creature 移动的目标生物 / moving target creature
	 */
	public static void creatureMoved(NpcAI2 npcAI, Creature creature) {
		if (npcAI.isInState(AIState.FOLLOWING)) {
			if (npcAI.getOwner().isTargeting(creature.getObjectId()) && !creature.getLifeStats().isAlreadyDead()) {
				checkFollowTarget(npcAI, creature);
			}
		}
	}

	/**
	 * 检查跟随目标是否过远，过远则触发 TARGET_TOOFAR 事件。
	 * Checks whether the follow target is too far; fires TARGET_TOOFAR if out of range.
	 *
	 * NPC AI instance
	 * follow target
	 */
	public static void checkFollowTarget(NpcAI2 npcAI, Creature creature) {
		if (!isInRange(npcAI, creature)) {
			npcAI.onGeneralEvent(AIEventType.TARGET_TOOFAR);
		}
	}

	/**
	 * 判断跟随目标是否在允许距离内（副本 / 残血 / 满血使用不同半径）。
	 * low HP / full HP).
	 *
	 * @param ai AI 实例 / AI instance
	 * @param object 目标可见对象 / target visible object
	 * @return 是否在范围内 / whether in range
	 */
	public static boolean isInRange(AbstractAI ai, VisibleObject object) {
		if (object == null) {
			return false;
		}
		if (object.isInInstance()) {
			return MathUtil.isIn3dRange(ai.getOwner(), object, 9999);
		} else if (ai.getOwner().getLifeStats().getHpPercentage() < 100) {
			return MathUtil.isIn3dRange(ai.getOwner(), object, 30);
		} else {
			return MathUtil.isIn3dRange(ai.getOwner(), object, 15);
		}
	}

	/**
	 * 停止跟随：切回空闲、清空目标、中止移动并删除 / 安排重生。
	 * schedules respawn.
	 *
	 * NPC AI instance
	 * @param creature 停止跟随的目标 / creature no longer followed
	 */
	public static void stopFollow(NpcAI2 npcAI, Creature creature) {
		if (npcAI.setStateIfNot(AIState.IDLE)) {
			npcAI.getOwner().setTarget(null);
			npcAI.getOwner().getMoveController().abortMove();
			npcAI.getOwner().getController().scheduleRespawn();
			npcAI.getOwner().getController().onDelete();
		}
	}
}
