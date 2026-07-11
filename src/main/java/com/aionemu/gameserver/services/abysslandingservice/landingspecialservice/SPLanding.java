package com.aionemu.gameserver.services.abysslandingservice.landingspecialservice;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssSpecialLandingDAO;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;

/**
 * 特殊欧比斯着陆点实现：激活/停用刷怪并持久化。
 * Special abyss-landing implementation: activate/deactivate spawns and persist.
 */
public class SPLanding extends SpecialLanding<LandingSpecialLocation> {

	/**
	 * @param landing 特殊着陆点位置 / Special landing location
	 */
	public SPLanding(LandingSpecialLocation landing) {
		super(landing);
	}

	/**
	 * 激活特殊着陆点（先去刷再刷 ACTIVE）。
	 * Activate the special landing (despawn first, then spawn ACTIVE).
	 */
	@Override
	public void startLanding() {
		getSpecialLandingLocation().setActiveLanding(this);
		if (!getSpecialLandingLocation().getSpawned().isEmpty()) {
			despawn();
		}
		spawn(LandingSpecialStateType.ACTIVE);
	}

	/**
	 * 将特殊着陆点状态写回数据库。
	 * Persist special landing location state to the database.
	 */
	public void saveLanding() {
		DAOManager.getDAO(AbyssSpecialLandingDAO.class).updateLocation(getSpecialLandingLocation());
	}

	/**
	 * 停用特殊着陆点：清空活动引用、去刷并刷 NO_ACTIVE。
	 * Deactivate special landing: clear active ref, despawn, spawn NO_ACTIVE.
	 */
	@Override
	public void stopLanding() {
		getSpecialLandingLocation().setActiveLanding(null);
		despawn();
		spawn(LandingSpecialStateType.NO_ACTIVE);
	}
}
