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
 * 伊吉尔穹顶报名服务，管理开启窗口与冷却。
 * Idgel Dome registration service managing open windows and cooldowns.
 */

@Slf4j

public class IdgelDomeService {
	private static volatile ObjectProvider<IdgelDomeService> instanceProvider;

	private boolean registerAvailable;
	private final List<Integer> playersWithCooldown = new ArrayList<Integer>();
	public static final byte minLevel = 61, capLevel = 66;
	public static final int maskId = 111;

	/**
	 * initIdgelDome 方法。
	 * initIdgelDome method.
	 */
	public void initIdgelDome() {
		if (AutoGroupConfig.IDGEL_ENABLED) {
			log.info(I18n.get("log.770479d27791"));
			// 伊杰尔穹顶 一/三/五 12:00–13:00 / Idgel Dome MON-WED-FRI "12PM-1PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startIdgelRegistration();
				}
			}, AutoGroupConfig.IDGEL_SCHEDULE_MIDDAY);
			// 伊杰尔穹顶 一/三/五 23:00–00:00 / Idgel Dome MON-WED-FRI "11PM-0PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startIdgelRegistration();
				}
			}, AutoGroupConfig.IDGEL_SCHEDULE_MIDNIGHT);
		}
	}

	private void startUregisterIdgelTask() {
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
		}, AutoGroupConfig.IDGEL_TIMER * 60 * 1000);
	}

	private void startIdgelRegistration() {
		this.registerAvailable = true;
		startUregisterIdgelTask();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					// 你现在可参与伊杰尔穹顶战斗。 / You can now participate in the Idgel Dome battle.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDLDF5_Fortress_Re);
				}
			}
		}
	}

	/**
	 * isIdgelAvailable 方法。
	 * isIdgelAvailable method.
	 * result
	 */
	public boolean isIdgelAvailable() {
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
		protected static final IdgelDomeService instance = new IdgelDomeService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static IdgelDomeService getInstance() {
		ObjectProvider<IdgelDomeService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<IdgelDomeService> provider) {
		instanceProvider = provider;
	}
}