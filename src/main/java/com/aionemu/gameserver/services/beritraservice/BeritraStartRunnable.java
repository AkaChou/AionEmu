package com.aionemu.gameserver.services.beritraservice;

import java.util.Map;

import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.beritra.BeritraLocation;
import lombok.AllArgsConstructor;

/**
 * 贝尔特拉/埃雷什基伽尔入侵启动定时任务。
 * Beritra/Ereshkigal invasion start timer task.
 * <p>按时间轴依次刷出入口、激光、黑天与正式入侵。
 * Stages portal, laser, black-sky and the actual invasion along a timed timeline.</p>
 * @author Rinzler (Encom)
 */
@AllArgsConstructor
public class BeritraStartRunnable implements Runnable {

	private final int id;

	/**
	 * 执行分阶段启动流程。
	 * Runs the staged start sequence.
	 */
	@Override
	public void run() {
		// 清理上一轮遗留的临时特效对象。 / Despawn temporary effect objects left by the previous cycle.
		BeritraService.getInstance().clearAdventObjects(id);
		// 贝里特拉入侵传送门。 / Beritra Invasion Portal.
		BeritraService.getInstance().adventPortalSP(id);
		// 埃雷什基伽尔入侵传送门。 / Ereshkigal Invasion Portal.
		BeritraService.getInstance().adventPortalEreshSP(id);
		// 贝里特拉军团入侵走廊已出现。 / The Beritra Legion's Invasion Corridor has appeared.
		BeritraService.getInstance().invasionCorridorMsg(id);
		// 埃雷什基伽尔军团入侵走廊已创建。 / The Ereshkigal Legion's Invasion Corridor has been created.
		BeritraService.getInstance().ereshkigalCorridorMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 贝里特拉入侵激光。 / Beritra Invasion Lazer.
			BeritraService.getInstance().adventDirectingSP(id);
			// 埃雷什基伽尔入侵激光。 / Ereshkigal Invasion Lazer.
			BeritraService.getInstance().adventDirectingEreshSP(id);
			// 恶魔部队已通过入侵走廊渗透。 / The Devil Unit has infiltrated through the Invasion Corridor.
			BeritraService.getInstance().devilUnitThroughMsg(id);
			// 埃雷什基伽尔军团的魔法武器已通过入侵走廊。 / The Ereshkigal Legion's magic weapon has infiltrated through the invasion corridor.
			BeritraService.getInstance().ereshkigalLegionThroughMsg(id);
		}, 180000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 贝里特拉入侵黑空。 / Beritra Invasion Black Sky.
			BeritraService.getInstance().adventControlSP(id);
			// 埃雷什基伽尔入侵黑空。 / Ereshkigal Invasion Black Sky.
			BeritraService.getInstance().adventControlEreshSP(id);
		}, 300000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			Map<Integer, BeritraLocation> locations = BeritraService.getInstance().getBeritraLocations();
			for (final BeritraLocation loc : locations.values()) {
				if (loc.getId() == id) {
					// 贝里特拉入侵浅蓝。 / Beritra Invasion Light Blue.
					BeritraService.getInstance().adventEffectSP(id);
					// 埃雷什基伽尔入侵浅蓝。 / Ereshkigal Invasion Light Blue.
					BeritraService.getInstance().adventEffectEreshSP(id);
					// 贝里特拉入侵开始 4.7 / Beritra Invasion Start 4.7
					BeritraService.getInstance().beritraInvasionMsg(id);
					// 埃雷什基伽尔入侵开始 4.9.1 / Ereshkigal Invasion Start 4.9.1
					BeritraService.getInstance().ereshkigalInvasionMsg(id);
					// 战舰防御。 / Dredgion Defense.
					BeritraService.getInstance().dredgionDefenseMsg(id);
					BeritraService.getInstance().startBeritraInvasion(loc.getId());
				}
			}
		}, 600000);
	}
}
