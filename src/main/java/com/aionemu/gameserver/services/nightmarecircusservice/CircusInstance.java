package com.aionemu.gameserver.services.nightmarecircusservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusLocation;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusStateType;

/**
 * 梦魇马戏团活动抽象基类。
 * Abstract base for Nightmare Circus world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <CL> 马戏团地点类型 / circus location type
 */
public abstract class CircusInstance<CL extends NightmareCircusLocation> {

	private boolean started;
	private final CL nightmareCircusLocation;
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * 停止活动的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopNightmareCircus();

	/**
	 * 启动活动的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startNightmareCircus();

	/**
	 * 绑定梦魇马戏团地点。
	 * Binds the Nightmare Circus location.
	 *
	 * @param nightmareCircusLocation 马戏团地点 / Circus location
	 */
	public CircusInstance(CL nightmareCircusLocation) {
		this.nightmareCircusLocation = nightmareCircusLocation;
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
		startNightmareCircus();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (closed.compareAndSet(false, true)) {
			stopNightmareCircus();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(NightmareCircusStateType type) {
		GameLocationBootstrapServices.nightmareCircusService().spawn(getNightmareCircusLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.nightmareCircusService().despawn(getNightmareCircusLocation());
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
	 * @return 绑定地点 / Bound location
	 */
	public CL getNightmareCircusLocation() {
		return nightmareCircusLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / Location id
	 */
	public int getNightmareCircusLocationId() {
		return nightmareCircusLocation.getId();
	}
}
