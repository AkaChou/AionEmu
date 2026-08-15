package com.aionemu.gameserver.services.conquestservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.conquest.ConquestLocation;

/**
 * 征服/供奉活动启动定时任务。
 * Start runnable for Conquest/Offering events.
 *
 * <p>启动对应地点。
 * Starts the matching location.</p>
 *
 * @author Rinzler (Encom)
 */
public class ConquestStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public ConquestStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 启用对应地点。
	 * Starts the matching location.
	 */
	@Override
	public void run() {
		Map<Integer, ConquestLocation> locations = GameLocationBootstrapServices.conquestService().getConquestLocations();
		for (final ConquestLocation loc : locations.values()) {
			if (loc.getId() == id) {
				GameLocationBootstrapServices.conquestService().startConquest(loc.getId());
			}
		}
	}
}
