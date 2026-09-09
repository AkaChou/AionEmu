package com.aionemu.gameserver.model.templates.ride;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.RideBound;
import lombok.Getter;

/**
 * Ride 信息模板（静态数据/XML）。
 * Ride info template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RideInfo", propOrder = { "ridebound" })
public class RideInfo {
	protected RideBound ridebound;

	/** 返回 cost fp / Returns the cost fp */
	@Getter
	@XmlAttribute(name = "cost_fp")
	protected Integer costFp;

	/** 返回开始飞行点 / Returns the start fp*/
	@Getter
	@XmlAttribute(name = "start_fp")
	protected int startFp;

	/** 返回 sprint speed / Returns the sprint speed */
	@Getter
	@XmlAttribute(name = "sprint_speed")
	protected float sprintSpeed;

	/** 返回 fly speed / Returns the fly speed */
	@Getter
	@XmlAttribute(name = "fly_speed")
	protected float flySpeed;

	/** 返回 move speed / Returns the move speed */
	@Getter
	@XmlAttribute(name = "move_speed")
	protected float moveSpeed;

	/** 获取类型。 / Returns the type. */
	@Getter
	@XmlAttribute
	protected Integer type;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回 ride bound / Returns the ride bound */
	public RideBound getRideBound() {
		return ridebound;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return id;
	}

	/**
	 * @return 是否可冲刺 / Whether sprint
	 */
	public boolean canSprint() {
		return sprintSpeed != 0.0f;
	}
}
