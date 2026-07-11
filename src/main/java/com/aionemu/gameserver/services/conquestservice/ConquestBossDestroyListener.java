package com.aionemu.gameserver.services.conquestservice;

import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.eventcallback.OnDieEventCallback;

/**
 * 征服/供奉 BOSS 死亡监听器（当前为空实现，预留钩子）。
 * Death listener for Conquest/Offering bosses (currently no-op, reserved hook).
 *
 * @author Rinzler (Encom)
 */
@SuppressWarnings("rawtypes")
public class ConquestBossDestroyListener extends OnDieEventCallback {

	private final ConquestOffering<?> conquestOffering;

	/**
	 * 绑定所属供奉事件。
	 * Binds the owning offering event.
	 *
	 * offering event
	 */
	public ConquestBossDestroyListener(ConquestOffering conquestOffering) {
		this.conquestOffering = conquestOffering;
	}

	/**
	 * 死亡前钩子（空实现）。
	 * Pre-death hook (no-op).
	 *
	 * dying AI
	 */
	@Override
	public void onBeforeDie(AbstractAI obj) {
	}

	/**
	 * 死亡后钩子（空实现）。
	 * Post-death hook (no-op).
	 *
	 * dying AI
	 */
	@Override
	public void onAfterDie(AbstractAI obj) {
	}
}
