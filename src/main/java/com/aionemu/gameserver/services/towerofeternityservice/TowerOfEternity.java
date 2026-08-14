package com.aionemu.gameserver.services.towerofeternityservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;

/**
 * 永恒之塔活动抽象基类。
 * Abstract base for Tower of Eternity world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Wnkrz
 * @param <TE> 永恒之塔地点类型 / tower location type
 */
public abstract class TowerOfEternity<TE extends TowerOfEternityLocation> {

	private boolean started;
	private final TE towerOfEternityLocation;
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * 关闭活动的具体实现。
	 * Concrete close logic.
	 */
	protected abstract void stopTowerOfEternity();

	/**
	 * 开启活动的具体实现。
	 * Concrete open logic.
	 */
	protected abstract void startTowerOfEternity();

	/**
	 * 绑定永恒之塔地点。
	 * Binds the tower location.
	 *
	 * @param towerOfEternityLocation 永恒之塔地点 / tower location
	 */
	public TowerOfEternity(TE towerOfEternityLocation) {
		this.towerOfEternityLocation = towerOfEternityLocation;
	}

	/**
	 * 开启活动（幂等）。
	 * Opens the event (idempotent).
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
		startTowerOfEternity();
	}

	/**
	 * 关闭活动（仅首次生效）。
	 * Closes the event (first call only).
	 */
	public final void stop() {
		if (closed.compareAndSet(false, true)) {
			stopTowerOfEternity();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(TowerOfEternityStateType type) {
		GameLocationBootstrapServices.towerOfEternityService().spawn(getTowerOfEternityLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.towerOfEternityService().despawn(getTowerOfEternityLocation());
	}

	/**
	 * 是否已关闭。
	 * Whether the event is closed.
	 *
	 * @return 已关闭则为 true / true if closed
	 */
	public boolean isClosed() {
		return closed.get();
	}

	/**
	 * 获取绑定地点。
	 * Returns the bound location.
	 *
	 * @return 绑定地点 / location
	 */
	public TE getTowerOfEternityLocation() {
		return towerOfEternityLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getTowerOfEternityLocationId() {
		return towerOfEternityLocation.getId();
	}
}
