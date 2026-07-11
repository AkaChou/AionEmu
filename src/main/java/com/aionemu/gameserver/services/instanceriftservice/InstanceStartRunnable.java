package com.aionemu.gameserver.services.instanceriftservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.instancerift.InstanceRiftLocation;

/**
 * 副本裂隙启动定时任务。
 * Start runnable for instance-rift events.
 *
 * @author Rinzler (Encom)
 */
public class InstanceStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public InstanceStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 查找匹配地点并启动裂隙。
	 * Finds the matching location and starts the rift.
	 */
	@Override
	public void run() {
		Map<Integer, InstanceRiftLocation> locations = GameLocationBootstrapServices.instanceRiftService().getInstanceRiftLocations();
		for (InstanceRiftLocation loc : locations.values()) {
			if (loc.getId() == id) {
				GameLocationBootstrapServices.instanceRiftService().startInstanceRift(loc.getId());
			}
		}
	}
}
