package com.aionemu.gameserver.model.templates.spawns;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 房屋刷新点模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HouseSpawn")
public class HouseSpawn {
	/** 返回 x 坐标 / Returns the x */
	@Getter
	@Setter
	@XmlAttribute(name = "x", required = true)
	protected float x;

	/** 返回 y 坐标 / Returns the y */
	@Getter
	@Setter
	@XmlAttribute(name = "y", required = true)
	protected float y;

	/** 返回 z 坐标 / Returns the z */
	@Getter
	@Setter
	@XmlAttribute(name = "z", required = true)
	protected float z;

	/** 设置朝向 / Sets the heading */
	@Setter
	@XmlAttribute(name = "h")
	protected Byte h;

	/** 返回实体 ID / Returns the entity id */
	@Getter
	@Setter
	@XmlAttribute(name = "entity_id")
	private int entityId;

	/** 获取类型。 / Returns the type. */
	@Getter
	@Setter
	@XmlAttribute(name = "type", required = true)
	protected SpawnType type;

	/** 返回朝向 / Returns the heading */
	public byte getH() {
		if (h == null) {
			return 0;
		}
		return h.byteValue();
	}
}
