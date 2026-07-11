package com.aionemu.gameserver.movement.processors.movement.motor;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.movement.processors.movement.MovementProcessor;

/**
 * 移动电机抽象基类：持有 NPC 与处理器引用，并维护目标点、朝向与移动掩码。
 * Abstract movement-motor base: holds NPC and processor references and tracks target, heading and movement mask.
 */
public abstract class AMovementMotor {

	/**
	 * 电机所属 NPC。
	 * Owner NPC of this motor.
	 */
	final Npc _owner;

	/**
	 * 所属移动处理器。
	 * Parent movement processor.
	 */
	final MovementProcessor _processor;

	/**
	 * 当前移动目标点。
	 * Current movement target position.
	 */
	Vector3f _targetPosition;

	/**
	 * 目标目的地 X。
	 * Target destination X.
	 */
	protected float targetDestX;

	/**
	 * 目标目的地 Y。
	 * Target destination Y.
	 */
	protected float targetDestY;

	/**
	 * 目标目的地 Z。
	 * Target destination Z.
	 */
	protected float targetDestZ;

	/**
	 * 客户端移动状态掩码。
	 * Client movement-state mask.
	 */
	byte _targetMask;

	/**
	 * 目标朝向。
	 * Target heading.
	 */
	byte _targetHeading;

	/**
	 * 绑定所属 NPC 与移动处理器。
	 * Bind the owner NPC and movement processor.
	 *
	 * Owner NPC
	 * @param processor 移动处理器 / Movement processor
	 */
	AMovementMotor(Npc owner, MovementProcessor processor) {
		this._owner = owner;
		this._processor = processor;
	}

	/**
	 * 启动电机。
	 * Start the motor.
	 */
	public abstract void start();

	/**
	 * 停止电机。
	 * Stop the motor.
	 */
	public abstract void stop();

	/**
	 * 返回当前目标位置。
	 * Return the current target position.
	 *
	 * Target position
	 */
	public Vector3f getCurrentTarget() {
		return this._targetPosition;
	}

	/**
	 * 返回当前移动掩码。
	 * Return the current movement mask.
	 *
	 * Movement mask
	 */
	public byte getMovementMask() {
		return this._targetMask;
	}

	/**
	 * 根据朝向变化、移动速度与生物状态重算移动掩码。
	 * Recalculate the movement mask from heading change, speed and creature state.
	 */
	void recalculateMovementParams() {
		byte oldHeading = this._owner.getHeading();
		byte _targetHeading = (byte) (Math.toDegrees(Math.atan2(this._targetPosition.getY() - this._owner.getY(),
				this._targetPosition.getX() - this._owner.getX())) / 3.0);
		this._targetMask = 0;
		if (oldHeading != _targetHeading) {
			this._targetMask = (byte) (this._targetMask | 0xFFFFFFE0);
		}
		Stat2 stat = this._owner.getGameStats().getMovementSpeed();
		if (this._owner.isInState(CreatureState.WEAPON_EQUIPPED)) {
			this._targetMask = (byte) (this._targetMask | (stat.getBonus() < 0 ? -30 : -28));
		} else if (this._owner.isInState(CreatureState.WALKING) || this._owner.isInState(CreatureState.ACTIVE)) {
			this._targetMask = (byte) (this._targetMask | (stat.getBonus() < 0 ? -24 : -22));
		}
		if (this._owner.isFlying()) {
			this._targetMask = (byte) (this._targetMask | 4);
		}
		if (this._owner.getAi2().getState() == AIState.RETURNING) {
			this._targetMask = (byte) (this._targetMask | 0xFFFFFFE2);
		}
	}
}
