package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Iterator;
import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.PeriodicSaveConfig;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.services.player.PlayerEnterWorldService;
import com.aionemu.gameserver.services.toypet.PetSpawnService;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 定期持久化服务，调度军团仓库等数据的周期保存。
 * Periodic save service scheduling periodic persistence of legion warehouse data, etc.
 *
 * @author ATracer
 */
@Slf4j
public class PeriodicSaveService {

	private static volatile ObjectProvider<PeriodicSaveService> instanceProvider;

	private Future<?> legionWhUpdateTask;

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static final PeriodicSaveService getInstance() {
		ObjectProvider<PeriodicSaveService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
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
	 * 构造并启动军团仓库周期保存任务。
	 * Constructs and starts the legion warehouse periodic save task.
	 */
	public PeriodicSaveService() {
		rescheduleLegionTask();
	}

	/**
	 * 重载周期保存配置并重置在线玩家相关任务。
	 * Reloads periodic-save config and reschedules online player-related tasks.
	 */
	public synchronized void reload() {
		legionWhUpdateTask.cancel(false);
		rescheduleLegionTask();
		for (Player player : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
			PlayerEnterWorldService.reschedulePeriodicSaveTasks(player);
			if (player.getPet() != null) {
				PetSpawnService.reschedulePeriodicSaveTask(player);
			}
		}
	}

	private void rescheduleLegionTask() {
		int delay = PeriodicSaveConfig.LEGION_ITEMS * 1000;
		legionWhUpdateTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new LegionWhUpdateTask(), delay, delay);
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
					/**
	 * 1. 先保存物品 / 1. save items first
	 */
					DAOManager.getDAO(InventoryDAO.class).store(allItems, null, null, legion.getLegionId());

					/**
	 * 2. 保存物品镶嵌石 / 2. save item stones
	 */
					DAOManager.getDAO(ItemStoneListDAO.class).save(allItems);
				} catch (Exception ex) {
					log.error(I18n.get("log.ea0f9e89569d", ex));
				}
				legionWhUpdated++;
			}
			long workTime = System.currentTimeMillis() - startTime;
			log.info(I18n.get("log.b1fef96f6a2c", workTime, legionWhUpdated));
		}
	}

		/**
	 * 关闭时立即保存待写数据。
	 * Saves pending data immediately on shutdown.
	 */
	public void onShutdown() {
		log.info(I18n.get("log.dd47b038ccfb"));
		// 保存军团仓库 / save legion warehouse
		legionWhUpdateTask.cancel(false);
		new LegionWhUpdateTask().run();
		log.info(I18n.get("log.179f3826e123"));
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final PeriodicSaveService instance = new PeriodicSaveService();
	}
}
