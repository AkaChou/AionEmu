package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.PeriodicSaveConfig;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.services.player.PlayerEnterWorldService;
import com.aionemu.gameserver.services.toypet.PetSpawnService;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 定期持久化服务，集中调度军团仓库、在线玩家常规数据与背包数据的周期保存。
 * Periodic save service centrally scheduling periodic persistence of legion warehouse, player general, and item data.
 *
 * @author ATracer
 */
@Slf4j
public class PeriodicSaveService {

	private static volatile ObjectProvider<PeriodicSaveService> instanceProvider;

	private final AbyssRankDAO abyssRankDAO;
	private final PlayerSkillListDAO playerSkillListDAO;
	private final PlayerQuestListDAO playerQuestListDAO;
	private final PlayerDAO playerDAO;
	private final PlayerStigmasEquippedDAO playerStigmasEquippedDAO;
	private final InventoryDAO inventoryDAO;
	private final ItemStoneListDAO itemStoneListDAO;

	private Future<?> legionWhUpdateTask;
	private Future<?> playerGeneralUpdateTask;
	private Future<?> playerItemsUpdateTask;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static PeriodicSaveService getInstance() {
		ObjectProvider<PeriodicSaveService> provider = instanceProvider;
		PeriodicSaveService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("PeriodicSaveService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<PeriodicSaveService> instanceProvider) {
		PeriodicSaveService.instanceProvider = instanceProvider;
	}

	/**
	 * 使用指定的 DAO 集合构造并启动全局周期保存任务。
	 * Constructs the service with given DAOs and starts global periodic save tasks.
	 */
	public PeriodicSaveService(@NonNull AbyssRankDAO abyssRankDAO, @NonNull PlayerSkillListDAO playerSkillListDAO,
							   @NonNull PlayerQuestListDAO playerQuestListDAO, @NonNull PlayerDAO playerDAO,
							   @NonNull PlayerStigmasEquippedDAO playerStigmasEquippedDAO,
							   @NonNull InventoryDAO inventoryDAO, @NonNull ItemStoneListDAO itemStoneListDAO) {
		this.abyssRankDAO = abyssRankDAO;
		this.playerSkillListDAO = playerSkillListDAO;
		this.playerQuestListDAO = playerQuestListDAO;
		this.playerDAO = playerDAO;
		this.playerStigmasEquippedDAO = playerStigmasEquippedDAO;
		this.inventoryDAO = inventoryDAO;
		this.itemStoneListDAO = itemStoneListDAO;
		rescheduleAllTasks();
	}

	/**
	 * 构造并启动全局周期保存任务（军团仓库、玩家常规数据、玩家物品数据）。
	 * Constructs and starts global periodic save tasks (legion warehouse, player general, player items).
	 */
	public PeriodicSaveService() {
		this(safeDao(AbyssRankDAO.class, com.aionemu.gameserver.dao.impl.AbyssRankDAO::new),
			safeDao(PlayerSkillListDAO.class, com.aionemu.gameserver.dao.impl.PlayerSkillListDAO::new),
			safeDao(PlayerQuestListDAO.class, com.aionemu.gameserver.dao.impl.PlayerQuestListDAO::new),
			safeDao(PlayerDAO.class, com.aionemu.gameserver.dao.impl.PlayerDAO::new),
			safeDao(PlayerStigmasEquippedDAO.class, com.aionemu.gameserver.dao.impl.PlayerStigmasEquippedDAO::new),
			safeDao(InventoryDAO.class, com.aionemu.gameserver.dao.impl.InventoryDAO::new),
			safeDao(ItemStoneListDAO.class, com.aionemu.gameserver.dao.impl.ItemStoneListDAO::new));
	}

	private static <T extends com.aionemu.commons.database.dao.DAO> T safeDao(Class<T> daoClass, java.util.function.Supplier<T> fallback) {
		if (DAOManager.isInitialized()) {
			try {
				return DAOManager.getDAO(daoClass);
			} catch (Exception ignored) {
			}
		}
		return fallback.get();
	}

	/**
	 * 重载周期保存配置并重置周期任务。
	 * Reloads periodic-save config and reschedules periodic tasks.
	 */
	public synchronized void reload() {
		cancelAllTasks();
		rescheduleAllTasks();
		for (Player player : GameWorldBootstrapServices.world().getAllPlayers()) {
			PlayerEnterWorldService.reschedulePeriodicSaveTasks(player);
			if (player.getPet() != null) {
				PetSpawnService.reschedulePeriodicSaveTask(player);
			}
		}
	}

	/**
	 * 重新调度全部全局存盘任务。
	 * Reschedules all global periodic save tasks.
	 */
	private void rescheduleAllTasks() {
		rescheduleLegionTask();
		reschedulePlayerGeneralTask();
		reschedulePlayerItemsTask();
	}

	/**
	 * 取消全部当前正在运行的全局存盘任务。
	 * Cancels all currently running global periodic save tasks.
	 */
	private void cancelAllTasks() {
		if (legionWhUpdateTask != null) {
			legionWhUpdateTask.cancel(false);
			legionWhUpdateTask = null;
		}
		if (playerGeneralUpdateTask != null) {
			playerGeneralUpdateTask.cancel(false);
			playerGeneralUpdateTask = null;
		}
		if (playerItemsUpdateTask != null) {
			playerItemsUpdateTask.cancel(false);
			playerItemsUpdateTask = null;
		}
	}

	private void rescheduleLegionTask() {
		int delay = PeriodicSaveConfig.LEGION_ITEMS * 1000;
		if (delay > 0) {
			legionWhUpdateTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new LegionWhUpdateTask(), delay, delay);
		}
	}

	private void reschedulePlayerGeneralTask() {
		int delay = PeriodicSaveConfig.PLAYER_GENERAL * 1000;
		if (delay > 0) {
			playerGeneralUpdateTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new PlayerGeneralUpdateTask(), delay, delay);
		}
	}

	private void reschedulePlayerItemsTask() {
		int delay = PeriodicSaveConfig.PLAYER_ITEMS * 1000;
		if (delay > 0) {
			playerItemsUpdateTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new PlayerItemsUpdateTask(), delay, delay);
		}
	}

	/**
	 * 持久化指定玩家的常规数据（军衔、技能、任务、人物基础、烙印与房屋）。
	 * Persists general data of the specified player (abyss rank, skills, quests, player, stigmas, and houses).
	 *
	 * @param player 目标玩家 / target player
	 */
	public void savePlayerGeneral(Player player) {
		if (player == null) {
			return;
		}
		try {
			this.abyssRankDAO.storeAbyssRank(player);
			this.playerSkillListDAO.storeSkills(player);
			this.playerQuestListDAO.store(player);
			this.playerDAO.storePlayer(player);
			this.playerStigmasEquippedDAO.storeItems(player);
			List<House> houses = player.getHouses();
			if (houses != null) {
				for (House house : houses) {
					house.save();
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.fe1aeacc963e", player.getName()), ex);
		}
	}

	/**
	 * 持久化指定玩家的物品数据（背包与魔石神石）。
	 * Persists item data of the specified player (inventory and item stones).
	 *
	 * @param player 目标玩家 / target player
	 */
	public void savePlayerItems(Player player) {
		if (player == null) {
			return;
		}
		try {
			this.inventoryDAO.store(player);
			this.itemStoneListDAO.save(player);
		} catch (Exception ex) {
			log.error(I18n.get("log.7850b77ba785", player.getName()), ex);
		}
	}

	private class LegionWhUpdateTask implements Runnable {

		@Override
		public void run() {
			log.info(I18n.get("log.cda6066cb95d"));
			long startTime = System.currentTimeMillis();
			Iterator<Legion> legionsIterator = GameCoreGameplayServices.legionService().getCachedLegionIterator();
			int legionWhUpdated = 0;
			while (legionsIterator.hasNext()) {
				Legion legion = legionsIterator.next();
				List<Item> allItems = legion.getLegionWarehouse().getItemsWithKinah();
				allItems.addAll(legion.getLegionWarehouse().getDeletedItems());
				try {
					inventoryDAO.store(allItems, null, null, legion.getLegionId());
					itemStoneListDAO.save(allItems);
				} catch (Exception ex) {
					log.error(I18n.get("log.ea0f9e89569d"), ex);
				}
				legionWhUpdated++;
			}
			long workTime = System.currentTimeMillis() - startTime;
			log.info(I18n.get("log.b1fef96f6a2c", workTime, legionWhUpdated));
		}
	}

	private class PlayerGeneralUpdateTask implements Runnable {

		@Override
		public void run() {
			long startTime = System.currentTimeMillis();
			int playersUpdated = 0;
			Iterator<Player> playersIterator = GameWorldBootstrapServices.world().getPlayersIterator();
			while (playersIterator.hasNext()) {
				Player player = playersIterator.next();
				if (player != null && player.isOnline()) {
					savePlayerGeneral(player);
					playersUpdated++;
				}
			}
			long workTime = System.currentTimeMillis() - startTime;
			log.info(I18n.get("log.8e83f631917d", workTime, playersUpdated));
		}
	}

	private class PlayerItemsUpdateTask implements Runnable {

		@Override
		public void run() {
			long startTime = System.currentTimeMillis();
			int playersUpdated = 0;
			Iterator<Player> playersIterator = GameWorldBootstrapServices.world().getPlayersIterator();
			while (playersIterator.hasNext()) {
				Player player = playersIterator.next();
				if (player != null && player.isOnline()) {
					savePlayerItems(player);
					playersUpdated++;
				}
			}
			long workTime = System.currentTimeMillis() - startTime;
			log.info(I18n.get("log.b8e76df845b1", workTime, playersUpdated));
		}
	}

	/**
	 * 关闭时立即保存待写数据并取消周期任务。
	 * Saves pending data immediately on shutdown and cancels periodic tasks.
	 */
	public void onShutdown() {
		log.info(I18n.get("log.dd47b038ccfb"));
		cancelAllTasks();
		// 保存军团仓库 / save legion warehouse
		new LegionWhUpdateTask().run();
		log.info(I18n.get("log.179f3826e123"));
	}
}
