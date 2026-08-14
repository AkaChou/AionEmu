package com.aionemu.gameserver.controllers.movement;

/**
 * 生物移动控制器接口，定义移动目标、方向、启停与掩码的统一契约。
 * Creature movement controller contract for destination, direction, start/stop, and mask.
 *
 * @author ATracer
 */
public interface MoveController {

	/**
	 * 向当前目标点推进一帧移动。
	 * Advance one movement step toward the current destination.
	 */
	void moveToDestination();

	/**
	 * 返回目标 X 坐标。
	 * Return the target X coordinate.
	 *
	 * @return 目标 X 坐标 / Target X
	 */
	float getTargetX2();

	/**
	 * 返回目标 Y 坐标。
	 * Return the target Y coordinate.
	 *
	 * @return 目标 Y 坐标 / Target Y
	 */
	float getTargetY2();

	/**
	 * 返回目标 Z 坐标。
	 * Return the target Z coordinate.
	 *
	 * @return 目标 Z 坐标 / Target Z
	 */
	float getTargetZ2();

	/**
	 * 设置新的移动方向与朝向。
	 * Set a new movement destination and heading.
	 *
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 * 朝向 / Heading
	 */
	void setNewDirection(float x, float y, float z, byte heading);

	/**
	 * 开始向目标点移动。
	 * Start moving toward the destination.
	 */
	void startMovingToDestination();

	/**
	 * 中止当前移动。
	 * Abort the current movement.
	 */
	void abortMove();

	/**
	 * 返回当前移动掩码。
	 * Return the current movement mask.
	 *
	 * @return 移动掩码 / Movement mask
	 */
	byte getMovementMask();

	/**
	 * 是否处于移动中。
	 * Whether the owner is currently moving.
	 *
	 * @return 是否移动中 / Whether in move
	 */
	boolean isInMove();

	/**
	 * 设置是否处于移动中。
	 * Set whether the owner is currently moving.
	 *
	 * @param value 移动状态 / In-move flag
	 */
	void setInMove(boolean value);

	/**
	 * 技能施放时的即时移动处理。
	 * Immediate movement handling during skill cast.
	 */
	void skillMovement();
}
