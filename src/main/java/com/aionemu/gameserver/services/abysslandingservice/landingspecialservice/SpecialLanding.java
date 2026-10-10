package com.aionemu.gameserver.services.abysslandingservice.landingspecialservice;

import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import lombok.RequiredArgsConstructor;

/**
 * 特殊欧比斯着陆点生命周期抽象基类。
 * Abstract special abyss-landing lifecycle base.
 * @param <RL> 特殊着陆点位置类型 / Special landing-location type
 */
@RequiredArgsConstructor
public abstract class SpecialLanding<RL extends LandingSpecialLocation> {
	private boolean started;
	private final RL spacialLandingLocation;

	/**
	 * 停止特殊着陆点（子类实现去刷细节）。
	 * Stop the special landing (subclass implements despawn details).
	 */
	protected abstract void stopLanding();

	/**
	 * 启动特殊着陆点刷怪。
	 * Start special-landing spawns.
	 */
	protected abstract void startLanding();

	/**
	 * 幂等启动：已启动则直接返回。
	 * Idempotent start; no-ops when already started.
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
		startLanding();
	}

	/**
	 * 停止特殊着陆点。
	 * Stop the special landing.
	 */
	public final void stop() {
		stopLanding();
	}

	/**
	 * 按状态类型刷出 NPC。
	 * Spawn NPCs for the given special state type.
	 * @param type 特殊着陆状态 / Special landing state type
	 */
	protected void spawn(LandingSpecialStateType type) {
		AbyssLandingSpecialService.spawn(getSpecialLandingLocation(), type);
	}

	/**
	 * 清除当前位置已刷出的实体。
	 * Despawn entities already spawned at this location.
	 */
	protected void despawn() {
		AbyssLandingSpecialService.despawn(getSpecialLandingLocation());
	}

	/**
	 * @return 特殊着陆点位置 / Special landing location
	 */
	public RL getSpecialLandingLocation() {
		return spacialLandingLocation;
	}
}
