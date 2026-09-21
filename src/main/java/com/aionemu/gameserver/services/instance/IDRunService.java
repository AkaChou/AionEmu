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

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;


/**
 * IDRun 副本报名服务，管理开启窗口与冷却。
 * IDRun instance registration service managing open windows and cooldowns.
 */

@Slf4j

public class IDRunService {
	private static volatile ObjectProvider<IDRunService> instanceProvider;
	private boolean registerAvailable;
	private final List<Integer> playersWithCooldown = new ArrayList<>();
	public static final byte minLevel = 10, capLevel = 76;
	public static final int maskId = 131;

	/**
	 * 初始化ID 奔跑报名：按配置调度开启报名。
	 * Initializes ID Run registration by scheduling open windows per config.
	 */
	public void initIDRun() {
		if (AutoGroupConfig.IDRUN_ENABLED) {
			log.info(I18n.get("log.d139cc6b9301"));
			// IDRun ？？？？ / IDRun ????
			/**
			 * 执行任务。
			 * Runs the task.
			 */GameCronServices.cronService().schedule(() -> startIDRunRegistration(), AutoGroupConfig.IDRUN_SCHEDULE);
		}
	}

	private void startUregisterIDRunTask() {
		/**
		 * 执行任务。
		 * Runs the task.
		 */GameThreadPoolServices.threadPoolManager().schedule(() -> {
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
		 }, AutoGroupConfig.IDRUN_TIMER * 60 * 1000);
	}

	private void startIDRunRegistration() {
		this.registerAvailable = true;
		startUregisterIDRunTask();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			if (player.getLevel() > minLevel && player.getLevel() < capLevel) {
				int instanceMaskId = getInstanceMaskId(player);
				if (instanceMaskId > 0) {
					PacketSendUtility.sendPacket(player,
							new SM_AUTO_GROUP(instanceMaskId, SM_AUTO_GROUP.wnd_EntryIcon));
					// 미지의 섬 전투에 참가할 수 있습니다.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDRun);
				}
			}
		}
	}

	/**
	 * 报名是否可用。
	 * Whether registration is available.
	 * @return 结果 / result
	 */
	public boolean isIDRunAvailable() {
		return this.registerAvailable;
	}

	/**
	 * 获取玩家的报名掩码 ID；等级不符时返回 0。
	 * Returns the registration mask id for the player, or 0 if level mismatch.
	 *
	 * 玩家 / player
	 * @return 结果 / result
	 */
	public int getInstanceMaskId(Player player) {
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
	public void showWindow(Player player, int instanceMaskId) {
		if (getInstanceMaskId(player) != instanceMaskId) {
			return;
		}
		if (!this.playersWithCooldown.contains(player.getObjectId())) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId));
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
	public static IDRunService getInstance() {
		ObjectProvider<IDRunService> provider = instanceProvider;
		IDRunService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("IDRunService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 设置服务提供者。
	 * Sets the service provider.
	 *
	 * @return 服务提供者 / provider
	 */
	public static void setInstanceProvider(ObjectProvider<IDRunService> provider) {
		instanceProvider = provider;
	}
}
