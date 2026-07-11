package com.aionemu.gameserver.movement.processors.movement.motor;

import java.util.concurrent.ScheduledFuture;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.movement.processors.movement.MovementProcessor;

/**
 * 回归电机：将 NPC 在一段时间内移回指定地点，并触发到家相关 AI 事件。
 * Return motor: moves an NPC back to a spot over a duration and fires home-related AI events.
 */
public class ReturnMotor extends AMovementMotor {

	/**
	 * 到达目标点的定时任务句柄。
	 * Scheduled task handle for arrival at the target.
	 */
	private ScheduledFuture<?> _task;

	/**
	 * 创建指向指定回归点的电机。
	 * Create a motor targeting the given return spot.
	 *
	 * Owner NPC
	 * @param spot 回归目标点 / Return destination
	 * @param processor 移动处理器 / Movement processor
	 */
	public ReturnMotor(Npc owner, Vector3f spot, MovementProcessor processor) {
		super(owner, processor);
		this._targetPosition = spot;
	}

	/**
	 * 广播移动包并在预计到达时间更新坐标、触发 AI 事件。
	 * Broadcast the move packet and, at the estimated arrival time, update position and fire AI events.
	 */
	@Override
	public void start() {
		assert (this._task == null);
		this.recalculateMovementParams();
		float speed = this._owner.getGameStats().getMovementSpeedFloat();
		long movementTime = (long) (100.0f / speed * 1000.0f);
		PacketSendUtility.broadcastPacket(this._owner,
				new SM_MOVE(this._owner.getObjectId(), this._owner.getX(), this._owner.getY(), this._owner.getZ(),
						this._targetPosition.x, this._targetPosition.y, this._targetPosition.z, this._targetHeading,
						this._targetMask));
		this._task = this._processor.schedule(new Runnable() {

			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(ReturnMotor.this._owner, ReturnMotor.this._targetPosition.x,
						ReturnMotor.this._targetPosition.y, ReturnMotor.this._targetPosition.z,
						ReturnMotor.this._targetHeading, false);
				ReturnMotor.this._owner.getAi2().onGeneralEvent(AIEventType.MOVE_ARRIVED);
				ReturnMotor.this._owner.getAi2().onGeneralEvent(AIEventType.BACK_HOME);
			}
		}, movementTime);
	}

	/**
	 * 停止电机（当前无额外清理逻辑）。
	 * Stop the motor (no additional cleanup currently).
	 */
	@Override
	public void stop() {
	}
}
