package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.controllers.GatherableController;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.gather.GatherableTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 可采集物游戏对象。
 * Gatherable game object.
 *
 * @author ATracer
 */
public class Gatherable extends VisibleObject {

	public Gatherable(SpawnTemplate spawnTemplate, VisibleObjectTemplate objectTemplate, int objId,
			GatherableController controller) {
		super(objId, controller, spawnTemplate, objectTemplate, new WorldPosition(spawnTemplate.getWorldId()));
		controller.setOwner(this);
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return objectTemplate.getName();
	}

	/** 获取对象模板。 / Returns the object template. */
	@Override
	public GatherableTemplate getObjectTemplate() {
		return (GatherableTemplate) objectTemplate;
	}

	/** 返回控制器 / Returns the controller */
	@Override
	public GatherableController getController() {
		return (GatherableController) super.getController();
	}
}
