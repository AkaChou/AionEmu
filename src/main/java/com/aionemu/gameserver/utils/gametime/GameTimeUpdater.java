package com.aionemu.gameserver.utils.gametime;

import lombok.AllArgsConstructor;

/**
 * 负责推进游戏时钟的定时任务。
 * Runnable responsible for advancing the game clock.
 *
 * @author Ben
 */
@AllArgsConstructor
public class GameTimeUpdater implements Runnable {

	/**
	 * 待推进的游戏时间。
	 * Game time to advance.
	 */
	private final GameTime time;

	/**
	 * 将游戏时间增加一分钟。
	 * Increase game time by one minute.
	 */
	@Override
	public void run() {
		time.increase();
	}
}
