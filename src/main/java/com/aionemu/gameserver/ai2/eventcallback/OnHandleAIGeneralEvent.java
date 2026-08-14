package com.aionemu.gameserver.ai2.eventcallback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.event.AIEventType;

/**
 * 当 AI 处理通用事件时广播的回调基类，分别在调用前后触发子类钩子。
 * Base callback broadcast when a general AI event is handled, invoking subclass hooks before and after the call.
 *
 * @author SoulKeeper
 */
@SuppressWarnings("rawtypes")
public abstract class OnHandleAIGeneralEvent implements Callback<AbstractAI> {

	/**
	 * 方法调用前：解析事件类型并触发 {@link #onBeforeHandleGeneralEvent}。
	 * Before method call: resolve the event type and invoke {@link #onBeforeHandleGeneralEvent}.
	 *
	 * @param obj 被回调的 AI 实例 / AI instance being called back
	 * @param args 方法参数，首项为 {@link AIEventType} / Method args; first is {@link AIEventType}
	 * @return 继续执行的回调结果 / Callback result that continues execution
	 */
	@Override
	public CallbackResult beforeCall(AbstractAI obj, Object[] args) {
		AIEventType eventType = (AIEventType) args[0];
		onBeforeHandleGeneralEvent(obj, eventType);
		return CallbackResult.newContinue();
	}

	/**
	 * 方法调用后：解析事件类型并触发 {@link #onAfterHandleGeneralEvent}。
	 * After method call: resolve the event type and invoke {@link #onAfterHandleGeneralEvent}.
	 *
	 * @param obj 被回调的 AI 实例 / AI instance being called back
	 * @param args 方法参数，首项为 {@link AIEventType} / Method args; first is {@link AIEventType}
	 * @param methodResult 方法返回值 / Method return value
	 * @return 继续执行的回调结果 / Callback result that continues execution
	 */
	@Override
	public CallbackResult afterCall(AbstractAI obj, Object[] args, Object methodResult) {
		AIEventType eventType = (AIEventType) args[0];
		onAfterHandleGeneralEvent(obj, eventType);
		return CallbackResult.newContinue();
	}

	/**
	 * 返回本回调的基类类型，用于回调系统注册与匹配。
	 * Return this callback's base class type for registration and matching.
	 *
	 * @return 本回调的基类类型 / base callback class type
	 */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return OnHandleAIGeneralEvent.class;
	}

	/**
	 * 通用事件处理前的钩子，由子类实现。
	 * Hook before general-event handling, implemented by subclasses.
	 *
	 * @param obj AI 实例 / AI instance
	 * @param eventType 事件类型 / Event type
	 */
	protected abstract void onBeforeHandleGeneralEvent(AbstractAI obj, AIEventType eventType);

	/**
	 * 通用事件处理后的钩子，由子类实现。
	 * Hook after general-event handling, implemented by subclasses.
	 *
	 * @param obj AI 实例 / AI instance
	 * @param eventType 事件类型 / Event type
	 */
	protected abstract void onAfterHandleGeneralEvent(AbstractAI obj, AIEventType eventType);
}
