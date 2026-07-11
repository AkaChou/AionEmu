package com.aionemu.gameserver.services.dynamicriftservice;

import com.aionemu.gameserver.model.dynamicrift.DynamicRiftLocation;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;

/**
 * 动态裂隙入口默认实现：切入 OPEN / 回到 CLOSED。
 * back to CLOSED. / back to CLOSED.
 *
 * @author Rinzler (Encom)
 */
public class Portal extends DynamicRift<DynamicRiftLocation> {

	/**
	 * 绑定动态裂隙地点。
	 * Binds the Dynamic Rift location.
	 *
	 * location
	 */
	public Portal(DynamicRiftLocation dynamicRift) {
		super(dynamicRift);
	}

	/**
	 * 激活活动并刷新 OPEN 刷怪。
	 * Activates the event and spawns OPEN entities.
	 */
	@Override
	public void startDynamicRift() {
		getDynamicRiftLocation().setActiveDynamicRift(this);
		despawn();
		spawn(DynamicRiftStateType.OPEN);
	}

	/**
	 * 结束活动并恢复 CLOSED 刷怪。
	 * Ends the event and restores CLOSED spawns.
	 */
	@Override
	public void stopDynamicRift() {
		getDynamicRiftLocation().setActiveDynamicRift(null);
		despawn();
		spawn(DynamicRiftStateType.CLOSED);
	}
}
