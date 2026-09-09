package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

/**
 * 攻城启动任务，到点触发指定要塞攻城。
 * Siege start runnable triggering a fortress siege when due.
 */
public record SiegeStartRunnable(int locationId) implements Runnable {

	@Override
	/**
	 * 执行任务。
	 * Runs the task.
	 */
	public void run() {
		GameFeatureServices.siegeService().checkSiegeStart(locationId());
	}

	/**
	 * 返回目标据点 ID。
	 * Returns the target location id.
	 *
	 * @return 据点 ID / location id
	 */
	@Override
	public int locationId() {
		return locationId;
	}
}
