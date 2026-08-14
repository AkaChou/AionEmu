package com.aionemu.gameserver.network.sequrity;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 网络侧定时冲洗器：按固定间隔调度 Runnable。
 * Network-side periodic flusher scheduling runnables at a fixed interval.
 *
 * @author NB4L1
 */
@Slf4j
public final class NetFlusher {
	private static final Timer _timer = new Timer(NetFlusher.class.getName(), true);

	/**
	 * 以固定间隔调度任务（守护 Timer）。
	 * Schedules a task at a fixed interval (daemon timer).
	 *
	 * @param runnable 待调度的任务 / task to schedule
	 * @param interval 调度间隔（毫秒）/ interval in ms
	 */
	public static void add(final Runnable runnable, long interval) {
		_timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} catch (RuntimeException e) {
					log.error(I18n.get("log.009023e8c909", e));
				}
			}
		}, interval, interval);
	}
}
