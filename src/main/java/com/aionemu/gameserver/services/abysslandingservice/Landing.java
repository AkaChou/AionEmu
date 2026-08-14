package com.aionemu.gameserver.services.abysslandingservice;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssLandingDAO;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.services.AbyssLandingService;

/**
 * 欧比斯着陆点生命周期抽象基类：启动/停止/更新与刷怪钩子。
 * Abstract abyss-landing lifecycle base: start/stop/update and spawn hooks.
 *
 * @param <RL> 着陆点位置类型 / Landing-location type
 */
public abstract class Landing<RL extends LandingLocation> {
	private int level;
	private boolean started;
	private final RL landingLocation;

	/**
	 * 停止着陆点（子类实现具体去刷逻辑）。
	 * Stop the landing (subclass implements despawn details).
	 */
	protected abstract void stopLanding();

	/**
	 * 持久化着陆点状态（子类实现 DAO 写入）。
	 * Persist landing state (subclass implements DAO write).
	 */
	protected abstract void saveLanding();

	/**
	 * 按等级启动着陆点刷怪。
	 * Start landing spawns at the given level.
	 *
	 * @param level 着陆等级 / Landing level
	 */
	protected abstract void startLanding(int level);

	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * @param landingLocation 着陆点位置 / Landing location
	 */
	public Landing(RL landingLocation) {
		this.landingLocation = landingLocation;
	}

	/**
	 * 幂等启动：已启动则直接返回。
	 * Idempotent start; no-ops when already started.
	 *
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
	 * 触发持久化更新。
	 * Trigger a persistence update.
	 */
	public final void update() {
		saveLanding();
	}

	/**
	 * 按状态类型刷出 NPC。
	 * Spawn NPCs for the given state type.
	 *
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
	 * 着陆点是否已关闭。
	 * Whether the landing is closed.
	 *
	 * @return 是否已关闭 / closed flag
	 */
	public boolean isClosed() {
		return closed.get();
	}

	/**
	 * @return 着陆点位置 / Landing location
	 */
	public RL getLandingLocation() {
		return landingLocation;
	}

	/**
	 * @return 着陆点位置 ID / Landing location id
	 */
	public int getLandingLocationId() {
		return landingLocation.getId();
	}

	/**
	 * 当前等级。
	 * Current level.
	 */
	public int getLevel() {
		return this.level;
	}

	/**
	 * @param level 着陆等级 / Landing level
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * 欧比斯着陆点 DAO。
	 * Landing DAO.
	 */
	private AbyssLandingDAO getDAO() {
		return DAOManager.getDAO(AbyssLandingDAO.class);
	}
}
