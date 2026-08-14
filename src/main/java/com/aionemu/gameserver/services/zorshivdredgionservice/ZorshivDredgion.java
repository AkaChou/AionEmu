package com.aionemu.gameserver.services.zorshivdredgionservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionLocation;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionStateType;

/**
 * 佐尔希夫挖掘舰活动抽象基类。
 * Abstract base for Zorshiv dredgion world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <ZL> 挖掘舰地点类型 / dredgion location type
 */
public abstract class ZorshivDredgion<ZL extends ZorshivDredgionLocation> {

	private boolean started;
	private final ZL zorshivDredgionLocation;
	private final AtomicBoolean peace = new AtomicBoolean();

	/**
	 * 停止活动的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopZorshivDredgion();

	/**
	 * 启动活动的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startZorshivDredgion();

	/**
	 * 绑定挖掘舰地点。
	 * Binds the dredgion location.
	 *
	 * @param zorshivDredgionLocation 挖掘舰地点 / dredgion location
	 */
	public ZorshivDredgion(ZL zorshivDredgionLocation) {
		this.zorshivDredgionLocation = zorshivDredgionLocation;
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
		startZorshivDredgion();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (peace.compareAndSet(false, true)) {
			stopZorshivDredgion();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(ZorshivDredgionStateType type) {
		GameLocationBootstrapServices.zorshivDredgionService().spawn(getZorshivDredgionLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.zorshivDredgionService().despawn(getZorshivDredgionLocation());
	}

	/**
	 * 是否已回到和平态。
	 * Whether the event is back to peace.
	 *
	 * @return 和平态则为 true / true if peace
	 */
	public boolean isPeace() {
		return peace.get();
	}

	/**
	 * 获取绑定地点。
	 * Returns the bound location.
	 *
	 * @return 绑定地点 / bound location
	 */
	public ZL getZorshivDredgionLocation() {
		return zorshivDredgionLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getZorshivDredgionLocationId() {
		return zorshivDredgionLocation.getId();
	}
}
