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
@Slf4j


/**
 * 掘金号副本报名服务（第二代），管理开启窗口与冷却。
 * Dredgion instance registration service (v2) managing open windows and cooldowns.
 */
public class DredgionService2 {
	private static volatile ObjectProvider<DredgionService2> instanceProvider;

	private boolean registerAvailable;
	private List<Integer> playersWithCooldown = new ArrayList<Integer>();
	private SM_AUTO_GROUP[] autoGroupUnreg, autoGroupReg;
	private final byte maskLvlGradeC = 1, maskLvlGradeB = 2, maskLvlGradeA = 3;
	public static final byte minLevel = 46, capLevel = 61;

	public DredgionService2() {
		this.autoGroupUnreg = new SM_AUTO_GROUP[this.maskLvlGradeA + 1];
		this.autoGroupReg = new SM_AUTO_GROUP[this.autoGroupUnreg.length];
		for (byte i = this.maskLvlGradeC; i <= this.maskLvlGradeA; i++) {
			this.autoGroupUnreg[i] = new SM_AUTO_GROUP(i, SM_AUTO_GROUP.wnd_EntryIcon, true);
			this.autoGroupReg[i] = new SM_AUTO_GROUP(i, SM_AUTO_GROUP.wnd_EntryIcon);
		}
	}

	/**
	 * initDredgion 方法。
	 * initDredgion method.
	 */
	public void initDredgion() {
		if (AutoGroupConfig.DREDGION_ENABLED) {
			log.info(I18n.get("log.e829b9492ce7"));
			// 战舰 周一至周日 12:00–13:00 / Dredgion MON-TUE-WED-THU-FRI-SAT-SUN "12PM-1PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startDredgionRegistration();
				}
			}, AutoGroupConfig.DREDGION_SCHEDULE_MIDDAY);
			// 战舰 周一至周日 20:00–21:00 / Dredgion MON-TUE-WED-THU-FRI-SAT-SUN "8PM-9PM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startDredgionRegistration();
				}
			}, AutoGroupConfig.DREDGION_SCHEDULE_EVENING);
			// 战舰 周一至周日 23:00–00:00 / Dredgion MON-TUE-WED-THU-FRI-SAT-SUN "23PM-0AM"
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				/**
				 * 执行任务。
				 * Runs the task.
				 */
				public void run() {
					startDredgionRegistration();
				}
			}, AutoGroupConfig.DREDGION_SCHEDULE_MIDNIGHT);
		}
	}

	private void startUregisterDredgionTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				registerAvailable = false;
				playersWithCooldown.clear();
				GameCoreGameplayServices.autoGroupService().unRegisterInstance(maskLvlGradeA);
				GameCoreGameplayServices.autoGroupService().unRegisterInstance(maskLvlGradeB);
				GameCoreGameplayServices.autoGroupService().unRegisterInstance(maskLvlGradeC);
				Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
				while (iter.hasNext()) {
					Player player = iter.next();
					if (player.getLevel() > minLevel) {
						int instanceMaskId = getInstanceMaskId(player);
						if (instanceMaskId > 0) {
							PacketSendUtility.sendPacket(player, DredgionService2.this.autoGroupUnreg[instanceMaskId]);
						}
					}
				}
			}
		}, AutoGroupConfig.DREDGION_TIMER * 60 * 1000);
	}

	private void startDredgionRegistration() {
		this.registerAvailable = true;
		startUregisterDredgionTask();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player, this.autoGroupReg[instanceMaskId]);
					switch (instanceMaskId) {
					case maskLvlGradeC:
						// 进入战舰的渗透路线已开放。 / An infiltration route into the Dredgion is open.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDAB1_DREADGION);
						break;
					case maskLvlGradeB:
						// 进入钱特拉战舰的渗透通道已开启。 / An infiltration passage into the Chantra Dredgion has opened.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDDREADGION_02);
						break;
					case maskLvlGradeA:
						// 进入特拉斯战舰的渗透通道已开启。 / An infiltration passage into the Terath Dredgion has opened.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDDREADGION_03);
						break;
					}
				}
			}
		}
	}

	/**
	 * isDredgionAvailable 方法。
	 * isDredgionAvailable method.
	 * result
	 */
	public boolean isDredgionAvailable() {
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
		if (level < 51) {
			return this.maskLvlGradeC;
		} else if (level < 56) {
			return this.maskLvlGradeB;
		} else {
			return this.maskLvlGradeA;
		}
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
	public void showWindow(Player player, int instanceMaskId) {
		if (getInstanceMaskId(player) != instanceMaskId) {
			return;
		}
		if (!this.playersWithCooldown.contains(player.getObjectId())) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId));
		}
	}

	private static class SingletonHolder {
		protected static final DredgionService2 instance = new DredgionService2();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static DredgionService2 getInstance() {
		ObjectProvider<DredgionService2> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param instanceProvider 副本提供者 / instanceProvider
	 */
	public static void setInstanceProvider(ObjectProvider<DredgionService2> instanceProvider) {
		DredgionService2.instanceProvider = instanceProvider;
	}
}
