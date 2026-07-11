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
	 * getLocationId 方法。
	 * getLocationId method.
	 * result
	 */
	public int getLocationId() {
		return locationId;
	}
}