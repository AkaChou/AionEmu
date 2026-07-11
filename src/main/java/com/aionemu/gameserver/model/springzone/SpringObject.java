package com.aionemu.gameserver.model.springzone;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.springzones.SpringTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.NpcKnownList;

/**
 * 温泉对象，用于 springzone 相关逻辑。
 * Spring Object for springzone logic.
 */

public class SpringObject extends VisibleObject {
	private float range;
	private SpringTemplate template;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SpringObject(SpringTemplate template, int instanceId) {
		super(GameWorldBootstrapServices.idFactory().nextId(), new VisibleObjectController() {
		}, null, null, com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(template.getMapId(), template.getX(), template.getY(),
				template.getZ(), (byte) 0, instanceId));
		this.template = template;
		range = template.getRange();
		setKnownlist(new NpcKnownList(this));
	}

	/** 获取模板。 / Returns the template. */
	public SpringTemplate getTemplate() {
		return template;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return "";
	}

	/** 返回范围 / Returns the range*/
	public float getRange() {
		return range;
	}

	/** 生成。 / Spawn. */
	public void spawn() {
		World w = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		w.storeObject(this);
		w.spawn(this);
	}
}
