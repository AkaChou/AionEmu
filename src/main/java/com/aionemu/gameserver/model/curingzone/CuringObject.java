package com.aionemu.gameserver.model.curingzone;


import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.curingzones.CuringTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.NpcKnownList;
import lombok.Getter;

/**
 * 治疗对象，用于 curingzone 相关逻辑。
 * Curing Object for curingzone logic.
 */

@Getter
public class CuringObject extends VisibleObject {

	/** 获取模板。 / Returns the template. */
	private final CuringTemplate template;
	/** 返回范围 / Returns the range*/
	private final float range;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CuringObject(CuringTemplate template, int instanceId) {
		super(IDFactory.getInstance().nextId(), new VisibleObjectController() {
		}, null, null, World.getInstance().createPosition(template.getMapId(), template.getX(), template.getY(),
				template.getZ(), (byte) 0, instanceId));

		this.template = template;
		range = template.getRange();
		setKnownlist(new NpcKnownList(this));
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return "";
	}

	/** 生成。 / Spawn. */
	public void spawn() {
		World w = World.getInstance();
		w.storeObject(this);
		w.spawn(this);
	}
}
