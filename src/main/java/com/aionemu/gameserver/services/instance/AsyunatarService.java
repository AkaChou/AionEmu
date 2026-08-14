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
 * 阿修纳塔副本报名服务，管理开启窗口与冷却。
 * Asyunatar instance registration service managing open windows and cooldowns.
 */

@Slf4j

public class AsyunatarService {
	private static volatile ObjectProvider<AsyunatarService> instanceProvider;
	private boolean registerAvailable;
	private final List<Integer> playersWithCooldown = new ArrayList<Integer>();
	public static final byte minLevel = 66, capLevel = 76;
	public static final int maskId = 121;

	/**
	 * 初始化阿修纳塔报名：按配置调度开启报名。
	 * Initializes Asyunatar registration by scheduling open windows per config.
	 */
	public void initAsyunatar() {
		if (AutoGroupConfig.ASHUNATAL_ENABLED) {
			log.info(I18n.get("log.a77e6c7b475a"));
			// 阿舒纳塔尔战舰 周一至周日 12:00–14:00 / Ashunatal Dredgion MON-TUE-WED-THU-FRI-SAT-SUN "12PM-2PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startAsyunatarRegistration();
				}
			}, AutoGroupConfig.ASHUNATAL_SCHEDULE_MIDDAY);
			// 阿舒纳塔尔战舰 周一至周日 12:00–14:00 / Ashunatal Dredgion MON-TUE-WED-THU-FRI "8PM-10PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startAsyunatarRegistration();
				}
			}, AutoGroupConfig.ASHUNATAL_SCHEDULE_EVENING);
			// 阿舒纳塔尔战舰 周六/日 23:00–00:00 / Ashunatal Dredgion SAT-SUN "11PM-00PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startAsyunatarRegistration();
				}
			}, AutoGroupConfig.ASHUNATAL_SCHEDULE_MIDNIGHT);
		}
	}

	private void startUregisterAsyunatarTask() {
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
		}, AutoGroupConfig.ASHUNATAL_TIMER * 60 * 1000);
	}

	private void startAsyunatarRegistration() {
		this.registerAvailable = true;
		startUregisterAsyunatarTask();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					// 进入阿舒纳塔尔战舰的传送现已开放。 / Transport into Ashunatal Dredgion has now been opened.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDDREADGION_04);
				}
			}
		}
	}

	/**
	 * 报名是否可用。
	 * Whether registration is available.
	 *
	 * @return 是否可用 / result
	 */
	public boolean isAsyunatarAvailable() {
		return this.registerAvailable;
	}

	/**
	 * 获取玩家的报名掩码 ID；等级不符时返回 0。
	 * Returns the registration mask id for the player, or 0 if level mismatch.
	 *
	 * @param player 玩家 / player
	 * @return 掩码 ID / result
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
	 * 玩家是否处于报名冷却。
	 * Whether the player is on registration cooldown.
	 *
	 * @param player 玩家 / player
	 * @return 是否冷却中 / result
	 */
	public boolean hasCoolDown(Player player) {
		return this.playersWithCooldown.contains(player.getObjectId());
	}

	/**
	 * 向符合条件的玩家显示报名窗口。
	 * Shows the registration window to eligible players.
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
		protected static final AsyunatarService instance = new AsyunatarService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务单例 / result
	 */
	public static AsyunatarService getInstance() {
		ObjectProvider<AsyunatarService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 设置服务提供者。
	 * Sets the service provider.
	 *
	 * @param instanceProvider 服务提供者 / service provider
	 */
	public static void setInstanceProvider(ObjectProvider<AsyunatarService> instanceProvider) {
		AsyunatarService.instanceProvider = instanceProvider;
	}
}
