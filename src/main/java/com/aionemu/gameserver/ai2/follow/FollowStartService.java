package com.aionemu.gameserver.ai2.follow;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;

/**
 * 召唤物跟随启动服务：创建周期性跟随/射程检查任务。
 * Summon follow start service: creates a periodic follow/range-check task.
 */
public class FollowStartService {

	/**
	 * 创建召唤物对目标的固定频率跟随检查任务（1 秒周期）。
	 * Creates a fixed-rate follow check task for a summon toward its leading creature (1s period).
	 *
	 * @param follower 跟随者召唤物 / following summon
	 * @param leading 被跟随的生物 / leading creature
	 * @return 已调度的任务句柄 / scheduled task handle
	 */
	public static final Future<?> newFollowingToTargetCheckTask(Summon follower, Creature leading) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new FollowSummonTaskAI(leading, follower), 1000,
				1000);
	}
}
