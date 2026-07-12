package com.aionemu.gameserver.services.anohaservice;

import com.aionemu.gameserver.model.anoha.AnohaLocation;
import com.aionemu.gameserver.model.anoha.AnohaStateType;

/**
 * 狂暴阿诺哈默认实现：切入 FIGHT / 回到 PEACE。
 * back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class DanuarHero extends BerserkAnoha<AnohaLocation> {

	/**
	 * 绑定阿诺哈地点。
	 * Binds the Anoha location.
	 *
	 * location
	 */
	public DanuarHero(AnohaLocation anoha) {
		super(anoha);
	}

	/**
	 * 激活活动并刷新 FIGHT 刷怪。
	 * Activates the event and spawns FIGHT entities.
	 */
	@Override
	public void startAnoha() {
		getAnohaLocation().setActiveAnoha(this);
		despawn();
		spawn(AnohaStateType.FIGHT);
	}

	/**
	 * 结束活动并恢复 PEACE 刷怪。
	 * Ends the event and restores PEACE spawns.
	 */
	@Override
	public void stopAnoha() {
		getAnohaLocation().setActiveAnoha(null);
		despawn();
		spawn(AnohaStateType.PEACE);
	}
}
