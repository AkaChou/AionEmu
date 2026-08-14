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
 * 坚韧殿堂报名服务，管理开启窗口与冷却。
 * Hall of Tenacity registration service managing open windows and cooldowns.
 */

@Slf4j

public class HallOfTenacityService {
	private static volatile ObjectProvider<HallOfTenacityService> instanceProvider;
	private boolean registerAvailable;
	private final List<Integer> playersWithCooldown = new ArrayList<Integer>();
	public static final byte minLevel = 66, capLevel = 76;
	public static final int maskId = 125;

	/**
	 * 初始化黄金神庙训练所报名：按配置调度开启报名。
	 * Initializes Hall of Tenacity registration by scheduling open windows per config.
	 */
	public void initHallOfTenacity() {
		if (AutoGroupConfig.HALL_OF_TENACITY_ENABLED) {
			log.info(I18n.get("log.9b3ac2974711"));
			// 坚韧大厅 周六/日 09:00–17:00 / Hall Of Tenacity SAT-SUN "9AM-5PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startHallOfTenacityRegistration();
				}
			}, AutoGroupConfig.HALL_OF_TENACITY_SCHEDULE_MORNING);
			// 坚韧大厅 周一至周五 18:00–00:00 / Hall Of Tenacity MON-TUE-WED-THU-FRI "6PM-0AM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startHallOfTenacityRegistration();
				}
			}, AutoGroupConfig.HALL_OF_TENACITY_SCHEDULE_EVENING);
		}
	}

	private void startUregisterHallOfTenacityTask() {
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
		}, AutoGroupConfig.HALL_OF_TENACITY_TIMER * 60 * 1000);
	}

	/**
	 * 开启报名窗口。
	 * Starts the registration window.
	 */
	public void startHallOfTenacityRegistration() {
		this.registerAvailable = true;
		startUregisterHallOfTenacityTask();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					// 坚韧大厅现已开放。 / The Hall of Tenacity is now open.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDTM_Lobby01);
				}
			}
		}
	}

	/**
	 * 报名是否可用。
	 * Whether registration is available.
	 * @return 结果 / result
	 */
	public boolean isHallAvailable() {
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
		protected static final HallOfTenacityService instance = new HallOfTenacityService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * @return 结果 / result
	 */
	public static HallOfTenacityService getInstance() {
		ObjectProvider<HallOfTenacityService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<HallOfTenacityService> provider) {
		instanceProvider = provider;
	}
}