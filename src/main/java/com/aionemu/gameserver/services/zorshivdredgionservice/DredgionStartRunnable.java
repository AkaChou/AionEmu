package com.aionemu.gameserver.services.zorshivdredgionservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionLocation;

/**
 * 佐尔希夫挖掘舰活动启动定时任务。
 * Start runnable for the Zorshiv dredgion world event.
 *
 * <p>按时间轴刷出入口、激光、黑天并最终启动着陆。
 * Stages portal, laser, black-sky and finally starts the landing.</p>
 *
 * @author Rinzler (Encom)
 */
public class DredgionStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public DredgionStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 执行分阶段启动流程。
	 * Runs the staged start sequence.
	 */
	@Override
	public void run() {
		// 入侵传送门。 / Invasion Portal.
		GameLocationBootstrapServices.zorshivDredgionService().adventPortalSP(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 入侵激光。 / Invasion Lazer.
				GameLocationBootstrapServices.zorshivDredgionService().adventDirectingSP(id);
			}
		}, 180000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 入侵黑空。 / Invasion Black Sky.
				GameLocationBootstrapServices.zorshivDredgionService().adventControlSP(id);
			}
		}, 300000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, ZorshivDredgionLocation> locations = GameLocationBootstrapServices.zorshivDredgionService()
						.getZorshivDredgionLocations();
				for (ZorshivDredgionLocation loc : locations.values()) {
					if (loc.getId() == id) {
						// 入侵浅蓝。 / Invasion Light Blue.
						GameLocationBootstrapServices.zorshivDredgionService().adventEffectSP(id);
						// 龙族战舰已出现。 / The Balaur Dredgion has appeared at levinshor.
						GameLocationBootstrapServices.zorshivDredgionService().levinshorMsg(id);
						// 龙族战舰已出现。 / The Balaur Dredgion has appeared at inggison.
						GameLocationBootstrapServices.zorshivDredgionService().inggisonMsg(id);
						GameLocationBootstrapServices.zorshivDredgionService().startZorshivDredgion(loc.getId());
					}
				}
			}
		}, 600000);
	}
}
