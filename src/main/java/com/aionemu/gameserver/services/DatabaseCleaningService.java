package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.CleaningConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.player.PlayerService;

/**
 * 数据库清理服务，按配置周期多线程删除长期不活跃角色。
 * Database cleaning service that multi-thread-deletes long-inactive characters by configured period.
 */
@Slf4j
public class DatabaseCleaningService {

	private static volatile ObjectProvider<DatabaseCleaningService> instanceProvider;
	/** 玩家 DAO / Player DAO */
	private PlayerDAO dao = (PlayerDAO) DAOManager.getDAO(PlayerDAO.class);

	/** 安全最小清理周期（天）。 / Security minimum cleaning period in days. */
	private final int SECURITY_MINIMUM_PERIOD = 30;

	/** 工作线程监控轮询间隔（毫秒）。 / Worker monitoring poll interval in ms. */
	private final int WORKER_CHECK_TIME = 10000;

	/** 清理工作线程列表。 / Cleaning worker list. */
	private List<Worker> workers;
	/** 清理开始时间戳。 / Cleaning start timestamp. */
	private long startTime;

	/**
	 * 构造服务；若启用清理则立即执行。
	 * Constructs the service and runs cleaning when enabled.
	 */
	public DatabaseCleaningService() {
		if (CleaningConfig.CLEANING_ENABLE) {
			runCleaning();
		}
	}

	/**
	 * 执行清理：校验周期后分发到工作线程并监控进度。
	 * Runs cleaning: validates period, delegates to workers and monitors progress.
	 */
	private void runCleaning() {
		log.info(I18n.get("log.c7f9fa919143"));
		startTime = System.currentTimeMillis();

		int periodInDays = CleaningConfig.CLEANING_PERIOD;

		if (periodInDays > SECURITY_MINIMUM_PERIOD) {
			delegateToThreads(CleaningConfig.CLEANING_THREADS,
					dao.getPlayersToDelete(periodInDays, CleaningConfig.CLEANING_LIMIT));
			monitoringProcess();
		} else {
			log.warn(I18n.get("log.d2c5a329849e"));
		}
	}

	/**
	 * 阻塞监控所有工作线程直至完成。
	 * Blocks while monitoring workers until all finish.
	 */
	private void monitoringProcess() {
		while (!allWorkersReady())
			try {
				Thread.sleep(WORKER_CHECK_TIME);
				log.info(I18n.get("log.4d9993c27cf4", currentlyDeletedChars(), (System.currentTimeMillis() - startTime) / 1000L));
			} catch (InterruptedException ex) {
				log.error(I18n.get("log.5460b157e46e"));
			}
	}

	/**
	 * 判断所有工作线程是否已完成。
	 * Returns whether all workers are ready.
	 *
	 * @return 全部完成返回 true / true if all ready
	 */
	private boolean allWorkersReady() {
		for (Worker w : workers) {
			if (!w.ready.get()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 汇总当前已删除角色数量。
	 * Returns the total number of characters deleted so far.
	 *
	 * @return 已删除数量 / deleted count
	 */
	private int currentlyDeletedChars() {
		int deletedChars = 0;
		for (Worker w : workers) {
			deletedChars += w.deletedChars.get();
		}
		return deletedChars;
	}

	/**
	 * 将待删除角色 ID 均分到多个工作线程执行。
	 * Distributes player ids to delete across worker threads.
	 *
	 * thread count
	 * @param idsToDelegate 待删除角色 ID / player ids to delete
	 */
	private void delegateToThreads(int numberOfThreads, List<Integer> idsToDelegate) {
		workers = new ArrayList<Worker>();
		log.info(I18n.get("log.12a06ef31fa4", numberOfThreads));

		int itr = 0;
		for (int workerNo = 0; itr < idsToDelegate.size(); workerNo %= numberOfThreads) {
			if (workerNo >= workers.size()) {
				workers.add(new Worker());
			}
			workers.get(workerNo).ids.add(idsToDelegate.get(itr));

			itr++;
			workerNo++;
		}

		for (Worker w : workers) {
			GameThreadPoolServices.threadPoolManager().executeLongRunning(w);
		}
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static DatabaseCleaningService getInstance() {
		ObjectProvider<DatabaseCleaningService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<DatabaseCleaningService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		private static final DatabaseCleaningService instance = new DatabaseCleaningService();
	}

	/**
	 * 清理工作线程：删除分配的角色 ID。
	 * Cleaning worker that deletes assigned player ids.
	 */
	private class Worker implements Runnable {

		private final List<Integer> ids = new ArrayList<Integer>();
		private final AtomicInteger deletedChars = new AtomicInteger();
		private final AtomicBoolean ready = new AtomicBoolean();

		private Worker() {
		}

		public void run() {
			for (int id : ids) {
				PlayerService.deletePlayerFromDB(id);
				deletedChars.incrementAndGet();
			}
			ready.set(true);
		}
	}
}
