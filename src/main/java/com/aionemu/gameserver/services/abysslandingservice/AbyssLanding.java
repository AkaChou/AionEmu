package com.aionemu.gameserver.services.abysslandingservice;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssLandingDAO;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.landing.LandingStateType;

/**
 * 标准欧比斯着陆点实现：按等级刷怪/去刷并持久化位置。
 * Standard abyss landing implementation: spawn/despawn by level and persist location.
 */
public class AbyssLanding extends Landing<LandingLocation> {

	/**
	 * @param landing 着陆点位置 / Landing location
	 */
	public AbyssLanding(LandingLocation landing) {
		super(landing);
	}

	/**
	 * 按等级启动着陆点刷怪（1–8 级，非法等级回退 LVL1）。
	 * Start landing spawns by level (1–8; invalid levels fall back to LVL1).
	 *
	 * @param level 着陆等级 / Landing level
	 */
	@Override
	public void startLanding(int level) {
		getLandingLocation().setActiveLanding(this);
		if (!getLandingLocation().getSpawned().isEmpty()) {
			despawn();
		}
		switch (level) {
		case 1:
			spawn(LandingStateType.LVL1);
			break;
		case 2:
			spawn(LandingStateType.LVL2);
			break;
		case 3:
			spawn(LandingStateType.LVL3);
			break;
		case 4:
			spawn(LandingStateType.LVL4);
			break;
		case 5:
			spawn(LandingStateType.LVL5);
			break;
		case 6:
			spawn(LandingStateType.LVL6);
			break;
		case 7:
			spawn(LandingStateType.LVL7);
			break;
		case 8:
			spawn(LandingStateType.LVL8);
			break;
		default:
			spawn(LandingStateType.LVL1);
			break;
		}
	}

	/**
	 * 将着陆点状态写回数据库。
	 * Persist landing location state to the database.
	 */
	public void saveLanding() {
		DAOManager.getDAO(AbyssLandingDAO.class).updateLocation(getLandingLocation());
	}

	/**
	 * 停止着陆点：清空活动引用、去刷并刷 NONE 状态。
	 * Stop landing: clear active ref, despawn, and spawn NONE state.
	 */
	@Override
	public void stopLanding() {
		getLandingLocation().setActiveLanding(null);
		despawn();
		spawn(LandingStateType.NONE);
	}
}
