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
package com.aionemu.gameserver.spawnengine;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.StaticObjectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.staticdoor.DoorType;
import com.aionemu.gameserver.model.templates.staticdoor.StaticDoorTemplate;
import com.aionemu.gameserver.model.templates.staticdoor.StaticDoorWorld;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;

/**
 * @author MrPoke
 */
@Slf4j
public class StaticDoorSpawnManager {


	/**
	 * @param worldId
	 * @param instanceIndex
	 */
	public static void spawnTemplate(int worldId, int instanceIndex) {
		StaticDoorWorld staticDoorWorld = DataManager.STATICDOOR_DATA.getStaticDoorWorlds(worldId);
		if (staticDoorWorld == null) {
			return;
		}
		int counter = 0;
		for (StaticDoorTemplate data : staticDoorWorld.getStaticDoors()) {
			if (data.getDoorType() != DoorType.DOOR) {
				continue;
			}
			SpawnTemplate spawn = new SpawnTemplate(new SpawnGroup2(worldId, 300001), data.getX(), data.getY(),
					data.getZ(), (byte) 0, 0, null, 0, 0);
			spawn.setStaticId(data.getDoorId());
			int objectId = GameWorldBootstrapServices.idFactory().nextId();
			StaticDoor staticDoor = new StaticDoor(objectId, new StaticObjectController(), spawn, data, instanceIndex);
			staticDoor.setKnownlist(new PlayerAwareKnownList(staticDoor));
			bringIntoWorld(staticDoor, spawn, instanceIndex);
			GameWorldServices.geoService().setDoorState(worldId, instanceIndex, data.getDoorId(), staticDoor.isOpen());
			counter++;
		}
		if (counter > 0) {
			log.info("Spawned static doors: " + worldId + " [" + instanceIndex + "] : " + counter);
		}
	}

	/**
	 * @param visibleObject
	 * @param spawn
	 * @param instanceIndex
	 */
	private static void bringIntoWorld(VisibleObject visibleObject, SpawnTemplate spawn, int instanceIndex) {
		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		world.storeObject(visibleObject);
		world.setPosition(visibleObject, spawn.getWorldId(), instanceIndex, spawn.getX(), spawn.getY(), spawn.getZ(),
				spawn.getHeading());
		world.spawn(visibleObject);
	}
}
