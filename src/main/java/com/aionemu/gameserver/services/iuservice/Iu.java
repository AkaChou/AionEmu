package com.aionemu.gameserver.services.iuservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.iu.IuLocation;
import com.aionemu.gameserver.model.iu.IuStateType;

/**
 * IU 演唱会活动抽象基类。
 * Abstract base for IU concert world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <IUL> IU 地点类型 / IU location type
 */
public abstract class Iu<IUL extends IuLocation> {

	private boolean started;
	private final IUL iuLocation;
	private final AtomicBoolean finished = new AtomicBoolean();

	/**
	 * 停止演唱会的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopConcert();

	/**
	 * 启动演唱会的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startConcert();

	/**
	 * 绑定 IU 地点。
	 * Binds the IU location.
	 *
	 * location
	 */
	public Iu(IUL iuLocation) {
		this.iuLocation = iuLocation;
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
		startConcert();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopConcert();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(IuStateType type) {
		GameLocationBootstrapServices.iuService().spawn(getIuLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.iuService().despawn(getIuLocation());
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
	public IUL getIuLocation() {
		return iuLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * location id
	 */
	public int getIuLocationId() {
		return iuLocation.getId();
	}
}
