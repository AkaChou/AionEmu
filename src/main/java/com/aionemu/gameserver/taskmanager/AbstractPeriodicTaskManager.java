package com.aionemu.gameserver.taskmanager;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.taskmanager.AbstractLockManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.GameServer.StartupHook;

import java.util.concurrent.Future;

/**
 * 周期性任务管理器抽象基类：服务器启动后按固定间隔调度 {@link #run()}。
 * Abstract base for periodic task managers: schedules {@link #run()} at a fixed interval after server startup.
 *
 * <p>基于 l2j-free 引擎思路。/ Based on l2j-free engines.</p>
 *
 * @author lord_rex, MrPoke
 */
@Slf4j(access = AccessLevel.PROTECTED)
public abstract class AbstractPeriodicTaskManager extends AbstractLockManager implements Runnable, StartupHook {

	/**
	 * 调度周期（毫秒）。
	 * Scheduling period in milliseconds.
	 */
	private int period;
	private Future<?> task;

	/**
	 * 以给定周期构造管理器，并注册到游戏服启动钩子。
	 * Construct with the given period and register as a game-server startup hook.
	 *
	 * @param period 周期毫秒数 / Period in milliseconds
	 */
	public AbstractPeriodicTaskManager(int period) {
		this.period = period;

		GameServer.addStartupHook(this);

		log.info(I18n.get("log.bbaa9ed5272c", getClass().getSimpleName()));
	}

	/**
	 * 启动完成后以随机偏移安排固定周期执行。
	 * After startup, schedule fixed-rate execution with a randomized offset.
	 */
	@Override
	public final synchronized void onStartup() {
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(this, 1000 + Rnd.get(period),
				Rnd.get(period - 5, period + 5));
	}

	public final synchronized void reschedule(int period) {
		if (task != null) {
			task.cancel(false);
		}
		this.period = period;
		onStartup();
	}

	/**
	 * 每个调度周期执行一次的任务体，由子类实现。
	 * Task body invoked once per schedule tick; implemented by subclasses.
	 */
	@Override
	public abstract void run();
}
