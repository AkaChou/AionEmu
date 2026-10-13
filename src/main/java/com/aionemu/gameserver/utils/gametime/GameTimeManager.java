package com.aionemu.gameserver.utils.gametime;

import com.aionemu.boot.i18n.I18n;

import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 游戏时间管理器：加载、启动、持久化与重载游戏时钟。
 * Game-time manager: load, start, persist and reload the in-game clock.
 */
@Slf4j
public class GameTimeManager {
	/**
	 * 当前游戏时间实例。
	 * Current game-time instance.
	 */
	private static GameTime instance;
	/**
	 * 时钟推进任务。
	 * Clock advancement task.
	 */
	private static GameTimeUpdater updater;
	/**
	 * 时钟是否已启动。
	 * Whether the clock has been started.
	 */
	private static boolean clockStarted = false;

	/** Spring 绑定的服务器变量 DAO。 / Spring-bound server-variables DAO. */
	private static volatile ServerVariableDao serverVariablesDao;

	/**
	 * 由 Spring 配置类在启动期绑定服务器变量 DAO 并加载当前游戏时间。
	 * Binds the server-variables DAO from the Spring configuration at startup and loads the current game time.
	 * @param dao 服务器变量 DAO / server-variables DAO
	 */
	public static void init(ServerVariableDao dao) {
		serverVariablesDao = java.util.Objects.requireNonNull(dao, "serverVariablesDao must not be null");
		instance = new GameTime(serverVariablesDao.load("time"));
	}

	/**
	 * 返回服务器变量 DAO；必须由 Spring 在启动期绑定，未绑定即失败。
	 * Returns the server-variables DAO; Spring must bind it at startup, otherwise it fails fast.
	 * @return 服务器变量 DAO / server-variables DAO
	 */
	private static ServerVariableDao serverVariablesDao() {
		ServerVariableDao current = serverVariablesDao;
		if (current == null) {
			throw new IllegalStateException("ServerVariableDao is not bound; Spring must bind it at startup");
		}
		return current;
	}

	/**
	 * 获取当前游戏时间。
	 * Get the current game time.
	 *
	 * @return 游戏时间实例 / GameTime instance
	 */
	public static GameTime getGameTime() {
		return instance;
	}

	/**
	 * 启动游戏时钟（每 5 秒推进一次）。
	 * Start the game clock (advances every 5 seconds).
	 */
	public static void startClock() {
		if (clockStarted) {
			throw new IllegalStateException("Clock is already started");
		}
		updater = new GameTimeUpdater(getGameTime());
		ThreadPoolManager.getInstance().scheduleAtFixedRate(updater, 0, 5000);
		clockStarted = true;
	}

	/**
	 * 将当前游戏时间写入服务器变量。
	 * Persist the current game time to server variables.
	 *
	 * @return 保存成功则为 true / True if stored
	 */
	public static void saveTime() {
		serverVariablesDao().store("time", getGameTime().getTime());
	}

	/**
	 * 以指定分钟数重载游戏时间并重启时钟。
	 * Reload game time to the given minutes value and restart the clock.
	 *
	 * @param time 自 01.01.0000 起的分钟数 / Minutes since 01.01.0000
	 */
	public static void reloadTime(int time) {
		ThreadPoolManager.getInstance().purge();
		instance = new GameTime(time);
		clockStarted = false;
		startClock();
		log.info(I18n.get("log.64527c97d567"));
	}
}
