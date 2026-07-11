package com.aionemu.gameserver.model.templates.staticdoor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 静态 DoorBounds 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StaticDoorBounds")
public class StaticDoorBounds {

	@XmlAttribute
	private float x1;

	@XmlAttribute
	private float y1;

	@XmlAttribute
	private float z1;

	@XmlAttribute
	private float x2;

	@XmlAttribute
	private float y2;

	@XmlAttribute
	private float z2;

	@XmlTransient
	private BoundingBox boundingBox;

	/** 返回 bounding box / Returns the bounding box */
	public BoundingBox getBoundingBox() {
		if (boundingBox == null) {
			boundingBox = new BoundingBox(new Vector3f(x1, y1, z1), new Vector3f(x2, y2, z2));
		}
		return boundingBox;
	}
}
