package com.aionemu.gameserver.services.siegeservice;

import java.util.concurrent.Future;

import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;

/**
 * 攻城突击基类，定义突击开始/结束流程。
 * Siege assault base class defining assault start/finish flow.
 */
public abstract class Assault<siege extends Siege<?>> {

	protected final SiegeLocation siegeLocation;
	protected final int locationId;
	protected final SiegeNpc boss;
	protected final int worldId;
	protected Future<?> dredgionTask;
	protected Future<?> spawnTask;

	public Assault(Siege<?> siege) {
		this.siegeLocation = siege.getSiegeLocation();
		this.boss = siege.getBoss();
		this.locationId = siege.getSiegeLocationId();
		this.worldId = siege.getSiegeLocation().getWorldId();
	}

	/**
	 * 返回本次突击所在世界 ID。
	 * Returns the world id of this assault.
	 *
	 * @return 世界 ID / world id
	 */
	public int getWorldId() {
		return worldId;
	}

	/**
	 * 开始突击。
	 * Starts the assault.
	 *
	 * @param delay 延迟毫秒 / delay
	 */
	public void startAssault(int delay) {
		scheduleAssault(delay);
	}

	/**
	 * 结束突击。
	 * Finishes the assault.
	 *
	 * @param captured 是否占领成功 / whether captured
	 */
	public void finishAssault(boolean captured) {
		if (dredgionTask != null && !dredgionTask.isDone()) {
			dredgionTask.cancel(true);
		}
		if (spawnTask != null && !spawnTask.isDone()) {
			spawnTask.cancel(true);
		}
		onAssaultFinish(captured && siegeLocation.getRace().equals(SiegeRace.BALAUR));
	}

	protected abstract void onAssaultFinish(boolean captured);

	protected abstract void scheduleAssault(int delay);
}