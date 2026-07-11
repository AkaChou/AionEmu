package com.aionemu.gameserver.services.instanceriftservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.instancerift.InstanceRiftLocation;
import com.aionemu.gameserver.model.instancerift.InstanceRiftStateType;

/**
 * 副本裂隙活动抽象基类。
 * Abstract base for instance-rift world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <RL> 裂隙地点类型 / rift location type
 */
public abstract class RiftInstance<RL extends InstanceRiftLocation> {

	private boolean started;
	private final RL instanceRiftLocation;
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * 关闭裂隙的具体实现。
	 * Concrete close logic.
	 */
	protected abstract void stopInstanceRift();

	/**
	 * 开启裂隙的具体实现。
	 * Concrete open logic.
	 */
	protected abstract void startInstanceRift();

	/**
	 * 绑定裂隙地点。
	 * Binds the rift location.
	 *
	 * location
	 */
	public RiftInstance(RL instanceRiftLocation) {
		this.instanceRiftLocation = instanceRiftLocation;
	}

	/**
	 * 开启裂隙（幂等）。
	 * Opens the rift (idempotent).
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
		startInstanceRift();
	}

	/**
	 * 关闭裂隙（仅首次生效）。
	 * Closes the rift (first call only).
	 */
	public final void stop() {
		if (closed.compareAndSet(false, true)) {
			stopInstanceRift();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(InstanceRiftStateType type) {
		GameLocationBootstrapServices.instanceRiftService().spawn(getInstanceRiftLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.instanceRiftService().despawn(getInstanceRiftLocation());
	}

	/**
	 * 是否已关闭。
	 * Whether the rift is closed.
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
	 * location
	 */
	public RL getInstanceRiftLocation() {
		return instanceRiftLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * location id
	 */
	public int getInstanceRiftLocationId() {
		return instanceRiftLocation.getId();
	}
}
