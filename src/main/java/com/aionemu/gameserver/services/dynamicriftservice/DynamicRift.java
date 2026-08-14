package com.aionemu.gameserver.services.dynamicriftservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftLocation;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;

/**
 * 动态裂隙活动抽象基类。
 * Abstract base for Dynamic Rift world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <DL> 动态裂隙地点类型 / Dynamic Rift location type
 */
public abstract class DynamicRift<DL extends DynamicRiftLocation> {

	private boolean started;
	private final DL dynamicRiftLocation;
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * 停止活动的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopDynamicRift();

	/**
	 * 启动活动的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startDynamicRift();

	/**
	 * 绑定动态裂隙地点。
	 * Binds the Dynamic Rift location.
	 *
	 * @param dynamicRiftLocation 动态裂隙地点 / dynamic rift location
	 */
	public DynamicRift(DL dynamicRiftLocation) {
		this.dynamicRiftLocation = dynamicRiftLocation;
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
		startDynamicRift();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (closed.compareAndSet(false, true)) {
			stopDynamicRift();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(DynamicRiftStateType type) {
		GameLocationBootstrapServices.dynamicRiftService().spawn(getDynamicRiftLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.dynamicRiftService().despawn(getDynamicRiftLocation());
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
	 * @return 动态裂隙地点 / dynamic rift location
	 */
	public DL getDynamicRiftLocation() {
		return dynamicRiftLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getDynamicRiftLocationId() {
		return dynamicRiftLocation.getId();
	}
}
