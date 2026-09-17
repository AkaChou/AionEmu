package com.aionemu.gameserver.taskmanager.tasks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.taskmanager.AbstractPeriodicTaskManager;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 临时交易窗口倒计时任务：管理可临时交易物品的剩余时间与提示。
 * Temporary-trade window countdown task: tracks remaining time and warnings for temporarily tradable items.
 *
 * @author Mr. Poke
 */
public class TemporaryTradeTimeTask extends AbstractPeriodicTaskManager {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<TemporaryTradeTimeTask> instanceProvider;

	/**
	 * 物品到可交易玩家 Id 集合的映射。
	 * Map of items to allowed trader player ids.
	 */
	private final Map<Item, Collection<Integer>> items = new HashMap<Item, Collection<Integer>>();

	/**
	 * 物品 objectId 到物品的索引。
	 * Index of item objectId to item.
	 */
	private final Map<Integer, Item> itemById = new HashMap<Integer, Item>();

	/**
	 * 以 1 秒周期构造临时交易计时任务。
	 * Construct the temporary-trade timer with a 1-second period.
	 */
	public TemporaryTradeTimeTask() {
		super(1000);
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
	public static TemporaryTradeTimeTask getInstance() {
		ObjectProvider<TemporaryTradeTimeTask> provider = instanceProvider;
		TemporaryTradeTimeTask provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("TemporaryTradeTimeTask 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Provider
	 */
	public static void setInstanceProvider(ObjectProvider<TemporaryTradeTimeTask> provider) {
		instanceProvider = provider;
	}

	/**
	 * 注册临时可交易物品及其允许的玩家列表。
	 * Register a temporarily tradable item and its allowed player list.
	 *
	 * @param item 物品 / Item
	 * @param players 允许交易的玩家 Id 集合 / Allowed trader player ids
	 */
	public void addTask(Item item, Collection<Integer> players) {
		writeLock();
		try {
			items.put(item, players);
			itemById.put(item.getObjectId(), item);
		} finally {
			writeUnlock();
		}
	}

	/**
	 * 判断指定玩家是否可交易该物品。
	 * Whether the given player may trade the item.
	 *
	 * @param item 物品 / Item
	 * @param playerObjectId 玩家对象 ID / Player object id
	 * @return 可交易则为 true / True if allowed
	 */
	public boolean canTrade(Item item, int playerObjectId) {
		Collection<Integer> players = items.get(item);
		if (players == null)
			return false;
		return players.contains(playerObjectId);
	}

	/**
	 * 判断物品是否仍在临时交易窗口中。
	 * Whether the item is still under a temporary-trade window.
	 *
	 * @param item 物品 / Item
	 */
	public boolean hasItem(Item item) {
		readLock();
		try {
			return items.containsKey(item);
		} finally {
			readUnlock();
		}
	}

	/**
	 * 按 objectId 查询临时交易中的物品。
	 * Look up a temporarily tradable item by objectId.
	 *
	 * @param objectId 物品对象 ID / Item object id
	 * @return 物品；不存在则为 null / Item, or null if absent
	 */
	public Item getItem(int objectId) {
		readLock();
		try {
			return itemById.get(objectId);
		} finally {
			readUnlock();
		}
	}

	/**
	 * 每秒检查剩余时间：60 秒提示、到期清除并通知玩家。
	 * Each second check remaining time: warn at 60s, clear and notify when expired.
	 */
	@Override
	public void run() {
		writeLock();
		try {
			for (Iterator<Map.Entry<Item, Collection<Integer>>> i = items.entrySet().iterator(); i.hasNext();) {
				Map.Entry<Item, Collection<Integer>> entry = i.next();
				Item item = entry.getKey();
				int time = (item.getTemporaryExchangeTime() - (int) (System.currentTimeMillis() / 1000));
				if (time == 60) {
					for (int playerId : entry.getValue()) {
						Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
						if (player != null) {
							PacketSendUtility.sendPacket(player,
									SM_SYSTEM_MESSAGE.STR_MSG_END_OF_EXCHANGE_TIME(item.getNameId(), time));
						}
					}
				} else if (time <= 0) {
					for (int playerId : entry.getValue()) {
						Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
						if (player != null) {
							PacketSendUtility.sendPacket(player,
									SM_SYSTEM_MESSAGE.STR_MSG_EXCHANGE_TIME_OVER(item.getNameId()));
						}
					}
					item.setTemporaryExchangeTime(0);
					i.remove();
					itemById.remove(item.getObjectId());
				}
			}
		} finally {
			writeUnlock();
		}
	}
}
