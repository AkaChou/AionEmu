package com.aionemu.gameserver.ai2.scenario;

import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * AI2 场景默认模板，事件钩子为空实现，供具体场景子类覆盖。
 * Default AI2 scenario template with empty event hooks for concrete scenario subclasses to override.
 *
 * @author ATracer
 */
public class ScenarioTemplate implements AI2Scenario {

	/**
	 * 生物相关事件的空实现。
	 * No-op implementation for creature-related events.
	 *
	 * @param ai 当前 AI 实例 / Current AI instance
	 * @param event 事件类型 / Event type
	 * Related creature
	 */
	@Override
	public void onCreatureEvent(AbstractAI ai, AIEventType event, Creature creature) {
	}

	/**
	 * 通用事件的空实现。
	 * No-op implementation for general events.
	 *
	 * @param ai 当前 AI 实例 / Current AI instance
	 * @param event 事件类型 / Event type
	 */
	@Override
	public void onGeneralEvent(AbstractAI ai, AIEventType event) {
	}
}
