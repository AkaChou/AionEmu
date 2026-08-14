package com.aionemu.gameserver.services.idiandepthsservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsLocation;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;

/**
 * 伊迪安深渊活动抽象基类。
 * Abstract base for Idian Depths world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <IL> 伊迪安深渊地点类型 / Idian Depths location type
 */
public abstract class IdianDepths<IL extends IdianDepthsLocation> {

	private boolean started;
	private final IL idianDepthsLocation;
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * 关闭活动的具体实现。
	 * Concrete close logic.
	 */
	protected abstract void stopIdianDepths();

	/**
	 * 开启活动的具体实现。
	 * Concrete open logic.
	 */
	protected abstract void startIdianDepths();

	/**
	 * 绑定伊迪安深渊地点。
	 * Binds the Idian Depths location.
	 *
	 * @param idianDepthsLocation 伊迪安深渊地点 / Idian Depths location
	 */
	public IdianDepths(IL idianDepthsLocation) {
		this.idianDepthsLocation = idianDepthsLocation;
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
		startIdianDepths();
	}

	/**
	 * 关闭活动（仅首次生效）。
	 * Closes the event (first call only).
	 */
	public final void stop() {
		if (closed.compareAndSet(false, true)) {
			stopIdianDepths();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(IdianDepthsStateType type) {
		GameLocationBootstrapServices.idianDepthsService().spawn(getIdianDepthsLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.idianDepthsService().despawn(getIdianDepthsLocation());
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
	 * @return 绑定地点 / bound location
	 */
	public IL getIdianDepthsLocation() {
		return idianDepthsLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getIdianDepthsLocationId() {
		return idianDepthsLocation.getId();
	}
}
