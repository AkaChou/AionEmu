package com.aionemu.gameserver.services.siegeservice;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.services.SiegeService;

/**
 * 攻城启动任务，到点触发指定要塞攻城。
 * Siege start runnable triggering a fortress siege when due.
 */
public class SiegeStartRunnable implements Runnable {

	private final int locationId;

	public SiegeStartRunnable(int locationId) {
		this.locationId = locationId;
	}

	@Override
	/**
	 * 执行任务。
	 * Runs the task.
	 */
	public void run() {
		GameFeatureServices.siegeService().checkSiegeStart(getLocationId());
	}

	/**
	 * 返回目标据点 ID。
	 * Returns the target location id.
	 *
	 * @return 据点 ID / location id
	 */
	public int getLocationId() {
		return locationId;
	}
}