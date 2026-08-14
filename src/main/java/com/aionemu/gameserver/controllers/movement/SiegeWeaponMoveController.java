package com.aionemu.gameserver.controllers.movement;

import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;

import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 攻城兵器召唤物移动控制器，持续追踪目标位置并插值推进。
 * Siege-weapon summon move controller that tracks the target and interpolates movement.
 */
public class SiegeWeaponMoveController extends SummonMoveController {

	/** Tracked point X / Tracked point X */
	private float pointX;
	/** Tracked point Y / Tracked point Y */
	private float pointY;
	/** Tracked point Z / Tracked point Z */
	private float pointZ;
	/** 停止偏移 / Stop offset */
	private float offset = 0.1f;
	/** 移动检测偏移阈值 / Move check offset threshold */
	public static final float MOVE_CHECK_OFFSET = 0.1f;

	/**
	 * 使用指定召唤物构造控制器。
	 * Construct the controller for the given summon.
	 *
	 * @param owner 召唤物 / Summon owner
	 */
	public SiegeWeaponMoveController(Summon owner) {
		super(owner);
	}

	/**
	 * 向当前目标持续推进；施法/不可移动时停止。
	 * Keep advancing toward the current target; stop when casting or unable to move.
	 */
	@Override
	public void moveToDestination() {
		if (!owner.canPerformMove() || (owner.getAi2().getSubState() == AISubState.CAST)) {
			if (started.compareAndSet(true, false)) {
				setAndSendStopMove(owner);
			}
			updateLastMove();
			return;
		} else if (started.compareAndSet(false, true)) {
			movementMask = -32;
			PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner));
		}

		if (MathUtil.getDistance(owner.getTarget(), pointX, pointY, pointZ) > MOVE_CHECK_OFFSET) {
			pointX = owner.getTarget().getX();
			pointY = owner.getTarget().getY();
			pointZ = owner.getTarget().getZ();
		}
		moveToLocation(pointX, pointY, pointZ, offset);
		updateLastMove();
	}

	/**
	 * 开始向目标对象移动并注册移动任务。
	 * Start moving toward the target object and register the move task.
	 */
	@Override
	public void moveToTargetObject() {
		updateLastMove();
		GameMovementLoopServices.moveTaskManager().addCreature(owner);
	}

	/**
	 * 按速度插值向指定坐标移动，方向变化时广播移动包。
	 * Interpolate toward the given coordinates by speed; broadcast when direction changes.
	 *
	 * @param targetX 目标 X 坐标 / Target X
	 * @param targetY 目标 Y 坐标 / Target Y
	 * @param targetZ 目标 Z 坐标 / Target Z
	 * @param offset 停止偏移 / Stop offset
	 */
	protected void moveToLocation(float targetX, float targetY, float targetZ, float offset) {
		boolean directionChanged;
		float ownerX = owner.getX();
		float ownerY = owner.getY();
		float ownerZ = owner.getZ();

		directionChanged = targetX != targetDestX || targetY != targetDestY || targetZ != targetDestZ;

		if (directionChanged) {
			heading = (byte) (Math.toDegrees(Math.atan2(targetY - ownerY, targetX - ownerX)) / 3);
		}

		targetDestX = targetX;
		targetDestY = targetY;
		targetDestZ = targetZ;

		float currentSpeed = owner.getGameStats().getMovementSpeedFloat();
		float futureDistPassed = currentSpeed * (System.currentTimeMillis() - lastMoveUpdate) / 1000f;

		float dist = (float) MathUtil.getDistance(ownerX, ownerY, ownerZ, targetX, targetY, targetZ);

		if (dist == 0) {
			return;
		}

		if (futureDistPassed > dist) {
			futureDistPassed = dist;
		}

		float distFraction = futureDistPassed / dist;
		float newX = (targetDestX - ownerX) * distFraction + ownerX;
		float newY = (targetDestY - ownerY) * distFraction + ownerY;
		float newZ = (targetDestZ - ownerZ) * distFraction + ownerZ;
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(owner, newX, newY, newZ, heading, false);
		if (directionChanged) {
			movementMask = -32;
			PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner));
		}
	}
}
