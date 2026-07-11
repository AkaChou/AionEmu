package com.aionemu.gameserver.controllers.movement;

import com.aionemu.gameserver.model.gameobjects.Pet;

/**
 * 宠物移动控制器，当前为占位实现。
 * Pet move controller; currently a placeholder implementation.
 *
 * @author ATracer
 */
public class PetMoveController extends CreatureMoveController<Pet> {

	/** 目标 X / Target X */
	protected float targetDestX;
	/** 目标 Y / Target Y */
	protected float targetDestY;
	/** 目标 Z / Target Z */
	protected float targetDestZ;
	/** 朝向 / Heading */
	protected byte heading;
	/** 移动掩码 / Movement mask */
	protected byte movementMask;

	/**
	 * 构造占位控制器（尚未绑定所有者）。
	 * Construct a placeholder controller (owner not bound yet).
	 */
	public PetMoveController() {
		super(null);// not used yet
	}

	/**
	 * 向目标点推进（占位空实现）。
	 * Advance toward the destination (placeholder no-op).
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
	 * 设置新方向（朝向默认为 0）。
	 * Set a new destination (heading defaults to 0).
	 *
	 * @param x2 目标 X / Target X
	 * @param y2 目标 Y / Target Y
	 * @param z2 目标 Z / Target Z
	 */
	@Override
	public void setNewDirection(float x2, float y2, float z2) {
		setNewDirection(x2, y2, z2, (byte) 0);
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
		this.targetDestX = x;
		this.targetDestY = y;
		this.targetDestZ = z;
		this.heading = heading;
	}

	/**
	 * 开始向目标点移动（占位空实现）。
	 * Start moving toward the destination (placeholder no-op).
	 */
	@Override
	public void startMovingToDestination() {
	}

	/**
	 * 中止当前移动（占位空实现）。
	 * Abort the current movement (placeholder no-op).
	 */
	@Override
	public void abortMove() {
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
	 * 是否处于移动中（固定返回 true）。
	 * Whether currently in move (always true).
	 *
	 * Always true
	 */
	@Override
	public boolean isInMove() {
		return true;
	}

	/**
	 * 设置是否处于移动中（占位空实现）。
	 * Set whether currently in move (placeholder no-op).
	 *
	 * @param value 移动状态 / In-move flag
	 */
	@Override
	public void setInMove(boolean value) {
	}

	/**
	 * 技能施放时设置为立即移动掩码。
	 * Set the immediate movement mask during skill cast.
	 */
	@Override
	public void skillMovement() {
		this.movementMask = MovementMask.IMMEDIATE;

	}
}
