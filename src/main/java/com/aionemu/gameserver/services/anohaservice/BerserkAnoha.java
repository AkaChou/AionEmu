package com.aionemu.gameserver.services.anohaservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.anoha.AnohaLocation;
import com.aionemu.gameserver.model.anoha.AnohaStateType;

/**
 * 狂暴阿诺哈活动抽象基类。
 * Abstract base for Berserk Anoha world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <AL> 阿诺哈地点类型 / Anoha location type
 */
public abstract class BerserkAnoha<AL extends AnohaLocation> {

	private boolean started;
	private final AL anohaLocation;
	private final AtomicBoolean finished = new AtomicBoolean();

	/**
	 * 停止活动的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopAnoha();

	/**
	 * 启动活动的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startAnoha();

	/**
	 * 绑定阿诺哈地点。
	 * Binds the Anoha location.
	 *
	 * location
	 */
	public BerserkAnoha(AL anohaLocation) {
		this.anohaLocation = anohaLocation;
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
		startAnoha();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopAnoha();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(AnohaStateType type) {
		GameLocationBootstrapServices.anohaService().spawn(getAnohaLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.anohaService().despawn(getAnohaLocation());
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
	public AL getAnohaLocation() {
		return anohaLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * location id
	 */
	public int getAnohaLocationId() {
		return anohaLocation.getId();
	}
}
