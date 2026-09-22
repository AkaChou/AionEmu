package com.aionemu.gameserver.services.events;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;


import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.skillengine.model.TransformType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 土匪活动服务，管理土匪变身、宣战与结算。
 * Bandit event service managing bandit morph, attack state and settlement.
 *
 * @author Rinzler (Encom)
 */

@Slf4j
public class BanditService {
	private static volatile ObjectProvider<BanditService> instanceProvider;

	/**
	 * 初始化服务。
	 * Initializes the service.
	 */
	public void onInit() {
		log.info(I18n.get("log.74d903aaf6ee"));
	}

	/**
	 * 开始土匪状态。
	 * Starts bandit state.
	 *
	 * @param player 玩家 / player
	 */
	public void startBandit(final Player player) {
		player.getEffectController().setAbnormal(AbnormalState.SLEEP.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
		final ActionObserver observer = new ActionObserver(ObserverType.ATTACKED) {
			@Override
			/**
			 * attacked 方法。
			 * attacked method.
			 *
			 * creature
			 */
			public void attacked(Creature creature) {
				if (player.getController().hasTask(TaskId.PK)) {
					player.getController().cancelTask(TaskId.PK);
					player.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
					player.getEffectController().updatePlayerEffectIcons();
					player.getEffectController().broadCastEffects();
				}
			}
		};
		player.getObserveController().attach(observer);
		/**
		 * 执行任务。
		 * Runs the task.
		 */player.getController().addTask(TaskId.PK, GameThreadPoolServices.threadPoolManager().schedule(() -> {
			 player.getObserveController().removeObserver(observer);
			 if (player.getLifeStats().isAlreadyDead()) {
				 PlayerReviveService.skillRevive(player);
			 }
			 if (player.isInGroup2()) {
				 PlayerGroupService.removePlayer(player);
			 }
			 if (player.isInAlliance2()) {
				 PlayerAllianceService.removePlayer(player);
			 }
			 player.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
			 player.getEffectController().updatePlayerEffectIcons();
			 player.getEffectController().broadCastEffects();
			 player.setBandit(true);
			 player.setAdminEnmity(2);
			 player.setAdminNeutral(0);
			 morphBandit(player, false);
			 player.clearKnownlist();
			 sendAnnounce(player);
			 PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
			 PacketSendUtility.sendPacket(player,
					 new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
			 player.getEffectController().updatePlayerEffectIcons();
			 player.updateKnownlist();
			 TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getX(),
					 player.getY(), player.getZ(), player.getHeading());
		 }, 10 * 1000));
	}

	/**
	 * 结束土匪状态。
	 * Stops bandit state.
	 *
	 * @param player 玩家 / player
	 */
	public void stopBandit(final Player player) {
		player.getEffectController().setAbnormal(AbnormalState.SLEEP.getId());
		player.getEffectController().updatePlayerEffectIcons();
		player.getEffectController().broadCastEffects();
		final ActionObserver observer = new ActionObserver(ObserverType.ATTACKED) {
			@Override
			/**
			 * attacked 方法。
			 * attacked method.
			 *
			 * creature
			 */
			public void attacked(Creature creature) {
				if (player.getController().hasTask(TaskId.PK)) {
					player.getController().cancelTask(TaskId.PK);
					player.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
					player.getEffectController().updatePlayerEffectIcons();
					player.getEffectController().broadCastEffects();
				}
			}
		};
		player.getObserveController().attach(observer);
		/**
		 * 执行任务。
		 * Runs the task.
		 */player.getController().addTask(TaskId.PK, GameThreadPoolServices.threadPoolManager().schedule(() -> {
			 player.getObserveController().removeObserver(observer);
			 if (player.getLifeStats().isAlreadyDead()) {
				 PlayerReviveService.skillRevive(player);
			 }
			 if (player.isInGroup2()) {
				 PlayerGroupService.removePlayer(player);
			 }
			 if (player.isInAlliance2()) {
				 PlayerAllianceService.removePlayer(player);
			 }
			 player.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
			 player.getEffectController().updatePlayerEffectIcons();
			 player.getEffectController().broadCastEffects();
			 player.setBandit(false);
			 player.setAdminEnmity(0);
			 player.setAdminNeutral(0);
			 morphBandit(player, true);
			 player.clearKnownlist();
			 PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
			 PacketSendUtility.sendPacket(player,
					 new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
			 player.getEffectController().updatePlayerEffectIcons();
			 player.updateKnownlist();
			 TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getX(),
					 player.getY(), player.getZ(), player.getHeading());
		 }, 10 * 1000));
	}

	/**
	 * 单位死亡时处理。
	 * Handles unit death.
	 *
	 * @param player 玩家 / player
	 * @param lastAttacker 最后攻击者 / lastAttacker
	 */
	public void onDie(final Player player, final Creature lastAttacker) {
		Summon summon = player.getSummon();
		if (summon != null) {
			summon.getController().release(UnsummonType.UNSPECIFIED);
		}
		PacketSendUtility.broadcastPacket(player,
				new SM_EMOTION(player, EmotionType.DIE, 0, lastAttacker == null ? 0 : lastAttacker.getObjectId()),
				true);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DEATH_MESSAGE_ME);
		player.getMoveController().abortMove();
		player.setState(CreatureState.DEAD);
		player.getObserveController().notifyDeathObservers(player);
		player.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
		player.getEffectController().removeEffectByDispelCat(DispelCategoryType.ALL, SkillTargetSlot.DEBUFF, 100, 2,
				100);
		player.setTarget(null);
		PacketSendUtility.sendPacket(player, new SM_TARGET_SELECTED(player));
		/**
		 * 执行任务。
		 * Runs the task.
		 */GameThreadPoolServices.threadPoolManager().schedule(() -> {
			 if (player.isBandit()) {
				 if (player.getLifeStats().isAlreadyDead()) {
					 PlayerReviveService.banditRevive(player);
				 }
				 player.setBandit(false);
				 player.setAdminEnmity(0);
				 player.setAdminNeutral(0);
				 player.clearKnownlist();
				 morphBandit(player, true);
				 sendDieAnnounce(player, (Player) lastAttacker);
				 PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
				 PacketSendUtility.sendPacket(player,
						 new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
				 player.getEffectController().updatePlayerEffectIcons();
				 player.updateKnownlist();
				 TeleportService2.moveToBindLocation(player, true);
			 }
		 }, 6000);
	}

	/**
	 * 土匪变身。
	 * Morphs into bandit form.
	 *
	 * @param player 玩家 / player
	 * @param die 是否死亡 / die
	 */
	public void morphBandit(Player player, boolean die) {
		if (!die) {
			player.getTransformModel().setModelId(219655); // 嗜血的瓦姆皮达鲁 / Bloodthirsty Vampidaru.
			player.getTransformModel().setPanelId(0);
			player.getTransformModel().setTransformType(TransformType.PC);
			PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 0, true, 0));
			player.setTransformed(true);
			player.setTransformedModelId(219655); // 嗜血的瓦姆皮达鲁 / Bloodthirsty Vampidaru.
		} else {
			player.getTransformModel().setModelId(0);
			player.getTransformModel().setPanelId(0);
			player.getTransformModel().setTransformType(TransformType.PC);
			PacketSendUtility.sendPacket(player, new SM_TRANSFORM(player, 0, false, 0));
			player.setTransformed(false);
			player.setTransformedModelId(0);
		}
	}

	/**
	 * 发送公告。
	 * Sends an announcement.
	 *
	 * @param player 玩家 / player
	 */
	public void sendAnnounce(final Player player) {
		/**
		 * visit 方法。
		 * visit method.
		 *
		 * @param pl 玩家 / pl
		 */com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(pl -> {
			 if (pl.getWorldId() == player.getWorldId() && pl != player) {
				 PacketSendUtility.sendSys3Message(pl, "[PK] Bandit", "A player just passed <Outlaw>, RUN!");
			 }
		 });
	}

	/**
	 * sendDieAnnounce 方法。
	 * sendDieAnnounce method.
	 *
	 * looser
	 * killer
	 */
	public void sendDieAnnounce(final Player looser, final Player killer) {
		/**
		 * visit 方法。
		 * visit method.
		 *
		 * @param pl 玩家 / pl
		 */com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(pl -> {
			 if (pl.getWorldId() == looser.getWorldId()) {
				 PacketSendUtility.sendSys3Message(pl, "[PK] Bandit",
						 killer.getName() + " stop the <Outlaw> (" + looser.getName() + ") !");
			 }
		 });
	}

	/**
	 * 击杀时处理。
	 * Handles a kill event.
	 *
	 * 玩家 / player
	 * diedPlayer
	 */
	public void onKill(Player player, Player diedPlayer) {
		player.setbanditKillStreak(player.getBanditKillStreak() + 1);
		if (player.getBanditKillStreak() == 5) {
			startBandit(player);
		}
		if (diedPlayer.getBanditKillStreak() > 0) {
			diedPlayer.setbanditKillStreak(0);
		}
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final BanditService getInstance() {
		ObjectProvider<BanditService> provider = instanceProvider;
		BanditService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("BanditService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param instanceProvider 副本提供者 / instanceProvider
	 */
	public static void setInstanceProvider(ObjectProvider<BanditService> instanceProvider) {
		BanditService.instanceProvider = instanceProvider;
	}
}
