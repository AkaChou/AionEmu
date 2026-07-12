package com.aionemu.gameserver.services.svsservice;

import com.aionemu.gameserver.model.svs.SvsLocation;
import com.aionemu.gameserver.model.svs.SvsStateType;

/**
 * 帕内斯特拉（SVS）大门默认实现：切入 SVS / 回到 PEACE。
 * back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class Gate extends Panesterra<SvsLocation> {

	/**
	 * 绑定 SVS 地点。
	 * Binds the SVS location.
	 *
	 * location
	 */
	public Gate(SvsLocation svs) {
		super(svs);
	}

	/**
	 * 激活活动并刷新 SVS 刷怪。
	 * Activates the event and spawns SVS entities.
	 */
	@Override
	public void startSvs() {
		getSvsLocation().setActiveSvs(this);
		despawn();
		spawn(SvsStateType.SVS);
	}

	/**
	 * 结束活动并恢复 PEACE 刷怪。
	 * Ends the event and restores PEACE spawns.
	 */
	@Override
	public void stopSvs() {
		getSvsLocation().setActiveSvs(null);
		despawn();
		spawn(SvsStateType.PEACE);
	}
}
