package com.aionemu.gameserver.model.flyring;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.FlyRingController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Plane3D;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.SphereKnownList;

/**
 * 飞行光环模型。
 * Fly Ring model.
 */

public class FlyRing extends VisibleObject {
	private FlyRingTemplate template = null;
	private String name = null;
	private Plane3D plane = null;
	private Point3D center = null;
	private Point3D left = null;
	private Point3D right = null;

	public FlyRing(FlyRingTemplate template, int instanceId) {
		super(GameWorldBootstrapServices.idFactory().nextId(), new FlyRingController(), null, null,
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(template.getMap(), template.getCenter().getX(),
						template.getCenter().getY(), template.getCenter().getZ(), (byte) 0, instanceId));
		((FlyRingController) getController()).setOwner(this);
		this.template = template;
		this.name = (template.getName() == null) ? "FLY_RING" : template.getName();
		this.center = new Point3D(template.getCenter().getX(), template.getCenter().getY(),
				template.getCenter().getZ());
		this.left = new Point3D(template.getLeft().getX(), template.getLeft().getY(), template.getLeft().getZ());
		this.right = new Point3D(template.getRight().getX(), template.getRight().getY(), template.getRight().getZ());
		this.plane = new Plane3D(center, left, right);
		setKnownlist(new SphereKnownList(this, template.getRadius() * 2));
	}

	/** 返回 plane / Returns the plane */
	public Plane3D getPlane() {
		return plane;
	}

	/** 获取模板。 / Returns the template. */
	public FlyRingTemplate getTemplate() {
		return template;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name;
	}

	/** 生成。 / Spawn. */
	public void spawn() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(this);
	}
}
