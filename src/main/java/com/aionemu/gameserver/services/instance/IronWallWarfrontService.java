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


/**
 * 铁壁前线报名服务，管理开启窗口与冷却。
 * Iron Wall Warfront registration service managing open windows and cooldowns.
 */

@Slf4j

public class IronWallWarfrontService {
	private static volatile ObjectProvider<IronWallWarfrontService> instanceProvider;

	private boolean registerAvailable;
	private final List<Integer> playersWithCooldown = new ArrayList<Integer>();
	public static final byte minLevel = 66, capLevel = 76;
	public static final int maskId = 109;

	/**
	 * 初始化钢铁城墙战线报名：按配置调度开启报名。
	 * Initializes Iron Wall Warfront registration by scheduling open windows per config.
	 */
	public void initIronWallWarfront() {
		log.info(I18n.get("log.578bb9df9c44"));
		if (AutoGroupConfig.BASTION_ENABLED) {
			// 铁壁战线 周五 23:00–00:00 / Iron Wall Warfront FRI "11PM-0AM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startBastionRegistration();
				}
			}, AutoGroupConfig.BASTION_SCHEDULE_MIDNIGHT);
		}
	}

	private void startUregisterBastionTask() {
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
		}, AutoGroupConfig.BASTION_TIMER * 60 * 1000);
	}

	private void startBastionRegistration() {
		this.registerAvailable = true;
		startUregisterBastionTask();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDF5_TD_war);
				}
			}
		}
	}

	/**
	 * 报名是否可用。
	 * Whether registration is available.
	 * @return 结果 / result
	 */
	public boolean isBastionAvailable() {
		return this.registerAvailable;
	}

	/**
	 * 获取玩家的报名掩码 ID；等级不符时返回 0。
	 * Returns the registration mask id for the player, or 0 if level mismatch.
	 *
	 * 玩家 / player
	 * @return 结果 / result
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
	 * @return 结果 / result
	 */
	public boolean hasCoolDown(Player player) {
		return this.playersWithCooldown.contains(player.getObjectId());
	}

	/**
	 * 显示报名窗口。
	 * Shows the registration window.
	 *
	 * @param player 玩家 / player
	 * @param instanceMaskId 副本掩码 ID / instance mask id
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
		protected static final IronWallWarfrontService instance = new IronWallWarfrontService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * @return 结果 / result
	 */
	public static IronWallWarfrontService getInstance() {
		ObjectProvider<IronWallWarfrontService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置服务提供者。
	 * Sets the service provider.
	 *
	 * @return 服务提供者 / provider
	 */
	public static void setInstanceProvider(ObjectProvider<IronWallWarfrontService> provider) {
		instanceProvider = provider;
	}
}