package com.aionemu.gameserver.ai2;

import java.util.Arrays;
import java.util.EnumSet;

import com.aionemu.gameserver.ai2.event.AIEventType;

/**
 * 状态允许的事件集合：限制特定 AI 状态下可处理的事件类型。
 * Allowed event sets per AI state: restricts which event types can be handled in a given state.
 *
 * @author ATracer
 */
public enum StateEvents {
	/** 创建状态允许的事件 / Events allowed in CREATED state */
	CREATED_EVENTS(AIEventType.SPAWNED),
	/** 消失状态允许的事件 / Events allowed in DESPAWNED state */
	DESPAWN_EVENTS(AIEventType.RESPAWNED, AIEventType.SPAWNED),
	/** 死亡状态允许的事件 / Events allowed in DIED state */
	DEAD_EVENTS(AIEventType.DESPAWNED, AIEventType.DROP_REGISTERED);

	private EnumSet<AIEventType> events;

	private StateEvents(AIEventType... aiEventTypes) {
		this.events = EnumSet.copyOf(Arrays.asList(aiEventTypes));
	}

	/**
	 * 判断该状态是否允许指定事件。
	 * Returns whether the given event is allowed for this state set.
	 *
	 * @param event 事件类型 / event type
	 * @return 是否包含 / whether contained
	 */
	public boolean hasEvent(AIEventType event) {
		return events.contains(event);
	}
}
