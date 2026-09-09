package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 边界半径模板（静态数据/XML）。
 * Bound radius template (static data / XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoundRadius")
@NoArgsConstructor
public class BoundRadius {
	/** 返回 front / Returns the front */
	@Getter
	@XmlAttribute
	private float front;

	/** 返回 side / Returns the side */
	@Getter
	@XmlAttribute
	private float side;

	/** 返回 upper / Returns the upper */
	@Getter
	@XmlAttribute
	private float upper;

	/** 返回碰撞 / Returns the collision */
	@Getter
	private float collision;

	public static final BoundRadius DEFAULT = new BoundRadius(0f, 0f, 0f);

	public BoundRadius(float front, float side, float upper) {
		this.front = front;
		this.side = side;
		this.upper = upper;
		calculateCollision(front, side);
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		calculateCollision(front, side);
	}

	protected void calculateCollision(float front, float side) {
		this.collision = (float) Math.sqrt(side * front);
	}
}
