package com.aionemu.gameserver.model.curingzone;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.curingzones.CuringTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.NpcKnownList;

/**
 * 治疗对象，用于 curingzone 相关逻辑。
 * Curing Object for curingzone logic.
 */

public class CuringObject extends VisibleObject {

	private CuringTemplate template;
	private float range;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CuringObject(CuringTemplate template, int instanceId) {
		super(GameWorldBootstrapServices.idFactory().nextId(), new VisibleObjectController() {
		}, null, null, com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(template.getMapId(), template.getX(), template.getY(),
				template.getZ(), (byte) 0, instanceId));

		this.template = template;
		range = template.getRange();
		setKnownlist(new NpcKnownList(this));
	}

	/** 获取模板。 / Returns the template. */
	public CuringTemplate getTemplate() {
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
