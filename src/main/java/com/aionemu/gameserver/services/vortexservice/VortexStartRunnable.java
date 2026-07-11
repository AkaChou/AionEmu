package com.aionemu.gameserver.services.vortexservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.vortex.VortexLocation;

/**
 * 次元漩涡入侵启动定时任务。
 * Start runnable for dimensional-vortex invasions.
 *
 * @author Rinzler (Encom)
 */
public class VortexStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public VortexStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 查找匹配地点并启动入侵。
	 * Finds the matching location and starts the invasion.
	 */
	@Override
	public void run() {
		Map<Integer, VortexLocation> locations = GameLocationBootstrapServices.vortexService().getVortexLocations();
		for (final VortexLocation loc : locations.values()) {
			if (loc.getId() == id) {
				GameLocationBootstrapServices.vortexService().startInvasion(loc.getId());
			}
		}
	}
}
