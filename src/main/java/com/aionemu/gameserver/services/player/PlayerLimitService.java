package com.aionemu.gameserver.services.player;

import com.aionemu.gameserver.lifecycle.GameCronServices;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.SellLimit;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家限制服务，管理出售限额等周期限制。
 * Player limit service managing periodic limits such as sell caps.
 *
 * @author Source
 */

public class PlayerLimitService {

	private static ConcurrentMap<Integer, Long> sellLimit = new ConcurrentHashMap<Integer, Long>();
	private static volatile ObjectProvider<PlayerLimitService> instanceProvider;

	/**
	 * 更新出售限额。
	 * Updates sell limit.
	 *
	 * 玩家 / player
	 * reward
	 * result
	 */
	public static boolean updateSellLimit(Player player, long reward) {
		if (!CustomConfig.LIMITS_ENABLED) {
			return true;
		}
		int accountId = player.getPlayerAccount().getId();
		AtomicBoolean allowed = new AtomicBoolean();
		AtomicLong remaining = new AtomicLong();
		sellLimit.compute(accountId, (id, currentLimit) -> {
			long limit = currentLimit == null ? SellLimit.getSellLimit(player.getPlayerAccount().getMaxPlayerLevel()) * CustomConfig.LIMITS_RATE : currentLimit;
			if (limit < reward) {
				allowed.set(false);
				remaining.set(limit);
				return limit;
			}
			long updatedLimit = limit - reward;
			allowed.set(true);
			remaining.set(updatedLimit);
			return updatedLimit;
		});

		if (!allowed.get()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DAY_CANNOT_SELL_NPC(remaining.get()));
			return false;
		}
		return true;
	}

	/**
	 * 调度限额更新。
	 * Schedules limit update.
	 */
	public void scheduleUpdate() {
		GameCronServices.cronService().schedule(new Runnable() {

			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				sellLimit.clear();
			}

		}, CustomConfig.LIMITS_UPDATE, true);
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static PlayerLimitService getInstance() {
		ObjectProvider<PlayerLimitService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<PlayerLimitService> instanceProvider) {
		PlayerLimitService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {

		protected static final PlayerLimitService instance = new PlayerLimitService();
	}
}
