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
	 * 更新出售限额，超出限额则拒绝并提示。
	 * Updates the sell limit, rejecting and notifying when exceeded.
	 *
	 * @param player 玩家 / player
	 * @param reward 本次出售所得 / reward gained
	 * @return 是否允许出售 / whether selling is allowed
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
	public static PlayerLimitService getInstance() {
		ObjectProvider<PlayerLimitService> provider = instanceProvider;
		PlayerLimitService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("PlayerLimitService 未由 Spring 提供："
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
	public static void setInstanceProvider(ObjectProvider<PlayerLimitService> instanceProvider) {
		PlayerLimitService.instanceProvider = instanceProvider;
	}
}
