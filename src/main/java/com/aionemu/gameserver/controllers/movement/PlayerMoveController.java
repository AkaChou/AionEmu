package com.aionemu.gameserver.controllers.movement;

import com.aionemu.gameserver.configs.main.FallDamageConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.stats.StatFunctions;

/**
 * 玩家移动控制器，额外处理坠落伤害累计与结算。
 * Player move controller with fall-damage accumulation and settlement.
 */
public class PlayerMoveController extends PlayableMoveController<Player> {

	/** 累计坠落距离 / Accumulated fall distance */
	private float fallDistance;
	/** Last fall Z / Last fall Z */
	private float lastFallZ;

	/**
	 * 使用指定玩家构造控制器。
	 * Construct the controller for the given player.
	 *
	 * @param owner 玩家 / Player owner
	 */
	public PlayerMoveController(Player owner) {
		super(owner);
	}

	/**
	 * 更新坠落中状态并在超过阈值时计算伤害。
	 * Update falling state and calculate damage when the threshold is exceeded.
	 *
	 * @param newZ 当前 Z 坐标 / Current Z
	 */
	public void updateFalling(float newZ) {
		if (lastFallZ != 0) {
			fallDistance += lastFallZ - newZ;
			if (fallDistance >= FallDamageConfig.MAXIMUM_DISTANCE_MIDAIR) {
				StatFunctions.calculateFallDamage(owner, fallDistance, false);
			}
		}
		lastFallZ = newZ;
		owner.getObserveController().notifyMoveObservers();
	}

	/**
	 * 停止坠落并结算最终坠落伤害。
	 * Stop falling and settle the final fall damage.
	 */
	public void stopFalling() {
		if (lastFallZ != 0) {
			if (!owner.isFlying()) {
				StatFunctions.calculateFallDamage(owner, fallDistance, true);
			}
			fallDistance = 0;
			lastFallZ = 0;
			owner.getObserveController().notifyMoveObservers();
		}
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
