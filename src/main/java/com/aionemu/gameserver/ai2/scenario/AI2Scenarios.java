package com.aionemu.gameserver.ai2.scenario;

/**
 * AI2 场景常量与工厂入口，提供空场景等预置实例。
 * AI2 scenario constants and factory entry point, providing a no-op scenario instance.
 *
 * @author ATracer
 */
public class AI2Scenarios {

	/** 空场景：不执行任何额外行为 / No-op scenario that performs no extra behavior */
	public static final AI2Scenario NO_SCENARIO = new ScenarioTemplate();
}
