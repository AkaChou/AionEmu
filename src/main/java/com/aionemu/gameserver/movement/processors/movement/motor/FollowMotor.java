package com.aionemu.gameserver.movement.processors.movement.motor;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import java.util.concurrent.ScheduledFuture;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.movement.processors.movement.MovementProcessor;
import com.aionemu.gameserver.movement.processors.movement.PathfindHelper;
import com.aionemu.gameserver.movement.utils.GeomUtil;

/**
 * 跟随电机：周期重算目标点，按速度插值更新 NPC 位置并广播移动包。
 * Follow motor: periodically revalidates the target, interpolates NPC position by speed, and broadcasts move packets.
 */
public class FollowMotor extends AMovementMotor {

	/**
	 * 目标重校验间隔（毫秒）。
	 * Target revalidation interval in milliseconds.
	 */
	private static final int TARGET_REVALIDATE_TIME = 300;

	/**
	 * 下一次允许寻路重算的时间戳。
	 * Timestamp after which pathfinding may be revalidated.
	 */
	private static long pathfindRevalidationTime;

	/**
	 * 当前跟随目标。
	 * Current follow target.
	 */
	public VisibleObject _target;

	/**
	 * 周期更新任务句柄。
	 * Periodic update task handle.
	 */
	private ScheduledFuture<?> _task;

	/**
	 * 上次移动时刻（毫秒）。
	 * Last move timestamp in milliseconds.
	 */
	private long _lastMoveMs;

	/**
	 * 上次移动起点。
	 * Last move origin point.
	 */
	private Vector3f _lastMovePoint;

	/**
	 * 新计算出的目标朝向。
	 * Newly computed target heading.
	 */
	private byte new_targetHeading;

	/**
	 * 创建跟随指定目标的电机。
	 * Create a motor that follows the given target.
	 *
	 * @param parentProcessor 父移动处理器 / Parent movement processor
	 * @param owner 所属 NPC / owner NPC
	 * @param target 跟随目标 / follow target
	 */
	public FollowMotor(MovementProcessor parentProcessor, Npc owner, VisibleObject target) {
		super(owner, parentProcessor);
		this._target = target;
	}

	/**
	 * 启动跟随：断言任务未创建并立即执行一次更新。
	 * Start following: assert no task exists and run an immediate update.
	 */
	@Override
	public void start() {
		assert (this._task == null);
		this.update();
	}

	/**
	 * 停止跟随：取消任务并清空目标与移动状态。
	 * Stop following: cancel the task and clear target and movement state.
	 */
	@Override
	public void stop() {
		if (this._task != null) {
			this._task.cancel(true);
		}
		pathfindRevalidationTime = 0L;
		this._lastMoveMs = 0L;
		this._lastMovePoint = null;
		this._target = null;
	}

	/**
	 * 重算跟随目标点、广播移动并调度下一次位置插值。
	 * Recompute the follow target, broadcast movement, and schedule the next position interpolation.
	 *
	 * @return 仍可继续跟随为 true / {@code true} if following can continue
	 */
	public boolean update() {
		VisibleObject target = this._target;
		if (target == null || this._task != null && this._task.isCancelled()
				|| this._owner.getLifeStats().isAlreadyDead() || this._owner.getAi2().getState() == AIState.DIED) {
			return false;
		}
		boolean directionChanged = false;
		this._lastMovePoint = new Vector3f(this._owner.getX(), this._owner.getY(), this._owner.getZ());
		boolean canPass = GameWorldServices.geoService().canPass(this._owner, target);
		if (this.canMove() && !canPass && pathfindRevalidationTime < System.currentTimeMillis()) {
			this._targetPosition = PathfindHelper.selectFollowStep(this._owner, target);
		} else if (this.canMove() && canPass) {
			float newZ = GameWorldServices.geoService().getZ(this._owner.getWorldId(), target.getX(), target.getY(),
					target.getZ(), 100.0f, this._owner.getInstanceId());
			Vector3f getTargetPos = new Vector3f(target.getX(), target.getY(), newZ);
			float range = (float) this._owner.getGameStats().getAttackRange().getCurrent() / 1000.0f;

			if (this._lastMovePoint == null) {
				this._lastMovePoint = new Vector3f(this._owner.getX(), this._owner.getY(), this._owner.getZ());
			}
			double distance = GeomUtil.getDistance3D(this._lastMovePoint, getTargetPos.x, getTargetPos.y,
					getTargetPos.z) - Math.max(range, this._owner.getCollision());
			Vector3f dir = GeomUtil.getDirection3D(this._lastMovePoint, getTargetPos);
			this._targetPosition = GeomUtil.getNextPoint3D(this._lastMovePoint, dir, (float) distance);
		} else if (pathfindRevalidationTime < System.currentTimeMillis()) {
			this._targetPosition = null;
		}
		if (this._targetPosition != null) {
			directionChanged = this._targetPosition.x != this.targetDestX || this._targetPosition.y != this.targetDestY
					|| this._targetPosition.z != this.targetDestZ;
			this.targetDestX = this._targetPosition.x;
			this.targetDestY = this._targetPosition.y;
			this.targetDestZ = this._targetPosition.z;
			double distance = GeomUtil.getDistance3D(this._owner.getX(), this._owner.getY(), this._owner.getZ(),
					this._targetPosition.x, this._targetPosition.y, this._targetPosition.z);
			float speed = this._owner.getGameStats().getMovementSpeedFloat();
			long movementTime = (long) (distance / (double) speed * 1000.0);
			pathfindRevalidationTime = System.currentTimeMillis() + movementTime;
			this.recalculateMovementParams();
			this.new_targetHeading = (byte) (Math.toDegrees(Math.atan2(this._targetPosition.y - this._owner.getY(),
					this._targetPosition.x - this._owner.getX())) / 3.0);
			if (directionChanged) {
				PacketSendUtility.broadcastPacket(this._owner,
						new SM_MOVE(this._owner.getObjectId(), this._owner.getX(), this._owner.getY(),
								this._owner.getZ(), this._targetPosition.x, this._targetPosition.y,
								this._targetPosition.z, this.new_targetHeading, this._targetMask));
			}
			this._lastMoveMs = System.currentTimeMillis();
		}
		this._task = this._processor.schedule(new Runnable() {

			@Override
			public void run() {
				if (FollowMotor.this._targetPosition != null) {
					Vector3f lastMove = FollowMotor.this._lastMovePoint;
					Vector3f targetMove = new Vector3f(FollowMotor.this._targetPosition.x,
							FollowMotor.this._targetPosition.y, FollowMotor.this._targetPosition.z);
					float speed = FollowMotor.this._owner.getGameStats().getMovementSpeedFloat();
					long time = System.currentTimeMillis() - FollowMotor.this._lastMoveMs;
					float distPassed = speed * ((float) time / 1000.0f);
					if (lastMove == null) {
						lastMove = new Vector3f(FollowMotor.this._owner.getX(), FollowMotor.this._owner.getY(),
								FollowMotor.this._owner.getZ());
					}
					float maxDist = lastMove.distance(targetMove);
					if (distPassed <= 0.0f) {
						return;
					}
					if (distPassed > maxDist) {
						distPassed = maxDist;
					}
					Vector3f dir = GeomUtil.getDirection3D(lastMove, targetMove);
					Vector3f position = GeomUtil.getNextPoint3D(lastMove, dir, distPassed);
					if (FollowMotor.this._owner.getWorldId() != 300230000) {
						float newZ = GameWorldServices.geoService().getZ(FollowMotor.this._owner.getWorldId(), position.x,
								position.y, position.z, 100.0f, FollowMotor.this._owner.getInstanceId());
						position.z = lastMove.getZ() < newZ & Math.abs(lastMove.getZ() - newZ) > 1.0f
								? newZ + FollowMotor.this._owner.getObjectTemplate().getBoundRadius().getUpper()
										- FollowMotor.this._owner.getObjectTemplate().getHeight()
								: newZ;
					}
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(FollowMotor.this._owner, position.x, position.y, position.z,
							FollowMotor.this.new_targetHeading, false);
				} else {
					PacketSendUtility.broadcastPacket(FollowMotor.this._owner,
							new SM_MOVE(FollowMotor.this._owner.getObjectId(), FollowMotor.this._owner.getX(),
									FollowMotor.this._owner.getY(), FollowMotor.this._owner.getZ(),
									FollowMotor.this._owner.getX(), FollowMotor.this._owner.getY(),
									FollowMotor.this._owner.getZ(), FollowMotor.this._owner.getHeading(), (byte) 0));
					pathfindRevalidationTime = 0L;
				}
				FollowMotor.this._processor.schedule(new Runnable() {

					@Override
					public void run() {
						FollowMotor.this.update();
					}
				}, 0L);
			}
		}, TARGET_REVALIDATE_TIME);
		return true;
	}

	/**
	 * 判断 NPC 当前是否允许移动（非恐惧、可执行移动且未施法）。
	 * Whether the NPC may move (not under fear, can perform move, and not casting).
	 *
	 * @return 允许移动时为 true / {@code true} if movement is allowed
	 */
	private boolean canMove() {
		return !this._owner.getEffectController().isUnderFear() && this._owner.canPerformMove()
				&& this._owner.getAi2().getSubState() != AISubState.CAST;
	}
}
