package com.aionemu.gameserver.services.instanceriftservice;

import com.aionemu.gameserver.model.instancerift.InstanceRiftLocation;
import com.aionemu.gameserver.model.instancerift.InstanceRiftStateType;

/**
 * 副本裂隙默认实现：切入 OPEN / 回到 CLOSED。
 * Default rift implementation: transitions to OPEN / back to CLOSED.
 *
 * @author Rinzler (Encom)
 */
public class Rift extends RiftInstance<InstanceRiftLocation> {

	/**
	 * 绑定裂隙地点。
	 * Binds the rift location.
	 *
	 * @param instanceRift 裂隙地点 / rift location
	 */
	public Rift(InstanceRiftLocation instanceRift) {
		super(instanceRift);
	}

	/**
	 * 激活裂隙并刷新 OPEN 刷怪。
	 * Activates the rift and spawns OPEN entities.
	 */
	@Override
	public void startInstanceRift() {
		getInstanceRiftLocation().setActiveInstanceRift(this);
		despawn();
		spawn(InstanceRiftStateType.OPEN);
	}

	/**
	 * 关闭裂隙并恢复 CLOSED 刷怪。
	 * Closes the rift and restores CLOSED spawns.
	 */
	@Override
	public void stopInstanceRift() {
		getInstanceRiftLocation().setActiveInstanceRift(null);
		despawn();
		spawn(InstanceRiftStateType.CLOSED);
	}
}
