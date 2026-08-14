package com.aionemu.gameserver.services.rvrservice;

import com.aionemu.gameserver.model.rvr.RvrLocation;
import com.aionemu.gameserver.model.rvr.RvrStateType;

/**
 * 种族对抗（RVR）直通门默认实现：切入 RVR / 回到 PEACE。
 * Default RVR direct portal implementation: switch to RVR / back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class DirectPortal extends Rvrlf3df3<RvrLocation> {

	/**
	 * 绑定 RVR 地点。
	 * Binds the RVR location.
	 *
	 * @param rvr 地点 / location
	 */
	public DirectPortal(RvrLocation rvr) {
		super(rvr);
	}

	/**
	 * 激活活动并刷新 RVR 刷怪。
	 * Activates the event and spawns RVR entities.
	 */
	@Override
	public void startRvr() {
		getRvrLocation().setActiveRvr(this);
		despawn();
		spawn(RvrStateType.RVR);
	}

	/**
	 * 结束活动并恢复 PEACE 刷怪。
	 * Ends the event and restores PEACE spawns.
	 */
	@Override
	public void stopRvr() {
		getRvrLocation().setActiveRvr(null);
		despawn();
		spawn(RvrStateType.PEACE);
	}
}
