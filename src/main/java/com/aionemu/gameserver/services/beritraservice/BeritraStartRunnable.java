package com.aionemu.gameserver.services.beritraservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.beritra.BeritraLocation;

/**
 * 贝尔特拉/埃雷什基伽尔入侵启动定时任务。
 * Ereshkigal invasion events. / Ereshkigal invasion events.
 *
 * <p>按时间轴依次刷出入口、激光、黑天与正式入侵。
 * Stages portal, laser, black-sky and the actual invasion along a timed timeline.</p>
 *
 * @author Rinzler (Encom)
 */
public class BeritraStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public BeritraStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 执行分阶段启动流程。
	 * Runs the staged start sequence.
	 */
	@Override
	public void run() {
		// 贝里特拉入侵传送门。 / Beritra Invasion Portal.
		GameLocationBootstrapServices.beritraService().adventPortalSP(id);
		// 埃雷什基伽尔入侵传送门。 / Ereshkigal Invasion Portal.
		GameLocationBootstrapServices.beritraService().adventPortalEreshSP(id);
		// 贝里特拉军团入侵走廊已出现。 / The Beritra Legion's Invasion Corridor has appeared.
		GameLocationBootstrapServices.beritraService().invasionCorridorMsg(id);
		// 埃雷什基伽尔军团入侵走廊已创建。 / The Ereshkigal Legion's Invasion Corridor has been created.
		GameLocationBootstrapServices.beritraService().ereshkigalCorridorMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 贝里特拉入侵激光。 / Beritra Invasion Lazer.
				GameLocationBootstrapServices.beritraService().adventDirectingSP(id);
				// 埃雷什基伽尔入侵激光。 / Ereshkigal Invasion Lazer.
				GameLocationBootstrapServices.beritraService().adventDirectingEreshSP(id);
				// 恶魔部队已通过入侵走廊渗透。 / The Devil Unit has infiltrated through the Invasion Corridor.
				GameLocationBootstrapServices.beritraService().devilUnitThroughMsg(id);
				// 埃雷什基伽尔军团的魔法武器已通过入侵 / The Ereshkigal Legion's Magic weapon has infiltrated through the Invasion
				// 走廊。 / Corridor.
				GameLocationBootstrapServices.beritraService().ereshkigalLegionThroughMsg(id);
			}
		}, 180000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// 贝里特拉入侵黑空。 / Beritra Invasion Black Sky.
				GameLocationBootstrapServices.beritraService().adventControlSP(id);
				// 埃雷什基伽尔入侵黑空。 / Ereshkigal Invasion Black Sky.
				GameLocationBootstrapServices.beritraService().adventControlEreshSP(id);
			}
		}, 300000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, BeritraLocation> locations = GameLocationBootstrapServices.beritraService().getBeritraLocations();
				for (final BeritraLocation loc : locations.values()) {
					if (loc.getId() == id) {
						// 贝里特拉入侵浅蓝。 / Beritra Invasion Light Blue.
						GameLocationBootstrapServices.beritraService().adventEffectSP(id);
						// 埃雷什基伽尔入侵浅蓝。 / Ereshkigal Invasion Light Blue.
						GameLocationBootstrapServices.beritraService().adventEffectEreshSP(id);
						// 贝里特拉入侵开始 4.7 / Beritra Invasion Start 4.7
						GameLocationBootstrapServices.beritraService().beritraInvasionMsg(id);
						// 埃雷什基伽尔入侵开始 4.9.1 / Ereshkigal Invasion Start 4.9.1
						GameLocationBootstrapServices.beritraService().ereshkigalInvasionMsg(id);
						// 战舰防御。 / Dredgion Defense.
						GameLocationBootstrapServices.beritraService().dredgionDefenseMsg(id);
						GameLocationBootstrapServices.beritraService().startBeritraInvasion(loc.getId());
					}
				}
			}
		}, 600000);
	}
}
