/*
/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services.abysslandingservice;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.AbyssLandingConfig;
import com.aionemu.gameserver.model.landing.LandingLocation;

public class LandingUpdateService {
	private static volatile ObjectProvider<LandingUpdateService> instanceProvider;
	private static final Logger log = LoggerFactory.getLogger(LandingUpdateService.class);

	final LandingLocation redemptionLanding = GameLocationBootstrapServices.abyssLandingService().redemptionLanding();
	final LandingLocation harbingerLanding = GameLocationBootstrapServices.abyssLandingService().harbingerLanding();

	// Quest Points.
	final int redemptionPts = redemptionLanding.getQuestPoints() - redemptionLanding.getQuestPoints();
	final int harbingerPts = harbingerLanding.getQuestPoints() - harbingerLanding.getQuestPoints();

	// Monument Points.
	final int redemptionPts1 = redemptionLanding.getMonumentsPoints() - redemptionLanding.getMonumentsPoints();
	final int harbingerPts1 = harbingerLanding.getMonumentsPoints() - harbingerLanding.getMonumentsPoints();

	// Facility Points.
	final int redemptionPts2 = redemptionLanding.getFacilityPoints() - redemptionLanding.getFacilityPoints();
	final int harbingerPts2 = harbingerLanding.getFacilityPoints() - harbingerLanding.getFacilityPoints();

	// Commander Points.
	final int redemptionPts3 = redemptionLanding.getCommanderPoints() - redemptionLanding.getCommanderPoints();
	final int harbingerPts3 = harbingerLanding.getCommanderPoints() - harbingerLanding.getCommanderPoints();

	public LandingUpdateService() {
	}

	public void initResetQuestPoints() {
		if (AbyssLandingConfig.ABYSS_LANDING_QUEST_RESET_ENABLED) {
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					resetQuestPoints();
				}
			}, AbyssLandingConfig.ABYSS_LANDING_QUEST_RESET_TIME);
		}
	}

	public void initResetAbyssLandingPoints() {
		if (AbyssLandingConfig.ABYSS_LANDING_POINTS_RESET_ENABLED) {
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					resetMonumentPoints();
					resetFacilityPoints();
					resetCommanderPoints();
				}
			}, AbyssLandingConfig.ABYSS_LANDING_POINTS_RESET_TIME);
		}
	}

	public void resetQuestPoints() {
		log.debug("##### Abyss Landing Reset Quest Points #####");
		long startTime = System.currentTimeMillis();
		// Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts);
		redemptionLanding.setQuestPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts);
		harbingerLanding.setQuestPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
	}

	public void resetMonumentPoints() {
		log.debug("##### Abyss Landing Reset Monuments Points #####");
		long startTime = System.currentTimeMillis();
		// Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts1);
		redemptionLanding.setMonumentsPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts1);
		harbingerLanding.setMonumentsPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
	}

	public void resetFacilityPoints() {
		log.debug("##### Abyss Landing Reset Facility Points #####");
		long startTime = System.currentTimeMillis();
		// Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts2);
		redemptionLanding.setFacilityPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts2);
		harbingerLanding.setFacilityPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
	}

	public void resetCommanderPoints() {
		log.debug("##### Abyss Landing Reset Commander Points #####");
		long startTime = System.currentTimeMillis();
		// Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts3);
		redemptionLanding.setCommanderPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts3);
		harbingerLanding.setCommanderPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
	}

	public static LandingUpdateService getInstance() {
		ObjectProvider<LandingUpdateService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<LandingUpdateService> instanceProvider) {
		LandingUpdateService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final LandingUpdateService instance = new LandingUpdateService();
	}
}
