package com.aionemu.gameserver.services.svsservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.svs.SvsLocation;

/**
 * 帕内斯特拉（SVS）活动启动定时任务。
 * Start runnable for Panesterra (SVS) events.
 *
 * <p>广播进阶走廊消息、延迟刷出走廊刷怪并启动对应地点。
 * Broadcasts advance-corridor messages, delayed corridor spawns, then starts the matching location.</p>
 *
 * @author Rinzler (Encom)
 */
public class SvsStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public SvsStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 执行启动流程。
	 * Runs the start sequence.
	 */
	@Override
	public void run() {
		// 进阶走廊【特兰西迪姆附楼】。 / Advance Corridor [Transidium Annex].
		GameLocationBootstrapServices.svsService().transidiumAnnexMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 进阶走廊【特兰西迪姆附楼】。 / Advance Corridor [Transidium Annex].
				GameLocationBootstrapServices.svsService().advanceCorridorSP(id);
			}
		}, 480000);
		Map<Integer, SvsLocation> locations = GameLocationBootstrapServices.svsService().getSvsLocations();
		for (final SvsLocation loc : locations.values()) {
			if (loc.getId() == id) {
				GameLocationBootstrapServices.svsService().startSvs(loc.getId());
			}
		}
	}
}
