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
package com.aionemu.gameserver.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssSpecialLandingDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.landingspecialspawns.LandingSpecialSpawnTemplate;
import com.aionemu.gameserver.services.abysslandingservice.landingspecialservice.SPLanding;
import com.aionemu.gameserver.services.abysslandingservice.landingspecialservice.SpecialLanding;
import com.aionemu.gameserver.spawnengine.SpawnEngine;

@Slf4j(topic = "com.aionemu.gameserver.services.AbyssLandingService")
public class AbyssLandingSpecialService {
	private static volatile ObjectProvider<AbyssLandingSpecialService> instanceProvider;
	private static Map<Integer, LandingSpecialLocation> abyssSpecialLanding;
	private final ConcurrentMap<Integer, SpecialLanding<?>> activeSpecialLanding = new ConcurrentHashMap<Integer, SpecialLanding<?>>();

	public void initLandingSpecialLocations() {
		abyssSpecialLanding = DataManager.LANDING_SPECIAL_LOCATION_DATA.getLandingSpecialLocations();
		DAOManager.getDAO(AbyssSpecialLandingDAO.class).loadLandingSpecialLocations(abyssSpecialLanding);
		for (LandingSpecialLocation loc : getLandingSpecialLocations().values()) {
			if (loc.getType().equals(LandingSpecialStateType.ACTIVE)) {
				spawn(loc, LandingSpecialStateType.ACTIVE);
			}
			log.info("[Abyss Landing Monument] ID: " + loc.getId() + " - STATUS: " + loc.getType());
		}
		log.info("[Abyss Landing Monument] Loaded " + abyssSpecialLanding.size() + " Locations");
	}

	public void startLanding(final int id) {
		SpecialLanding<?> land = new SPLanding(abyssSpecialLanding.get(id));
		if (activeSpecialLanding.putIfAbsent(id, land) != null) {
			return;
		}
		land.start();
	}

	public void stopLanding(int id) {
		SpecialLanding<?> landing = activeSpecialLanding.remove(id);
		if (landing == null) {
			return;
		}
		landing.stop();
	}

	public static void spawn(LandingSpecialLocation loc, LandingSpecialStateType fstate) {
		if (fstate.equals(LandingSpecialStateType.ACTIVE)) {
			List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getLandingSpecialSpawnsByLocId(loc.getId());
			for (SpawnGroup2 group : locSpawns) {
				for (SpawnTemplate st : group.getSpawnTemplates()) {
					LandingSpecialSpawnTemplate landingtTemplate = (LandingSpecialSpawnTemplate) st;
					if (landingtTemplate.getFStateType().equals(fstate)) {
						loc.getSpawned().add(SpawnEngine.spawnObject(landingtTemplate, 1));
					}
				}
			}
		}
	}

	public static void onSave(LandingSpecialLocation loc) {
		getDAO().updateLocation(loc);
	}

	public static void despawn(LandingSpecialLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<VisibleObject>(loc.getSpawned())) {
			Npc spawned = (Npc) obj;
			spawned.setDespawnDelayed(true);
			if (spawned.getAggroList().getList().isEmpty()) {
				spawned.getController().cancelTask(TaskId.RESPAWN);
				obj.getController().onDelete();
			}
		}
		loc.getSpawned().clear();
	}

	public static AbyssLandingSpecialService getInstance() {
		ObjectProvider<AbyssLandingSpecialService> provider = instanceProvider;
		if (provider == null) {
			return AbyssLandingSpecialService.SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> AbyssLandingSpecialService.SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<AbyssLandingSpecialService> instanceProvider) {
		AbyssLandingSpecialService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final AbyssLandingSpecialService instance = new AbyssLandingSpecialService();
	}

	public LandingSpecialLocation getLandingSpecialLocation(int id) {
		return abyssSpecialLanding.get(id);
	}

	public static Map<Integer, LandingSpecialLocation> getLandingSpecialLocations() {
		return abyssSpecialLanding;
	}

	public static AbyssSpecialLandingDAO getDAO() {
		return DAOManager.getDAO(AbyssSpecialLandingDAO.class);
	}
}
