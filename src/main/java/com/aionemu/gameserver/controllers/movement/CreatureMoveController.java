package com.aionemu.gameserver.controllers.movement;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 生物移动控制器抽象基类，维护目标点、朝向、移动掩码与启停状态。
 * Abstract base move controller for creatures: destination, heading, mask, and start/stop state.
 *
 * @author ATracer
 * @param <T> 所有者可见对象类型 / Owner visible-object type
 */
public abstract class CreatureMoveController<T extends VisibleObject> implements MoveController {

	/** 所有者 / Owner */
	protected T owner;
	/** 朝向 / Heading */
	protected byte heading;
	/** 上次移动更新时间戳 / Last move update timestamp */
	protected long lastMoveUpdate = System.currentTimeMillis();
	/** 是否处于移动中 / Whether currently in move */
	protected boolean isInMove = false;
	/** 移动是否已启动 / Whether movement has started */
	protected transient AtomicBoolean started = new AtomicBoolean(false);

	/** 移动掩码 / Movement mask */
	public byte movementMask;
	/** 目标 X / Target X */
	protected float targetDestX;
	/** 目标 Y / Target Y */
	protected float targetDestY;
	/** 目标 Z / Target Z */
	protected float targetDestZ;

	/**
	 * 使用指定所有者构造控制器。
	 * Construct the controller for the given owner.
	 *
	 * Owner
	 */
	public CreatureMoveController(T owner) {
		this.owner = owner;
	}

	/**
	 * 向目标点推进一帧（基类空实现）。
	 * Advance one step toward the destination (no-op base).
	 */
	@Override
	public void moveToDestination() {
	}

	/**
	 * 返回目标 X 坐标。
	 * Return the target X coordinate.
	 *
	 * Target X
	 */
	@Override
	public float getTargetX2() {
		return targetDestX;
	}

	/**
	 * 返回目标 Y 坐标。
	 * Return the target Y coordinate.
	 *
	 * Target Y
	 */
	@Override
	public float getTargetY2() {
		return targetDestY;
	}

	/**
	 * 返回目标 Z 坐标。
	 * Return the target Z coordinate.
	 *
	 * Target Z
	 */
	@Override
	public float getTargetZ2() {
		return targetDestZ;
	}

	/**
	 * 设置新方向与朝向。
	 * Set a new destination and heading.
	 *
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 * 朝向 / Heading
	 */
	@Override
	public void setNewDirection(float x, float y, float z, byte heading) {
		this.heading = heading;
		setNewDirection(x, y, z);
	}

	/**
	 * 仅更新目标坐标。
	 * Update destination coordinates only.
	 *
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 */
	protected void setNewDirection(float x, float y, float z) {
		this.targetDestX = x;
		this.targetDestY = y;
		this.targetDestZ = z;
	}

	/**
	 * 开始向目标点移动（基类空实现）。
	 * Start moving toward the destination (no-op base).
	 */
	@Override
	public void startMovingToDestination() {
	}

	/**
	 * 中止当前移动（基类空实现）。
	 * Abort the current movement (no-op base).
	 */
	@Override
	public void abortMove() {
	}

	/**
	 * 设置停止掩码并向周围广播停止移动包。
	 * Set the stop mask and broadcast a stop-move packet.
	 *
	 * @param owner 生物所有者 / Creature owner
	 */
	protected void setAndSendStopMove(Creature owner) {
		movementMask = MovementMask.IMMEDIATE;
		PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner));
	}

	/**
	 * 刷新上次移动更新时间。
	 * Refresh the last move update timestamp.
	 */
	public final void updateLastMove() {
		lastMoveUpdate = System.currentTimeMillis();
	}

	/**
	 * 返回上次移动更新时间戳。
	 * Return the last move update timestamp.
	 *
	 * Timestamp
	 */
	public long getLastMoveUpdate() {
		return lastMoveUpdate;
	}

	/**
	 * 返回当前移动掩码。
	 * Return the current movement mask.
	 *
	 * Movement mask
	 */
	@Override
	public byte getMovementMask() {
		return movementMask;
	}

	/**
	 * 是否处于移动中。
	 * Whether the owner is currently moving.
	 *
	 * @return 是否移动中 / Whether in move
	 */
	@Override
	public boolean isInMove() {
		return isInMove;
	}

	/**
	 * 设置是否处于移动中。
	 * Set whether the owner is currently moving.
	 *
	 * @param value 移动状态 / In-move flag
	 */
	@Override
	public void setInMove(boolean value) {
		isInMove = value;
	}
}
