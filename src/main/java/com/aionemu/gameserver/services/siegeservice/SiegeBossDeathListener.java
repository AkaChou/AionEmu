package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.eventcallback.OnDieEventCallback;
import com.aionemu.gameserver.services.SiegeService;

/**
 * 攻城 BOSS 死亡监听器，在击杀后结算攻城。
 * Siege boss death listener settling the siege after the boss dies.
 */
@SuppressWarnings("rawtypes")
public class SiegeBossDeathListener extends OnDieEventCallback {

	private final Siege<?> siege;

	public SiegeBossDeathListener(Siege siege) {
		this.siege = siege;
	}

	@Override
	/**
	 * 死亡前回调。
	 * Before-death callback.
	 *
	 * @param obj 首领 AI / boss AI
	 */
	public void onBeforeDie(AbstractAI obj) {
	}

	@Override
	/**
	 * 死亡后回调。
	 * After-death callback.
	 *
	 * @param obj 首领 AI / boss AI
	 */
	public void onAfterDie(AbstractAI obj) {
		siege.setBossKilled(true);
		GameFeatureServices.siegeService().stopSiege(siege.getSiegeLocationId());
	}
}