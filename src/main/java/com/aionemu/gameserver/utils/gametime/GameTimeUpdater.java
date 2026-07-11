package com.aionemu.gameserver.utils.gametime;

/**
 * 负责推进游戏时钟的定时任务。
 * Runnable responsible for advancing the game clock.
 *
 * @author Ben
 */
public class GameTimeUpdater implements Runnable {

	/**
	 * 待推进的游戏时间。
	 * Game time to advance.
	 */
	private GameTime time;

	/**
	 * 构造用于推进指定 GameTime 的更新器。
	 * Construct an updater for the given GameTime.
	 *
	 * @param time 要更新的游戏时间 / GameTime to update
	 */
	public GameTimeUpdater(GameTime time) {
		this.time = time;
	}

	/**
	 * 将游戏时间增加一分钟。
	 * Increase game time by one minute.
	 */
	@Override
	public void run() {
		time.increase();
	}
}
