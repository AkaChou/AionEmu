package com.aionemu.gameserver.services.moltenusservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.moltenus.MoltenusLocation;
import com.aionemu.gameserver.model.moltenus.MoltenusStateType;

/**
 * 熔岩领主（Moltenus）活动抽象基类。
 * Abstract base for Moltenus world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <ML> 熔岩领主地点类型 / Moltenus location type
 */
public abstract class MoltenusFight<ML extends MoltenusLocation> {

	private boolean started;
	private final ML moltenusLocation;
	private final AtomicBoolean finished = new AtomicBoolean();

	/**
	 * 停止活动的具体实现。
	 * Concrete stop logic.
	 */
	protected abstract void stopMoltenus();

	/**
	 * 启动活动的具体实现。
	 * Concrete start logic.
	 */
	protected abstract void startMoltenus();

	/**
	 * 绑定熔岩领主地点。
	 * Binds the Moltenus location.
	 *
	 * @param moltenusLocation 熔岩领主地点 / Moltenus location
	 */
	public MoltenusFight(ML moltenusLocation) {
		this.moltenusLocation = moltenusLocation;
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
		startMoltenus();
	}

	/**
	 * 停止活动（仅首次生效）。
	 * Stops the event (first call only).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopMoltenus();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(MoltenusStateType type) {
		GameLocationBootstrapServices.moltenusService().spawn(getMoltenusLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.moltenusService().despawn(getMoltenusLocation());
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
	 * @return 绑定地点 / bound location
	 */
	public ML getMoltenusLocation() {
		return moltenusLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getMoltenusLocationId() {
		return moltenusLocation.getId();
	}
}
