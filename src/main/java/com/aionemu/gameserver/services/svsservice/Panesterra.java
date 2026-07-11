package com.aionemu.gameserver.services.svsservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.svs.SvsLocation;
import com.aionemu.gameserver.model.svs.SvsStateType;

/**
 * 帕内斯特拉（SVS）活动抽象基类。
 * Abstract base for Panesterra (SVS) world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <PL> SVS 地点类型 / SVS location type
 */
public abstract class Panesterra<PL extends SvsLocation> {

	private boolean started;
	private final PL svsLocation;
	private final AtomicBoolean finished = new AtomicBoolean();

	/**
	 * 停止活动的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopSvs();

	/**
	 * 启动活动的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startSvs();

	/**
	 * 绑定 SVS 地点。
	 * Binds the SVS location.
	 *
	 * location
	 */
	public Panesterra(PL svsLocation) {
		this.svsLocation = svsLocation;
	}

	/**
	 * 启动活动（幂等）。
	 * Starts the event (idempotent).
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
		startSvs();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopSvs();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(SvsStateType type) {
		GameLocationBootstrapServices.svsService().spawn(getSvsLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.svsService().despawn(getSvsLocation());
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
	 * 获取绑定地点。
	 * Returns the bound location.
	 *
	 * location
	 */
	public PL getSvsLocation() {
		return svsLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * location id
	 */
	public int getSvsLocationId() {
		return svsLocation.getId();
	}
}
