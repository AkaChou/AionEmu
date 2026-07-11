package com.aionemu.gameserver.taskmanager.tasks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.taskmanager.AbstractPeriodicTaskManager;

/**
 * 可过期对象倒计时任务：每秒检查并触发到期消息/结束逻辑。
 * Countdown task for expirable objects: each second checks and fires expire messages/end logic.
 */
public class ExpireTimerTask extends AbstractPeriodicTaskManager {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<ExpireTimerTask> instanceProvider;

	/**
	 * 可过期对象到所属玩家的映射。
	 * Map of expirable objects to their owning players.
	 */
	private Map<IExpirable, Player> expirables = new HashMap<IExpirable, Player>();

	/**
	 * 以 1 秒周期构造过期计时任务。
	 * Construct the expire timer with a 1-second period.
	 */
	public ExpireTimerTask() {
		super(1000);
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则静态 holder。
	 * Get the singleton: prefer Spring provider, otherwise the static holder.
	 *
	 * Task instance
	 */
	public static ExpireTimerTask getInstance() {
		ObjectProvider<ExpireTimerTask> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder._instance);
		}
		return SingletonHolder._instance;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * Provider
	 */
	public static void setInstanceProvider(ObjectProvider<ExpireTimerTask> provider) {
		instanceProvider = provider;
	}

	/**
	 * 注册可过期对象及其所属玩家。
	 * Register an expirable object and its owning player.
	 *
	 * @param expirable 可过期对象 / Expirable object
	 * 所属玩家 / Owning player
	 */
	public void addTask(IExpirable expirable, Player player) {
		writeLock();
		try {
			expirables.put(expirable, player);
		} finally {
			writeUnlock();
		}
	}

	/**
	 * 移除某玩家相关的全部可过期条目。
	 * Remove all expirable entries belonging to a player.
	 *
	 * @param player 玩家 / Player
	 */
	public void removePlayer(Player player) {
		writeLock();
		try {
			for (Iterator<Map.Entry<IExpirable, Player>> i = expirables.entrySet().iterator(); i.hasNext();) {
				Map.Entry<IExpirable, Player> entry = i.next();
				if (entry.getValue() == player) {
					i.remove();
				}
			}
		} finally {
			writeUnlock();
		}
	}

	/**
	 * 检查到期：可结束则 {@code expireEnd}，并在关键倒计时点发送消息。
	 * Check expiry: call {@code expireEnd} when due, and send messages at key countdown points.
	 */
	@Override
	public void run() {
		writeLock();
		try {
			int timeNow = (int) (System.currentTimeMillis() / 1000);
			for (Iterator<Map.Entry<IExpirable, Player>> i = expirables.entrySet().iterator(); i.hasNext();) {
				Map.Entry<IExpirable, Player> entry = i.next();
				IExpirable expirable = entry.getKey();
				Player player = entry.getValue();
				int min = (expirable.getExpireTime() - timeNow);
				if (min < 0 && expirable.canExpireNow()) {
					expirable.expireEnd(player);
					i.remove();
					continue;
				}
				switch (min) {
				case 1800:
				case 900:
				case 600:
				case 300:
				case 60:
					expirable.expireMessage(player, min / 60);
					break;
				}
			}
		} finally {
			writeUnlock();
		}
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		/**
		 * 默认单例实例。
		 * Default singleton instance.
		 */
		protected static final ExpireTimerTask _instance = new ExpireTimerTask();
	}
}
