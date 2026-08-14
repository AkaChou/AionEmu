package com.aionemu.gameserver.services.beritraservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.beritra.BeritraLocation;
import com.aionemu.gameserver.model.beritra.BeritraStateType;

/**
 * 贝尔特拉入侵活动抽象基类。
 * Abstract base for Beritra invasion events.
 *
 * <p>封装启动/停止的幂等守卫，以及按状态类型刷怪/清怪的通用逻辑。
 * Encapsulates idempotent start/stop guards and shared spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <BL> 入侵位置类型 / invasion location type
 */
public abstract class BeritraInvasion<BL extends BeritraLocation> {

	private boolean started;
	private final BL beritraLocation;
	private final AtomicBoolean finished = new AtomicBoolean();

	/**
	 * 停止入侵的具体实现（由子类提供）。
	 * Concrete stop logic (implemented by subclasses).
	 */
	protected abstract void stopBeritraInvasion();

	/**
	 * 启动入侵的具体实现（由子类提供）。
	 * Concrete start logic (implemented by subclasses).
	 */
	protected abstract void startBeritraInvasion();

	/**
	 * 绑定入侵地点。
	 * Binds the invasion location.
	 *
	 * @param beritraLocation 入侵地点 / invasion location
	 */
	public BeritraInvasion(BL beritraLocation) {
		this.beritraLocation = beritraLocation;
	}

	/**
	 * 启动入侵（幂等，重复调用会被忽略）。
	 * Starts the invasion (idempotent; subsequent calls are ignored).
	 */
	public final void start() {
		boolean doubleStart = false;
		synchronized (this) {
			if (started) {
				doubleStart = true;
			} else {
				started = true;
			}
		}
		if (doubleStart) {
			return;
		}
		startBeritraInvasion();
	}

	/**
	 * 停止入侵（仅首次生效）。
	 * Stops the invasion (only the first call takes effect).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopBeritraInvasion();
		}
	}

	/**
	 * 按状态类型刷新该地点刷怪。
	 * Spawns entities for this location by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(BeritraStateType type) {
		GameLocationBootstrapServices.beritraService().spawn(getBeritraLocation(), type);
	}

	/**
	 * 清除该地点已刷出的对象。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.beritraService().despawn(getBeritraLocation());
	}

	/**
	 * 是否已结束。
	 * Whether the event has finished.
	 *
	 * @return 已结束则为 true / true if finished
	 */
	public boolean isFinished() {
		return finished.get();
	}

	/**
	 * 获取绑定的入侵地点。
	 * Returns the bound invasion location.
	 *
	 * @return 绑定的入侵地点 / invasion location
	 */
	public BL getBeritraLocation() {
		return beritraLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getBeritraLocationId() {
		return beritraLocation.getId();
	}
}
