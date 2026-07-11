package com.aionemu.gameserver.utils.gametime;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.ServerVariablesDAO;
import lombok.extern.slf4j.Slf4j;

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

	static {
		ServerVariablesDAO dao = DAOManager.getDAO(ServerVariablesDAO.class);
		instance = new GameTime(dao.load("time"));
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
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(updater, 0, 5000);
		clockStarted = true;
	}

	/**
	 * 将当前游戏时间写入服务器变量。
	 * Persist the current game time to server variables.
	 *
	 * @return 保存成功则为 true / True if stored
	 */
	public static boolean saveTime() {
		return DAOManager.getDAO(ServerVariablesDAO.class).store("time", getGameTime().getTime());
	}

	/**
	 * 以指定分钟数重载游戏时间并重启时钟。
	 * Reload game time to the given minutes value and restart the clock.
	 *
	 * @param time 自 01.01.0000 起的分钟数 / Minutes since 01.01.0000
	 */
	public static void reloadTime(int time) {
		GameThreadPoolServices.threadPoolManager().purge();
		instance = new GameTime(time);
		clockStarted = false;
		startClock();
		log.info(I18n.get("log.64527c97d567"));
	}
}
