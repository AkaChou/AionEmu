package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.controllers.StaticObjectController;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 静态对象。
 * Static Object game object.
 *
 * @author ATracer
 */
public class StaticObject extends VisibleObject {

	public StaticObject(int objectId, StaticObjectController controller, SpawnTemplate spawnTemplate,
			VisibleObjectTemplate objectTemplate) {
		super(objectId, controller, spawnTemplate, objectTemplate, new WorldPosition(spawnTemplate.getWorldId()));
		controller.setOwner(this);
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return objectTemplate.getName();
	}
}
