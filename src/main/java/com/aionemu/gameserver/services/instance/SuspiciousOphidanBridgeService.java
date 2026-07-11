package com.aionemu.gameserver.services.instance;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;


/****/
/**
 * 可疑的奥菲丹桥副本报名服务，管理开启窗口与冷却。
 * Suspicious Ophidan Bridge registration service managing open windows and cooldowns.
 */

@Slf4j

public class SuspiciousOphidanBridgeService {
	private static volatile ObjectProvider<SuspiciousOphidanBridgeService> instanceProvider;
	private boolean registerAvailable;
	private final List<Integer> playersWithCooldown = new ArrayList<Integer>();
	public static final byte minLevel = 66, capLevel = 76;
	public static final int maskId = 122;

	/**
	 * initSuspiciousOphidan 方法。
	 * initSuspiciousOphidan method.
	 */
	public void initSuspiciousOphidan() {
		if (AutoGroupConfig.OPHIDAN_WARPATH_ENABLED) {
			log.info(I18n.get("log.926526f1607c"));
			// 奥菲丹战道 二/四 23:00–00:00 / Ophidan Warpath TUE-THU "11PM-00AM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startSuspiciousOphidanRegistration();
				}
			}, AutoGroupConfig.OPHIDAN_WARPATH_SCHEDULE_MIDNIGHT);
		}
	}

	private void startUregisterSuspiciousTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				registerAvailable = false;
				playersWithCooldown.clear();
				GameCoreGameplayServices.autoGroupService().unRegisterInstance(maskId);
				Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
				while (iter.hasNext()) {
					Player player = iter.next();
					if (player.getLevel() > minLevel) {
						int instanceMaskId = getInstanceMaskId(player);
						if (instanceMaskId > 0) {
							PacketSendUtility.sendPacket(player,
									new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon, true));
						}
					}
				}
			}
		}, AutoGroupConfig.OPHIDAN_WARPATH_TIMER * 60 * 1000);
	}

	private void startSuspiciousOphidanRegistration() {
		this.registerAvailable = true;
		startUregisterSuspiciousTask();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					// 你现在可参与奇异奥菲丹进阶路线战斗。 / You can now participate in the Odd Ophidan Advanced Route battle.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDLDF5_Under_02_War);
				}
			}
		}
	}

	/**
	 * isSuspiciousAvailable 方法。
	 * isSuspiciousAvailable method.
	 * result
	 */
	public boolean isSuspiciousAvailable() {
		return this.registerAvailable;
	}

	/**
	 * getInstanceMaskId 方法。
	 * getInstanceMaskId method.
	 *
	 * 玩家 / player
	 * result
	 */
	public byte getInstanceMaskId(Player player) {
		int level = player.getLevel();
		if (level < minLevel || level >= capLevel) {
			return 0;
		}
		return maskId;
	}

	/**
	 * 添加冷却。
	 * Adds a cooldown.
	 *
	 * @param player 玩家 / player
	 */
	public void addCoolDown(Player player) {
		this.playersWithCooldown.add(player.getObjectId());
	}

	/**
	 * 是否处于冷却。
	 * Whether cooldown is active.
	 *
	 * 玩家 / player
	 * result
	 */
	public boolean hasCoolDown(Player player) {
		return this.playersWithCooldown.contains(player.getObjectId());
	}

	/**
	 * 显示报名窗口。
	 * Shows the registration window.
	 *
	 * 玩家 / player
	 * instanceMaskId
	 */
	public void showWindow(Player player, byte instanceMaskId) {
		if (getInstanceMaskId(player) != instanceMaskId) {
			return;
		}
		if (!this.playersWithCooldown.contains(player.getObjectId())) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId));
		}
	}

	private static class SingletonHolder {
		protected static final SuspiciousOphidanBridgeService instance = new SuspiciousOphidanBridgeService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static SuspiciousOphidanBridgeService getInstance() {
		ObjectProvider<SuspiciousOphidanBridgeService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<SuspiciousOphidanBridgeService> provider) {
		instanceProvider = provider;
	}
}