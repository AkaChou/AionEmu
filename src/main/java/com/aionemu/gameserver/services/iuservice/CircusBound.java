package com.aionemu.gameserver.services.iuservice;

import com.aionemu.gameserver.model.iu.IuLocation;
import com.aionemu.gameserver.model.iu.IuStateType;

/**
 * IU 演唱会默认实现：启动时切入 OPEN 状态，停止时回到 CLOSED 状态。
 * Default IU concert implementation: switches to OPEN on start, back to CLOSED on stop.
 *
 * @author Rinzler (Encom)
 */
public class CircusBound extends Iu<IuLocation> {

	/**
	 * 绑定 IU 地点。
	 * Binds the IU location.
	 *
	 * @param iu 地点 / location
	 */
	public CircusBound(IuLocation iu) {
		super(iu);
	}

	/**
	 * 激活演唱会并刷新 OPEN 刷怪。
	 * Activates the concert and spawns OPEN entities.
	 */
	@Override
	public void startConcert() {
		getIuLocation().setActiveIu(this);
		despawn();
		spawn(IuStateType.OPEN);
	}

	/**
	 * 结束演唱会并恢复 CLOSED 刷怪。
	 * Ends the concert and restores CLOSED spawns.
	 */
	@Override
	public void stopConcert() {
		getIuLocation().setActiveIu(null);
		despawn();
		spawn(IuStateType.CLOSED);
	}
}
