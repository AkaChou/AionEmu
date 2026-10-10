package com.aionemu.gameserver.services.abysslandingservice;

import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.services.AbyssLandingService;
import lombok.RequiredArgsConstructor;

/**
 * 欧比斯着陆点生命周期抽象基类：启动/停止与刷怪钩子。
 * Abstract abyss-landing lifecycle base: start/stop and spawn hooks.
 * @param <RL> 着陆点位置类型 / Landing-location type
 */
@RequiredArgsConstructor
public abstract class Landing<RL extends LandingLocation> {
	private boolean started;
	private final RL landingLocation;

	/**
	 * 停止着陆点（子类实现具体去刷逻辑）。
	 * Stop the landing (subclass implements despawn details).
	 */
	protected abstract void stopLanding();

	/**
	 * 按等级启动着陆点刷怪。
	 * Start landing spawns at the given level.
	 * @param level 着陆等级 / Landing level
	 */
	protected abstract void startLanding(int level);

	/**
	 * 幂等启动：已启动则直接返回。
	 * Idempotent start; no-ops when already started.
	 * @param level 着陆等级 / Landing level
	 */
	public final void start(int level) {
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
		startLanding(level);
	}

	/**
	 * 停止着陆点。
	 * Stop the landing.
	 */
	public final void stop() {
		stopLanding();
	}

	/**
	 * 按状态类型刷出 NPC。
	 * Spawn NPCs for the given state type.
	 * @param type 着陆状态 / Landing state type
	 */
	protected void spawn(LandingStateType type) {
		AbyssLandingService.spawn(getLandingLocation(), type);
	}

	/**
	 * 清除当前位置已刷出的实体。
	 * Despawn entities already spawned at this location.
	 */
	protected void despawn() {
		AbyssLandingService.despawn(getLandingLocation());
	}

	/**
	 * 获取着陆点位置。
	 * Returns the landing location.
	 * @return 着陆点位置 / Landing location
	 */
	public RL getLandingLocation() {
		return landingLocation;
	}
}
