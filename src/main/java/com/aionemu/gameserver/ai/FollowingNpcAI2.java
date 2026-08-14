package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.StateEvents;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.handler.FollowEventHandler;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 跟随型 NPC AI：持续跟随指定目标移动。
 * Following NPC AI that continuously follows a designated target.
 *
 * @author Encom
 */
@AIName("following")
public class FollowingNpcAI2 extends GeneralNpcAI2
{
	/**
	 * 处理开始跟随事件。
	 * Handle start-follow.
	 *
	 * @param creature 跟随目标 / target to follow
	 */
	@Override
	protected void handleFollowMe(Creature creature) {
		FollowEventHandler.follow(this, creature);
	}
	
	/**
	 * 判断是否可处理指定 AI 事件类型。
	 * Whether the given AI event type can be handled.
	 *
	 * @param eventType AI 事件类型 / AI event type
	 * @return 是否可处理 / whether it can be handled
	 */
	@Override
	protected boolean canHandleEvent(AIEventType eventType) {
		switch (getState()) {
			case DESPAWNED:
				return StateEvents.DESPAWN_EVENTS.hasEvent(eventType);
			case DIED:
				return StateEvents.DEAD_EVENTS.hasEvent(eventType);
			case CREATED:
				return StateEvents.CREATED_EVENTS.hasEvent(eventType);
		} if (eventType == AIEventType.CREATURE_MOVED) {
				return getState() == AIState.FOLLOWING;
		}
		return true;
	}
	
	/**
	 * 处理生物移动事件。
	 * Handle creature-moved.
	 *
	 * @param creature 移动的生物 / moved creature
	 */
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature == getOwner().getTarget())
			FollowEventHandler.creatureMoved(this, creature);
	}
	
	/**
	 * 处理停止跟随事件。
	 * Handle stop-follow.
	 *
	 * @param creature 跟随目标 / followed target
	 */
	@Override
	protected void handleStopFollowMe(Creature creature) {
		FollowEventHandler.stopFollow(this, creature);
	}
}
