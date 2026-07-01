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

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftLocation;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns.DynamicRiftSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.dynamicriftservice.DynamicRift;
import com.aionemu.gameserver.services.dynamicriftservice.Portal;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Rinzler (Encom)
 */
@Slf4j

public class DynamicRiftService {
	private static volatile ObjectProvider<DynamicRiftService> instanceProvider;
	private Map<Integer, DynamicRiftLocation> dynamicRift;
	private static final int duration = CustomConfig.DYNAMIC_RIFT_DURATION;
	private final Map<Integer, DynamicRift<?>> activeDynamicRift = new LinkedHashMap<Integer, DynamicRift<?>>();

	public void initDynamicRiftLocations() {
		if (CustomConfig.DYNAMIC_RIFT_ENABLED) {
			dynamicRift = DataManager.DYNAMIC_RIFT_DATA.getDynamicRiftLocations();
			for (DynamicRiftLocation loc : getDynamicRiftLocations().values()) {
				spawn(loc, DynamicRiftStateType.CLOSED);
			}
			log.info("[DynamicRiftService] Loaded " + dynamicRift.size() + " locations.");
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					startDynamicRift(1);
					startDynamicRift(3);
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendPacket(player,
									SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_PORTAL_OPEN_IDDF3_Dragon);
						}
					});
				}
			}, CustomConfig.DYNAMIC_RIFT_DRAGON_SCHEDULE);
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					startDynamicRift(2);
					startDynamicRift(4);
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendPacket(player,
									SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_PORTAL_OPEN_IDLF3_Castle_Indratoo);
						}
					});
				}
			}, CustomConfig.DYNAMIC_RIFT_INDRATOO_SCHEDULE);
			// Shugo Merchant League
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					startDynamicRift(5);
					startDynamicRift(6);
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							// The Shugo Merchant League has arrived.
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HF_ShugoCaravanAppear);
						}
					});
				}
			}, CustomConfig.SHUGO_MERCHANT_LEAGUE_SCHEDULE);
		} else {
			dynamicRift = Collections.emptyMap();
		}
	}

	public void initDynamicRift() {
		if (CustomConfig.DYNAMIC_RIFT_ENABLED) {
			log.info("[DynamicRiftService] is initialized...");
		} else {
			log.info("[DynamicRiftService] DynamicRift is disabled in config...");
		}
	}

	public void startDynamicRift(final int id) {
		final DynamicRift<?> portal;
		synchronized (this) {
			if (activeDynamicRift.containsKey(id)) {
				return;
			}
			portal = new Portal(dynamicRift.get(id));
			activeDynamicRift.put(id, portal);
		}
		portal.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopDynamicRift(id);
			}
		}, duration * 3600 * 1000);
	}

	public void stopDynamicRift(int id) {
		if (!isDynamicRiftInProgress(id)) {
			return;
		}
		DynamicRift<?> portal;
		synchronized (this) {
			portal = activeDynamicRift.remove(id);
		}
		if (portal == null || portal.isClosed()) {
			return;
		}
		portal.stop();
	}

	public void spawn(DynamicRiftLocation loc, DynamicRiftStateType dstate) {
		if (dstate.equals(DynamicRiftStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getDynamicRiftSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				DynamicRiftSpawnTemplate dynamicRifttemplate = (DynamicRiftSpawnTemplate) st;
				if (dynamicRifttemplate.getDStateType().equals(dstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(dynamicRifttemplate, 1));
				}
			}
		}
	}

	public void despawn(DynamicRiftLocation loc) {
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

	public boolean isDynamicRiftInProgress(int id) {
		return activeDynamicRift.containsKey(id);
	}

	public Map<Integer, DynamicRift<?>> getActiveDynamicRift() {
		return activeDynamicRift;
	}

	public int getDuration() {
		return duration;
	}

	public DynamicRiftLocation getDynamicRiftLocation(int id) {
		return dynamicRift.get(id);
	}

	public Map<Integer, DynamicRiftLocation> getDynamicRiftLocations() {
		return dynamicRift;
	}

	public static DynamicRiftService getInstance() {
		ObjectProvider<DynamicRiftService> provider = instanceProvider;
		if (provider == null) {
			return DynamicRiftServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> DynamicRiftServiceHolder.INSTANCE);
	}

	public static void setInstanceProvider(ObjectProvider<DynamicRiftService> instanceProvider) {
		DynamicRiftService.instanceProvider = instanceProvider;
	}

	private static class DynamicRiftServiceHolder {
		private static final DynamicRiftService INSTANCE = new DynamicRiftService();
	}
}
