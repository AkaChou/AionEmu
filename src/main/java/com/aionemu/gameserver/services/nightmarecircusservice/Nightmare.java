package com.aionemu.gameserver.services.nightmarecircusservice;

import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusLocation;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusStateType;

/**
 * 梦魇马戏团默认实现：切入 OPEN / 回到 CLOSED。
 * back to CLOSED.
 *
 * @author Rinzler (Encom)
 */
public class Nightmare extends CircusInstance<NightmareCircusLocation> {

	/**
	 * 绑定梦魇马戏团地点。
	 * Binds the Nightmare Circus location.
	 *
	 * location
	 */
	public Nightmare(NightmareCircusLocation nightmareCircus) {
		super(nightmareCircus);
	}

	/**
	 * 激活活动并刷新 OPEN 刷怪。
	 * Activates the event and spawns OPEN entities.
	 */
	@Override
	public void startNightmareCircus() {
		getNightmareCircusLocation().setActiveNightmareCircus(this);
		despawn();
		spawn(NightmareCircusStateType.OPEN);
	}

	/**
	 * 结束活动并恢复 CLOSED 刷怪。
	 * Ends the event and restores CLOSED spawns.
	 */
	@Override
	public void stopNightmareCircus() {
		getNightmareCircusLocation().setActiveNightmareCircus(null);
		despawn();
		spawn(NightmareCircusStateType.CLOSED);
	}
}
