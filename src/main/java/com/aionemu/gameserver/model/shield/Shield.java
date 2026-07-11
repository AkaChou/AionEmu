package com.aionemu.gameserver.model.shield;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.ShieldController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.shield.ShieldTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.SphereKnownList;

/**
 * 护盾模型。
 * Shield model.
 *
 * @author Wakizashi
 */
public class Shield extends VisibleObject {

	private ShieldTemplate template = null;
	private String name = null;
	private int id = 0;

	public Shield(ShieldTemplate template) {
		super(GameWorldBootstrapServices.idFactory().nextId(), new ShieldController(), null, null, null);

		((ShieldController) getController()).setOwner(this);
		this.template = template;
		this.name = (template.getName() == null) ? "SHIELD" : template.getName();
		this.id = template.getId();
		setKnownlist(new SphereKnownList(this, template.getRadius() * 2));
	}

	/** 获取模板。 / Returns the template. */
	public ShieldTemplate getTemplate() {
		return template;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 生成。 / Spawn. */
	public void spawn() {
		World w = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		WorldPosition position = w.createPosition(template.getMap(), template.getCenter().getX(),
				template.getCenter().getY(), template.getCenter().getZ(), (byte) 0, 0);
		this.setPosition(position);
		w.storeObject(this);
		w.spawn(this);
	}
}
