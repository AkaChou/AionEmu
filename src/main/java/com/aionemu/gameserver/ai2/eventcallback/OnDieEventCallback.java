package com.aionemu.gameserver.ai2.eventcallback;

import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.event.AIEventType;

/**
 * 针对 {@link AIEventType#DIED} 事件的回调，在死亡处理前后分别触发钩子。
 * Callback for the {@link AIEventType#DIED} event, invoking hooks before and after death handling.
 *
 * @author SoulKeeper
 */
public abstract class OnDieEventCallback extends OnHandleAIGeneralEvent {

	/**
	 * 通用事件处理前：仅在事件为 {@link AIEventType#DIED} 时调用 {@link #onBeforeDie}。
	 * Before general-event handling: call {@link #onBeforeDie} only when the event is {@link AIEventType#DIED}.
	 *
	 * AI instance
	 * Event type
	 */
	@Override
	protected void onBeforeHandleGeneralEvent(AbstractAI obj, AIEventType eventType) {
		if (AIEventType.DIED == eventType) {
			onBeforeDie(obj);
		}
	}

	/**
	 * 通用事件处理后：仅在事件为 {@link AIEventType#DIED} 时调用 {@link #onAfterDie}。
	 * After general-event handling: call {@link #onAfterDie} only when the event is {@link AIEventType#DIED}.
	 *
	 * AI instance
	 * Event type
	 */
	@Override
	protected void onAfterHandleGeneralEvent(AbstractAI obj, AIEventType eventType) {
		if (AIEventType.DIED == eventType) {
			onAfterDie(obj);
		}
	}

	/**
	 * 死亡处理前的钩子。
	 * Hook invoked before death handling.
	 *
	 * @param obj 死亡的 AI 实例 / Dying AI instance
	 */
	public abstract void onBeforeDie(AbstractAI obj);

	/**
	 * 死亡处理后的钩子。
	 * Hook invoked after death handling.
	 *
	 * @param obj 已死亡的 AI 实例 / Dead AI instance
	 */
	public abstract void onAfterDie(AbstractAI obj);
}
