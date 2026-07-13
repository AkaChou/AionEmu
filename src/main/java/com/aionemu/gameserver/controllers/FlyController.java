package com.aionemu.gameserver.controllers;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 玩家飞行控制器，管理飞行、滑翔状态切换与飞行冷却。
 * Player fly controller managing flight, gliding state switches and fly cooldown.
 *
 * @author ATracer
 */
@Slf4j
public class FlyController {

	/** 飞行复用冷却时间（毫秒）。 / Fly reuse cooldown in milliseconds. */
	private static final long FLY_REUSE_TIME = 10000;
	/** 关联玩家。 / Associated player. */
	private Player player;
	/** 异常状态导致无法移动时停止滑翔的观察者。 / Observer that stops gliding when an abnormal state prevents movement. */
	private ActionObserver glideObserver = new ActionObserver(ObserverType.ABNORMALSETTED) {

		public void abnormalsetted(AbnormalState state) {
			if ((state.getId() & AbnormalState.CANT_MOVE_STATE.getId()) > 0 && !player.isInvulnerableWing()) {
				player.getFlyController().onStopGliding(true);
			}
		}
	};

	/**
	 * 为指定玩家构造飞行控制器。
	 * Constructs a fly controller for the given player.
	 *
	 * associated player
	 */
	public FlyController(Player player) {
		this.player = player;
	}

	/**
	 * 停止滑翔；若未在飞行则恢复 FP 并可选择收起翅膀。
	 * Stops gliding; restores FP when not flying and optionally removes wings.
	 *
	 * @param removeWings 是否广播落地并收翼 / whether to broadcast landing and remove wings
	 */
	public void onStopGliding(boolean removeWings) {
		if (player.isInState(CreatureState.GLIDING)) {
			player.unsetState(CreatureState.GLIDING);

			if (player.isInState(CreatureState.FLYING)) {
				player.setFlyState(1);
			} else {
				player.setFlyState(0);
				player.getLifeStats().triggerFpRestore();
				if (removeWings) {
					PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.LAND, 0, 0), true);
				}
			}

			player.getObserveController().removeObserver(glideObserver);
			player.getGameStats().updateStatsAndSpeedVisually();
		}
	}

	/**
	 * 结束飞行。可由客户端情绪包、传送或 FP 耗尽触发。
	 * Ends flying. Triggered by client emotion packets, teleport, or FP exhaustion.
	 *
	 * @param forceEndFly 是否强制广播落地动画 / whether to force-broadcast the landing animation
	 */
	public void endFly(boolean forceEndFly) {
		if (player.isInState(CreatureState.FLYING) || player.isInState(CreatureState.GLIDING)) {
			player.unsetState(CreatureState.FLYING);
			player.unsetState(CreatureState.GLIDING);
			player.unsetState(CreatureState.FLOATING_CORPSE);
			player.setFlyState(0);

			if (forceEndFly) {
				PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.LAND, 0, 0), true);
			}

			player.getObserveController().removeObserver(glideObserver);
			player.getGameStats().updateStatsAndSpeedVisually();
			player.getLifeStats().triggerFpRestore();
		}
	}

	/**
	 * 开始飞行（由 CM_EMOTION 页上键或飞行按钮触发）。
	 * Starts flying (triggered by CM_EMOTION pageUp or the fly button).
	 */
	public void startFly() {
		startFly(true);
	}

	/**
	 * 开始飞行，并按需广播起飞动作。
	 * Starts flying and optionally broadcasts the take-off action.
	 *
	 * @param broadcastPacket 是否广播起飞动作 / whether to broadcast the take-off action
	 */
	public void startFly(boolean broadcastPacket) {
		if (player.getTransformModel().isFlyDisabled()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANT_FLY_NOW_DUE_TO_NOFLY);
			return;
		}
		if (player.getFlyReuseTime() > System.currentTimeMillis()) {
			AuditLogger.info(player, "No Flight Cooldown Hack. Reuse time: "
					+ ((player.getFlyReuseTime() - System.currentTimeMillis()) / 1000));
			return;
		}
		player.setFlyReuseTime(System.currentTimeMillis() + FLY_REUSE_TIME);
		player.setState(CreatureState.FLYING);
		if (player.isInPlayerMode(PlayerMode.RIDE)) {
			player.setState(CreatureState.FLOATING_CORPSE);
		}
		player.setFlyState(1);
		player.getLifeStats().triggerFpReduce();
		if (broadcastPacket) {
			PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.FLY, 0, 0), true);
		}
		player.getGameStats().updateStatsAndSpeedVisually();
	}

	/**
	 * 切换到滑翔模式（由 CM_MOVE 的 VALIDATE_GLIDE 触发）。
	 * Switches to gliding mode (triggered by CM_MOVE with VALIDATE_GLIDE).
	 *
	 * @return 是否成功切换（含已在滑翔） / whether the switch succeeded (including already gliding)
	 */
	public boolean switchToGliding() {
		if (player.getTransformModel().isFlyDisabled()) {
			return false;
		}
		if (!player.isInState(CreatureState.GLIDING) && player.canPerformMove()) {
			if (player.getFlyReuseTime() > System.currentTimeMillis()) {
				return false;
			}
			player.setFlyReuseTime(System.currentTimeMillis() + FLY_REUSE_TIME);
			player.setState(CreatureState.GLIDING);
			if (player.getFlyState() == 0) {
				player.getLifeStats().triggerFpReduce();
			}
			player.setFlyState(2);

			player.getObserveController().addObserver(glideObserver);
			player.getGameStats().updateStatsAndSpeedVisually();
		}
		return true;
	}
}
