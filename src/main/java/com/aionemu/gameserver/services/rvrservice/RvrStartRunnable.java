package com.aionemu.gameserver.services.rvrservice;

import java.util.Map;

import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.rvr.RvrLocation;
import lombok.AllArgsConstructor;

/**
 * 种族对抗（RVR）活动启动定时任务。
 * Start runnable for the RVR (Race vs Race) world event.
 * <p>按时间轴刷出入口/激光/黑天、播报入侵倒计时，并最终启动 RVR。
 * Stages portal/laser/black-sky, invasion countdown messages, then starts RVR.</p>
 * @author Rinzler (Encom)
 */
@AllArgsConstructor
public class RvrStartRunnable implements Runnable {

	private final int id;

	/**
	 * 执行分阶段启动流程。
	 * Runs the staged start sequence.
	 */
	@Override
	public void run() {
		// 清理上一轮遗留的临时对象。 / Despawn temporary objects left by the previous cycle.
		RvrService.getInstance().clearAdventObjects(id);
		// 入侵传送门。 / Invasion Portal.
		RvrService.getInstance().adventPortalSP(id);
		// 天族战舰将在 10 分钟后入侵。 / An Elyos warship will invade in 10 minutes.
		RvrService.getInstance().DF6G1Spawn01Msg(id);
		// 魔族战舰将在 10 分钟后入侵。 / An Asmodian warship will invade in 10 minutes.
		RvrService.getInstance().LF6G1Spawn01Msg(id);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 入侵激光。 / Invasion Lazer.
			RvrService.getInstance().adventDirectingSP(id);
		}, 180000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 入侵黑空。 / Invasion Black Sky.
			RvrService.getInstance().adventControlSP(id);
			// 天族战舰将在 5 分钟后入侵。 / An Elyos Warship will invade in 5 minutes.
			RvrService.getInstance().DF6G1Spawn02Msg(id);
			// 魔族战舰将在 5 分钟后入侵。 / An Asmodian Warship will invade in 5 minutes.
			RvrService.getInstance().LF6G1Spawn02Msg(id);
			// 检测到入侵。 / Intrusion was detected.
			RvrService.getInstance().F6RaidStart5Minute(id);
		}, 300000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 天族战舰将在 3 分钟后入侵。 / An Elyos warship will invade in 3 minutes.
			RvrService.getInstance().DF6G1Spawn03Msg(id);
			// 魔族战舰将在 3 分钟后入侵。 / An Asmodian warship will invade in 3 minutes.
			RvrService.getInstance().LF6G1Spawn03Msg(id);
		}, 480000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			// 天族战舰将在 1 分钟后入侵。 / An Elyos warship will invade in 1 minute.
			RvrService.getInstance().DF6G1Spawn04Msg(id);
			// 魔族战舰将在 1 分钟后入侵。 / An Asmodian warship will invade in 1 minute.
			RvrService.getInstance().LF6G1Spawn04Msg(id);
		}, 540000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			Map<Integer, RvrLocation> locations = RvrService.getInstance().getRvrLocations();
			for (final RvrLocation loc : locations.values()) {
				if (loc.getId() == id) {
					// 入侵浅蓝。 / Invasion Light Blue.
					RvrService.getInstance().adventEffectSP(id);
					// 天族战舰入侵。 / Elyos Warship Invasion.
					RvrService.getInstance().DF6G1Spawn05Msg(id);
					// 魔族战舰入侵。 / Asmodian Warship Invasion.
					RvrService.getInstance().LF6G1Spawn05Msg(id);
					// 古代武器入侵。 / Ancient's Weapon Invasion.
					RvrService.getInstance().F6RaidStart(id);
					// 旅团将军的紧急命令。 / Brigade General's Urgent Order.
					RvrService.getInstance().startRvr(loc.getId());
					// 军官落败后，魔族士兵正在撤退。 / The Asmodian Troopers are retreating after the defeat of their officers.
					RvrService.getInstance().LF6EventG2Start02Msg(id);
					// 军官落败后，埃托斯正在撤退。 / The Aetos are retreating after the defeat of their officers.
					RvrService.getInstance().DF6EventG2Start02Msg(id);
				}
			}
		}, 600000);
	}
}
