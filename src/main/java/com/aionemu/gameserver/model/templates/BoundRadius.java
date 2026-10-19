package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 边界半径模板（静态数据/XML）。
 * Bound radius template (static data / XML).
 * <p>本地存在两套约定：玩家/坐骑/机甲与旧 NPC 模板的 front/side 是**半径（半值）**；
 * 真端投影的 NPC 模板 front/side 是**全长**，碰撞半径为 {@code max(front, side) * 0.5}。
 * The local data has two conventions: player/ride/robot and legacy NPC templates store
 * front/side as **radii (half lengths)**; retail-projected NPC templates store **full lengths**
 * and derive the collision radius as {@code max(front, side) * 0.5}.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoundRadius")
@NoArgsConstructor
public class BoundRadius {
	/** 返回 front / Returns the front */
	@XmlAttribute
	private float front;

	/** 返回 side / Returns the side */
	@XmlAttribute
	private float side;

	/** 返回 upper / Returns the upper */
	@XmlAttribute
	private float upper;

	/** 返回碰撞 / Returns the collision */
	private float collision;

	/** front/side 是否为全长约定 / whether front/side use the full-length convention */
	@XmlTransient
	private boolean fullLength;

	public static final BoundRadius DEFAULT = new BoundRadius(0f, 0f, 0f);

	public BoundRadius(float front, float side, float upper) {
		this(front, side, upper, false);
	}

	public BoundRadius(float front, float side, float upper, boolean fullLength) {
		this.front = front;
		this.side = side;
		this.upper = upper;
		this.fullLength = fullLength;
		calculateCollision(front, side);
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		calculateCollision(front, side);
	}

	protected void calculateCollision(float front, float side) {
		this.collision = fullLength ? Math.max(front, side) * 0.5f : (float) Math.sqrt(side * front);
	}

	/**
	 * 返回 front 方向的半径：全长约定取一半，半径约定原样返回。
	 * Returns the front radius: half for the full-length convention, unchanged for the radius convention.
	 *
	 * @return front 方向半径 / front radius
	 */
	public float getHalfFront() {
		return fullLength ? front * 0.5f : front;
	}

	/**
	 * 返回 side 方向的半径：全长约定取一半，半径约定原样返回。
	 * Returns the side radius: half for the full-length convention, unchanged for the radius convention.
	 *
	 * @return side 方向半径 / side radius
	 */
	public float getHalfSide() {
		return fullLength ? side * 0.5f : side;
	}

	/**
	 * 返回水平方向的最大半径：全长约定取 max*0.5，半径约定取 max。
	 * Returns the largest horizontal radius: max*0.5 for full lengths, max for radii.
	 *
	 * @return 水平最大半径 / largest horizontal radius
	 */
	public float getHalfMaxHorizontal() {
		return fullLength ? Math.max(front, side) * 0.5f : Math.max(front, side);
	}
}
