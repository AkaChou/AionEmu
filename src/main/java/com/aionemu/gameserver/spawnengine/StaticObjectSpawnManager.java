package com.aionemu.gameserver.spawnengine;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.StaticObjectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.StaticObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;

/**
 * 静态物体（如场景道具）的刷怪管理器。
 * Spawn manager for static objects such as scene props.
 *
 * @author ATracer
 */
public class StaticObjectSpawnManager {

	/**
	 * 按刷怪组在指定实例中刷出静态物体。
	 * Spawns static objects from a spawn group into the given instance.
	 *
	 * @param spawn 刷怪组 / the spawn group
	 * @param instanceIndex 实例索引 / instance index
	 */
	public static void spawnTemplate(SpawnGroup2 spawn, int instanceIndex) {
		VisibleObjectTemplate objectTemplate = DataManager.ITEM_DATA.getItemTemplate(spawn.getNpcId());
		if (objectTemplate == null) {
			return;
		}
		if (spawn.hasPool()) {
			spawn.resetTemplates(instanceIndex);
			for (int i = 0; i < spawn.getPool(); i++) {
				SpawnTemplate template = spawn.getRndTemplate(instanceIndex);
				int objectId = GameWorldBootstrapServices.idFactory().nextId();
				StaticObject staticObject = new StaticObject(objectId, new StaticObjectController(), template,
						objectTemplate);
				staticObject.setKnownlist(new PlayerAwareKnownList(staticObject));
				bringIntoWorld(staticObject, template, instanceIndex);
			}
		} else {
			for (SpawnTemplate template : spawn.getSpawnTemplates()) {
				int objectId = GameWorldBootstrapServices.idFactory().nextId();
				StaticObject staticObject = new StaticObject(objectId, new StaticObjectController(), template,
						objectTemplate);
				staticObject.setKnownlist(new PlayerAwareKnownList(staticObject));
				bringIntoWorld(staticObject, template, instanceIndex);
			}
		}
	}

	/**
	 * 将可见对象登记、定位并刷入世界。
	 * Stores, positions and spawns a visible object into the world.
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
