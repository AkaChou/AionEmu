package com.aionemu.gameserver.controllers.movement;

import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.StatFunctions;
import com.aionemu.gameserver.world.World;

/**
 * 可玩单位（玩家/召唤物）移动控制器基类，处理强制移动包、速度插值与恐惧控制。
 * Base move controller for playable units (player/summon): forced move packets, speed interpolation, and fear control.
 *
 * @author ATracer
 * @param <T> 生物所有者类型 / Creature owner type
 */
public abstract class PlayableMoveController<T extends Creature> extends CreatureMoveController<T> {

	/** 是否需要发送移动包 / Whether a move packet should be sent */
	private boolean sendMovePacket = true;
	/** 移动朝向档位 / Movement heading sector */
	private int movementHeading = -1;

	/** Vehicle X / Vehicle X */
	public float vehicleX;
	/** Vehicle Y / Vehicle Y */
	public float vehicleY;
	/** Vehicle Z / Vehicle Z */
	public float vehicleZ;
	/** 载具速度 / Vehicle speed */
	public int vehicleSpeed;

	/** Direction vector X / Direction vector X */
	public float vectorX;
	/** Direction vector Y / Direction vector Y */
	public float vectorY;
	/** Direction vector Z / Direction vector Z */
	public float vectorZ;
	/** 滑翔标志 / Glide flag */
	public byte glideFlag;
	/** 未知字段 1 / Unknown field 1 */
	public int unk1;
	/** 未知字段 2 / Unknown field 2 */
	public int unk2;

	/**
	 * 使用指定所有者构造控制器。
	 * Construct the controller for the given owner.
	 *
	 * @param owner 所有者 / Owner
	 */
	public PlayableMoveController(T owner) {
		super(owner);
	}

	/**
	 * 在可控状态下启动向目标点的移动并注册任务。
	 * Start moving to the destination when controllable and register the move task.
	 */
	@Override
	public void startMovingToDestination() {
		updateLastMove();
		if (owner.canPerformMove()) {
			if (isControlled() && started.compareAndSet(false, true)) {
				this.movementMask = MovementMask.NPC_STARTMOVE;
				sendForcedMovePacket();
				GameMovementLoopServices.playerMoveTaskManager().addPlayer(owner);
			}
		}
	}

	/**
	 * 是否处于恐惧等强制控制状态。
	 * Whether under forced control such as fear.
	 *
	 * @return 是否被控制 / Whether controlled
	 */
	private final boolean isControlled() {
		return owner.getEffectController().isUnderFear() || owner.getEffectController().isConfused();
	}

	/**
	 * 强制广播移动包。
	 * Force-broadcast a move packet.
	 */
	private void sendForcedMovePacket() {
		PacketSendUtility.broadcastPacketAndReceive(owner, new SM_MOVE(owner));
		sendMovePacket = false;
	}

	/**
	 * 按当前速度向目标点插值推进一帧。
	 * Advance one interpolated step toward the destination using current speed.
	 */
	@Override
	public void moveToDestination() {
		if (!owner.canPerformMove()) {
			if (started.compareAndSet(true, false)) {
				setAndSendStopMove(owner);
			}
			updateLastMove();
			return;
		}

		if (sendMovePacket && isControlled()) {
			sendForcedMovePacket();
		}

		float x = owner.getX();
		float y = owner.getY();
		float z = owner.getZ();

		float currentSpeed = StatFunctions.getMovementModifier(owner, StatEnum.SPEED,
				owner.getGameStats().getMovementSpeedFloat());
		float futureDistPassed = currentSpeed * (System.currentTimeMillis() - lastMoveUpdate) / 1000f;
		float dist = (float) MathUtil.getDistance(x, y, z, targetDestX, targetDestY, targetDestZ);

		if (dist == 0) {
			return;
		}

		if (futureDistPassed > dist) {
			futureDistPassed = dist;
		}

		float distFraction = futureDistPassed / dist;
		float newX = (targetDestX - x) * distFraction + x;
		float newY = (targetDestY - y) * distFraction + y;
		float newZ = (targetDestZ - z) * distFraction + z;

		/*
		 * if ((movementMask & MovementMask.MOUSE) == 0) { targetDestX = newX + vectorX;
		 * targetDestY = newY + vectorY; targetDestZ = newZ + vectorZ; }
		 */

		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(owner, newX, newY, newZ, heading, false);
		updateLastMove();
	}

	/**
	 * 中止移动：移除任务、清目标并广播停止。
	 * Abort movement: remove task, clear destination, and broadcast stop.
	 */
	@Override
	public void abortMove() {
		started.set(false);
		GameMovementLoopServices.playerMoveTaskManager().removePlayer(owner);
		targetDestX = 0;
		targetDestY = 0;
		targetDestZ = 0;
		setAndSendStopMove(owner);
	}

	/**
	 * 设置新目标点并在变化时标记需要发包；同步计算移动朝向档位。
	 * Set a new destination, mark packet send when changed, and recompute movement heading sector.
	 *
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 */
	@Override
	public void setNewDirection(float x, float y, float z) {
		if (targetDestX != x || targetDestY != y || targetDestZ != z) {
			sendMovePacket = true;
		}
		this.targetDestX = x;
		this.targetDestY = y;
		this.targetDestZ = z;

		float h = MathUtil.calculateAngleFrom(owner.getX(), owner.getY(), targetDestX, targetDestY);
		if (h != 0) {
			int value = (int) (((heading * 3) - h) / 45);
			if (value < 0) {
				value += 8;
			}
			if (movementHeading != value) {
				movementHeading = value;
			}
		}
	}

	/**
	 * 返回当前移动朝向档位；未移动时返回 -1。
	 * Return the current movement heading sector; -1 when not moving.
	 *
	 * @return 朝向档位 / Heading sector
	 */
	public int getMovementHeading() {
		if (!isInMove()) {
			return -1;
		}
		return movementHeading;
	}
}
