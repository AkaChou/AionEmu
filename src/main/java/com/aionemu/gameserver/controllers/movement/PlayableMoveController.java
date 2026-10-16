package com.aionemu.gameserver.controllers.movement;


import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.StatFunctions;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.path.PathService;

/**
 * 可玩单位（玩家/召唤物）移动控制器基类，处理强制移动包、速度插值与恐惧控制。
 * Base move controller for playable units (player/summon): forced move packets, speed interpolation, and fear control.
 * @author ATracer
 * @param <T> 生物所有者类型 / Creature owner type
 */
public abstract class PlayableMoveController<T extends Creature> extends CreatureMoveController<T> {

	/** 是否需要发送移动包 / Whether a move packet should be sent */
	private boolean sendMovePacket = true;
	/** 真端 MoveState 滞后跟踪器 / Retail MoveState hysteresis tracker */
	private final RetailMoveStateTracker moveStateTracker = new RetailMoveStateTracker();

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
				PlayerMoveTaskManager.getInstance().addPlayer(owner);
			}
		}
	}

	/**
	 * 是否处于恐惧等强制控制状态。
	 * Whether under forced control such as fear.
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

		// 失控位移（恐惧/混沌）不得把角色推离可站立地面：平台/悬崖外的下一步落在空中时，客户端会拒绝
		// 该位移并把角色拉回原位，形成“跑出去又瞬间回位”的循环；拦下后由周期任务重选方向。
		// Forced movement (fear/confuse) must not push a grounded playable off walkable ground: the client
		// rejects an in-air step and snaps the character back, looping until the effect ends.
		if (isControlled() && !PathService.getInstance().hasStandableGround(owner, newX, newY, newZ)) {
			abortMove();
			return;
		}

		/*
		 * if ((movementMask & MovementMask.MOUSE) == 0) { targetDestX = newX + vectorX;
		 * targetDestY = newY + vectorY; targetDestZ = newZ + vectorZ; }
		 */

		World.getInstance().updatePosition(owner, newX, newY, newZ, heading, false);
		updateLastMove();
	}

	/**
	 * 中止移动：移除任务、清目标并广播停止。
	 * Abort movement: remove task, clear destination, and broadcast stop.
	 */
	@Override
	public void abortMove() {
		started.set(false);
		PlayerMoveTaskManager.getInstance().removePlayer(owner);
		targetDestX = 0;
		targetDestY = 0;
		targetDestZ = 0;
		setAndSendStopMove(owner);
	}

	/**
	 * 设置新目标点并在变化时标记需要发包；同步计算移动朝向档位。
	 * Set a new destination, mark packet send when changed, and recompute movement heading sector.
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

		// 真端鼠标移动不参与朝向差计算，恒判前进；键盘按朝向差分前进/侧移/后退。
		// Retail mouse movement skips the heading comparison and is always forward; keyboard input
		// is classified by heading difference into forward, sidestep, or backward.
		if ((movementMask & MovementMask.MOUSE) != 0) {
			recordRetailMoveState(1);
			return;
		}
		float h = MathUtil.calculateAngleFrom(owner.getX(), owner.getY(), targetDestX, targetDestY);
		if (h != 0) {
			int value = (int) (((heading * 3) - h) / 45);
			if (value < 0) {
				value += 8;
			}
			// 8 向档位映射真端枚举：0=正前(≤45°)→1；1/2/6/7=侧带→4；3/4/5=后带→2。
			// Map the 8-way sector to the retail enum: 0 forward (≤45°) →1; 1/2/6/7 side →4; 3/4/5 back →2.
			recordRetailMoveState(switch (value) {
				case 0 -> 1;
				case 3, 4, 5 -> 2;
				default -> 4;
			});
		}
	}

	/**
	 * 记录真端 MoveState（见 {@link RetailMoveStateTracker}）。
	 * Record the retail MoveState (see {@link RetailMoveStateTracker}).
	 * @param state 真端 MoveState 枚举值 / Retail MoveState enum value
	 */
	private void recordRetailMoveState(int state) {
		moveStateTracker.record(state, System.currentTimeMillis());
	}

	/**
	 * 返回当前真端 MoveState（0=静止、1=前进、2=后退、4=侧移，滞后期间可位或组合）。
	 * Return the current retail MoveState (0 standing, 1 forward, 2 backward, 4 sidestep;
	 * values may combine bitwise during the hysteresis window).
	 * @return 真端 MoveState / Retail MoveState
	 */
	public int getRetailMoveState() {
		if (!isInMove()) {
			recordRetailMoveState(0);
			return 0;
		}
		return moveStateTracker.current(System.currentTimeMillis());
	}

	/**
	 * 真端 MoveState 双状态滞后跟踪器（User_CalcMoveState / FUN_140578e10 / FUN_140578eb0 语义）：
	 * 方向变化时旧值若已超过 666ms 则升级为保留态（存活 1332ms），新值延迟 666ms 生效，读取时位或合并。
	 * Retail dual-state MoveState hysteresis tracker (User_CalcMoveState / FUN_140578e10 /
	 * FUN_140578eb0 semantics): on change, a direction older than 666 ms is promoted to the
	 * retained state (alive 1332 ms), the new value takes effect after 666 ms, and reads merge
	 * both bitwise.
	 */
	static final class RetailMoveStateTracker {

		/** 新方向生效延迟 / Delay before a new direction takes effect */
		static final long EFFECT_DELAY_MS = 666;
		/** 旧方向保留时长 / How long the previous direction stays alive */
		static final long RETAIN_MS = 1332;

		private int retainedState;
		private long retainedTime;
		private int pendingState;
		private long pendingTime;

		/**
		 * 记录一次方向写入；与当前待生效值相同则不刷新时间戳。
		 * Record one direction write; writing the same pending value keeps its timestamp.
		 * @param state 真端 MoveState 枚举值 / Retail MoveState enum value
		 * @param now   当前时间 / Current time
		 */
		void record(int state, long now) {
			if (state != pendingState) {
				if (pendingState != 0 && now - pendingTime > EFFECT_DELAY_MS) {
					retainedState = pendingState;
					retainedTime = now;
				}
				pendingState = state;
				pendingTime = now;
			}
		}

		/**
		 * 读取当前生效 MoveState：保留态 1332ms 内并入，待生效态 666ms 后并入，位或后取低 3 位。
		 * Read the effective MoveState: the retained state merges within 1332 ms, the pending one
		 * after 666 ms, merged bitwise and masked to 3 bits.
		 * @param now 当前时间 / Current time
		 * @return 真端 MoveState / Retail MoveState
		 */
		int current(long now) {
			int state = 0;
			if (retainedState != 0 && now - retainedTime < RETAIN_MS) {
				state = retainedState;
			}
			if (pendingState != 0 && now - pendingTime > EFFECT_DELAY_MS) {
				state |= pendingState;
			}
			return state & 7;
		}
	}
}
