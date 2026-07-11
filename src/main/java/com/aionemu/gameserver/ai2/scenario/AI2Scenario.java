package com.aionemu.gameserver.ai2.scenario;

import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * AI2 场景接口：在生物相关事件与通用事件上挂接自定义行为脚本。
 * AI2 scenario interface for attaching custom behavior scripts to creature and general events.
 *
 * @author ATracer
 */
public interface AI2Scenario {

	/**
	 * 处理与生物相关的 AI 事件。
	 * Handle a creature-related AI event.
	 *
	 * @param ai 当前 AI 实例 / Current AI instance
	 * @param event 事件类型 / Event type
	 * Related creature
	 */
	void onCreatureEvent(AbstractAI ai, AIEventType event, Creature creature);

	/**
	 * 处理通用 AI 事件（无关联生物）。
	 * Handle a general AI event (no related creature).
	 *
	 * @param ai 当前 AI 实例 / Current AI instance
	 * @param event 事件类型 / Event type
	 */
	void onGeneralEvent(AbstractAI ai, AIEventType event);
}
