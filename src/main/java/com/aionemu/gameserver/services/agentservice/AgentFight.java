package com.aionemu.gameserver.services.agentservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.agent.AgentLocation;
import com.aionemu.gameserver.model.agent.AgentStateType;

/**
 * 代理战（Agent Fight）活动抽象基类。
 * Abstract base for Agent Fight world events.
 *
 * <p>封装启动/停止幂等守卫与按状态刷怪/清怪。
 * Encapsulates idempotent start/stop guards and spawn/despawn by state type.</p>
 *
 * @author Rinzler (Encom)
 * @param <AL> 代理地点类型 / agent location type
 */
public abstract class AgentFight<AL extends AgentLocation> {

	private boolean started;
	private final AL agentLocation;
	private final AtomicBoolean finished = new AtomicBoolean();

	/**
	 * 停止战斗的具体实现。
	 * Concrete stop-fight logic.
	 */
	protected abstract void stopAgentFight();

	/**
	 * 启动战斗的具体实现。
	 * Concrete start-fight logic.
	 */
	protected abstract void startAgentFight();

	/**
	 * 绑定代理地点。
	 * Binds the agent location.
	 *
	 * location
	 */
	public AgentFight(AL agentLocation) {
		this.agentLocation = agentLocation;
	}

	/**
	 * 启动战斗（幂等）。
	 * Starts the fight (idempotent).
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
		startAgentFight();
	}

	/**
	 * 停止战斗（仅首次生效）。
	 * Stops the fight (first call only).
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			stopAgentFight();
		}
	}

	/**
	 * 按状态类型刷新刷怪。
	 * Spawns entities by state type.
	 *
	 * @param type 状态类型 / state type
	 */
	protected void spawn(AgentStateType type) {
		GameLocationBootstrapServices.agentService().spawn(getAgentLocation(), type);
	}

	/**
	 * 清除该地点刷怪。
	 * Despawns entities for this location.
	 */
	protected void despawn() {
		GameLocationBootstrapServices.agentService().despawn(getAgentLocation());
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
	public AL getAgentLocation() {
		return agentLocation;
	}

	/**
	 * 获取地点 ID。
	 * Returns the location id.
	 *
	 * location id
	 */
	public int getAgentLocationId() {
		return agentLocation.getId();
	}
}
