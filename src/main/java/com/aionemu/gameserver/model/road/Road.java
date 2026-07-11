package com.aionemu.gameserver.model.road;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.controllers.RoadController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.road.RoadTemplate;
import com.aionemu.gameserver.model.utils3d.Plane3D;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.SphereKnownList;

/**
 * 道路模型。
 * Road model.
 *
 * @author SheppeR
 */
public class Road extends VisibleObject {

	private RoadTemplate template = null;
	private String name = null;
	private Plane3D plane = null;
	private Point3D center = null;
	private Point3D p1 = null;
	private Point3D p2 = null;

	public Road(RoadTemplate template) {
		super(GameWorldBootstrapServices.idFactory().nextId(), new RoadController(), null, null,
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(template.getMap(), template.getCenter().getX(),
						template.getCenter().getY(), template.getCenter().getZ(), (byte) 0, 0));

		((RoadController) getController()).setOwner(this);
		this.template = template;
		this.name = template.getName() == null ? "ROAD" : template.getName();
		this.center = new Point3D(template.getCenter().getX(), template.getCenter().getY(),
				template.getCenter().getZ());
		this.p1 = new Point3D(template.getP1().getX(), template.getP1().getY(), template.getP1().getZ());
		this.p2 = new Point3D(template.getP2().getX(), template.getP2().getY(), template.getP2().getZ());
		this.plane = new Plane3D(center, p1, p2);
		setKnownlist(new SphereKnownList(this, template.getRadius() * 2));
	}

	/** 返回 plane / Returns the plane */
	public Plane3D getPlane() {
		return plane;
	}

	/** 获取模板。 / Returns the template. */
	public RoadTemplate getTemplate() {
		return template;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name;
	}

	/** 生成。 / Spawn. */
	public void spawn() {
		World w = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		w.storeObject(this);
		w.spawn(this);
	}
}
