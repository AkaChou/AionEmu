package com.aionemu.gameserver.model.templates.ride;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.RideBound;

/**
 * Ride 信息模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RideInfo", propOrder = { "ridebound" })
public class RideInfo {
	protected RideBound ridebound;

	@XmlAttribute(name = "cost_fp")
	protected Integer costFp;

	@XmlAttribute(name = "start_fp")
	protected int startFp;

	@XmlAttribute(name = "sprint_speed")
	protected float sprintSpeed;

	@XmlAttribute(name = "fly_speed")
	protected float flySpeed;

	@XmlAttribute(name = "move_speed")
	protected float moveSpeed;

	@XmlAttribute
	protected Integer type;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回 ride bound / Returns the ride bound */
	public RideBound getRideBound() {
		return ridebound;
	}

	/** 返回 cost fp / Returns the cost fp */
	public Integer getCostFp() {
		return costFp;
	}

	/** 返回开始飞行点 / Returns the start fp*/
	public int getStartFp() {
		return startFp;
	}

	/** 返回 sprint speed / Returns the sprint speed */
	public float getSprintSpeed() {
		return sprintSpeed;
	}

	/** 返回 fly speed / Returns the fly speed */
	public float getFlySpeed() {
		return flySpeed;
	}

	/** 返回 move speed / Returns the move speed */
	public float getMoveSpeed() {
		return moveSpeed;
	}

	/** 获取类型。 / Returns the type. */
	public Integer getType() {
		return type;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return id;
	}

	/**
	 * @return Whether sprint
	 */
	public boolean canSprint() {
		return sprintSpeed != 0.0f;
	}
}
