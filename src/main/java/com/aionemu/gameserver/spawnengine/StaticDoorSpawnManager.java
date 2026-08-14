package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
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
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;

/**
 * 静态门刷怪管理器，按世界与实例生成门并同步地理状态。
 * Static door spawn manager; creates doors per world/instance and syncs geo state.
 *
 * @author MrPoke
 */
@Slf4j
public class StaticDoorSpawnManager {

	/**
	 * 在指定世界与实例中刷出所有静态门。
	 * Spawns all static doors for the world and instance.
	 *
	 * @param worldId 世界 ID / world id
	 * @param instanceIndex 实例索引 / instance index
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
			log.info(I18n.get("log.cb79035d4670", worldId, instanceIndex, counter));
		}
	}

	/**
	 * 将门对象登记、定位并刷入世界。
	 * Stores, positions and spawns a door into the world.
	 *
	 * @param visibleObject 可见对象 / the visible object
	 * @param spawn 刷怪模板 / spawn template
	 * @param instanceIndex 实例索引 / instance index
	 */
	private static void bringIntoWorld(VisibleObject visibleObject, SpawnTemplate spawn, int instanceIndex) {
		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		world.storeObject(visibleObject);
		world.setPosition(visibleObject, spawn.getWorldId(), instanceIndex, spawn.getX(), spawn.getY(), spawn.getZ(),
				spawn.getHeading());
		world.spawn(visibleObject);
	}
}
