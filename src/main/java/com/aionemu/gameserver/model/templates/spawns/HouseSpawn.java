package com.aionemu.gameserver.model.templates.spawns;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 房屋刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HouseSpawn")
public class HouseSpawn {
	@XmlAttribute(name = "x", required = true)
	protected float x;

	@XmlAttribute(name = "y", required = true)
	protected float y;

	@XmlAttribute(name = "z", required = true)
	protected float z;

	@XmlAttribute(name = "h")
	protected Byte h;

	@XmlAttribute(name = "entity_id")
	private int entityId;

	@XmlAttribute(name = "type", required = true)
	protected SpawnType type;

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 设置 x / Sets the x */
	public void setX(float value) {
		x = value;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 设置 y / Sets the y */
	public void setY(float value) {
		y = value;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 设置 z / Sets the z */
	public void setZ(float value) {
		z = value;
	}

	/** 返回 h / Returns the h */
	public byte getH() {
		if (h == null) {
			return 0;
		}
		return h.byteValue();
	}

	/** 设置 h / Sets the h */
	public void setH(Byte value) {
		h = value;
	}

	/** 获取类型。 / Returns the type. */
	public SpawnType getType() {
		return type;
	}

	/** 设置类型。 / Sets the type. */
	public void setType(SpawnType value) {
		type = value;
	}

	/** 返回 entity id / Returns the entity id */
	public int getEntityId() {
		return entityId;
	}

	/** 设置 entity id / Sets the entity id */
	public void setEntityId(int entityId) {
		this.entityId = entityId;
	}
}
