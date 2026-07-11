package com.aionemu.gameserver.services.idiandepthsservice;

import com.aionemu.gameserver.model.idiandepths.IdianDepthsLocation;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;

/**
 * 伊迪安深渊默认实现：切入 OPEN / 回到 CLOSED。
 * back to CLOSED. / back to CLOSED.
 *
 * @author Rinzler (Encom)
 */
public class Idian extends IdianDepths<IdianDepthsLocation> {

	/**
	 * 绑定伊迪安深渊地点。
	 * Binds the Idian Depths location.
	 *
	 * location
	 */
	public Idian(IdianDepthsLocation idianDepths) {
		super(idianDepths);
	}

	/**
	 * 激活活动并刷新 OPEN 刷怪。
	 * Activates the event and spawns OPEN entities.
	 */
	@Override
	public void startIdianDepths() {
		getIdianDepthsLocation().setActiveIdianDepths(this);
		despawn();
		spawn(IdianDepthsStateType.OPEN);
	}

	/**
	 * 结束活动并恢复 CLOSED 刷怪。
	 * Ends the event and restores CLOSED spawns.
	 */
	@Override
	public void stopIdianDepths() {
		getIdianDepthsLocation().setActiveIdianDepths(null);
		despawn();
		spawn(IdianDepthsStateType.CLOSED);
	}
}
