package com.aionemu.gameserver.services.vortexservice;

import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.eventcallback.OnDieEventCallback;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

/**
 * 次元漩涡生成器死亡监听器：摧毁后结束对应入侵。
 * Death listener for the dimensional-vortex generator; ends the matching invasion when destroyed.
 *
 * @author Rinzler (Encom)
 */
@SuppressWarnings("rawtypes")
public class GeneratorDestroyListener extends OnDieEventCallback {

	private final DimensionalVortex<?> vortex;

	/**
	 * 绑定所属漩涡事件。
	 * Binds the owning vortex event.
	 *
	 * @param vortex 漩涡事件 / vortex event
	 */
	public GeneratorDestroyListener(DimensionalVortex vortex) {
		this.vortex = vortex;
	}

	/**
	 * 死亡前钩子（空实现）。
	 * Pre-death hook (no-op).
	 *
	 * @param obj 濒死 AI / dying AI
	 */
	@Override
	public void onBeforeDie(AbstractAI obj) {
	}

	/**
	 * 死亡后标记生成器摧毁并停止入侵。
	 * After death, marks the generator destroyed and stops the invasion.
	 *
	 * @param obj 濒死 AI / dying AI
	 */
	@Override
	public void onAfterDie(AbstractAI obj) {
		vortex.setGeneratorDestroyed(true);
		GameLocationBootstrapServices.vortexService().stopInvasion(vortex.getVortexLocationId());
	}
}
