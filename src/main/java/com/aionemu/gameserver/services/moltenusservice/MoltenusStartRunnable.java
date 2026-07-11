package com.aionemu.gameserver.services.moltenusservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.moltenus.MoltenusLocation;

/**
 * 熔岩领主（Moltenus）活动启动定时任务。
 * Start runnable for the Moltenus world event.
 *
 * <p>先广播三堡守护者预告，再延迟 10 分钟启动对应地点。
 * Broadcasts the three fortress guardian warnings, then starts the matching location after 10 minutes.</p>
 *
 * @author Rinzler (Encom)
 */
public class MoltenusStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public MoltenusStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 执行启动流程。
	 * Runs the start sequence.
	 */
	@Override
	public void run() {
		// 暴怒的硫磺守护者将在 10 分钟后出现。 / Enraged Sulfur Guardian will appear in 10 minutes.
		GameLocationBootstrapServices.moltenusService().sulfurFortressMsg(id);
		// 暴怒的西部守护者将在 10 分钟后出现。 / Enraged Western Guardian will appear in 10 minutes.
		GameLocationBootstrapServices.moltenusService().westernFortressMsg(id);
		// 暴怒的东部守护者将在 10 分钟后出现。 / Enraged Eastern Guardian will appear in 10 minutes.
		GameLocationBootstrapServices.moltenusService().easternFortressMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, MoltenusLocation> locations = GameLocationBootstrapServices.moltenusService().getMoltenusLocations();
				for (final MoltenusLocation loc : locations.values()) {
					if (loc.getId() == id) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(loc.getId());
					}
				}
			}
		}, 600000);
	}
}
