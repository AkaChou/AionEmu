package com.aionemu.gameserver.services.nightmarecircusservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusLocation;

/**
 * 梦魇马戏团活动启动定时任务。
 * Start runnable for the Nightmare Circus world event.
 *
 * <p>匹配地点 ID 后启动对应马戏团实例。
 * Starts the matching circus instance for the bound location id.</p>
 *
 * @author Rinzler (Encom)
 */
public class CircusStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public CircusStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 执行启动流程。
	 * Runs the start sequence.
	 */
	@Override
	public void run() {
		Map<Integer, NightmareCircusLocation> locations = GameLocationBootstrapServices.nightmareCircusService()
				.getNightmareCircusLocations();
		for (final NightmareCircusLocation loc : locations.values()) {
			if (loc.getId() == id) {
				GameLocationBootstrapServices.nightmareCircusService().startNightmareCircus(loc.getId());
			}
		}
	}
}
