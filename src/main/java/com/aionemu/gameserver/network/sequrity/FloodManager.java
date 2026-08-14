package com.aionemu.gameserver.network.sequrity;


import com.aionemu.boot.i18n.I18n;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

/**
 * 连接/请求洪泛管理器：按 key 统计 tick 内次数，给出接受/警告/拒绝。
 * Connection/request flood manager: counts per-key ticks and returns accept/warn/reject.
 *
 * @author NB4L1
 */
@Slf4j
public final class FloodManager {
	/**
	 * 错误模式分类（预留）。
	 * Error mode categories (reserved).
	 */
	public static enum ErrorMode {
		INVALID_OPCODE, BUFFER_UNDER_FLOW, BUFFER_OVER_FLOW, FAILED_READING, FAILED_RUNNING;
	}

	/**
	 * 洪泛过滤阈值：警告上限、拒绝上限、统计 tick 数。
	 * Flood filter thresholds: warn limit, reject limit, tick window.
	 */
	public static final class FloodFilter {
		private final int _warnLimit;
		private final int _rejectLimit;
		private final int _tickLimit;

		/**
		 * @param warnLimit 警告上限 / warn limit
		 * @param rejectLimit 拒绝上限 / reject limit
		 * @param tickLimit 时间窗口 / tick window
		 */
		public FloodFilter(final int warnLimit, final int rejectLimit, final int tickLimit) {
			_warnLimit = warnLimit;
			_rejectLimit = rejectLimit;
			_tickLimit = tickLimit;
		}

		/**
		 * @return 拒绝上限 / reject limit
		 */
		public int getRejectLimit() {
			return _rejectLimit;
		}

		/**
		 * @return 时间窗口 / tick window
		 */
		public int getTickLimit() {
			return _tickLimit;
		}

		/**
		 * @return 警告上限 / warn limit
		 */
		public int getWarnLimit() {
			return _warnLimit;
		}
	}

	/**
	 * 单个 key 的 tick 计数日志。
	 * Per-key tick count log entry.
	 */
	private final class LogEntry {
		private final short[] _ticks = new short[_tickAmount];

		private int _lastTick = getCurrentTick();

		/**
		 * @return 当前 tick 索引 / current tick index
		 */
		public int getCurrentTick() {
			return (int) ((System.currentTimeMillis() - ZERO) / _tickLength);
		}

		/**
		 * 是否仍活跃（近期有活动）。
		 * Whether still active (recent activity).
		 *
		 * @return 若 active 则为 true / true if active
		 */
		public boolean isActive() {
			return getCurrentTick() - _lastTick < _tickAmount * 10;
		}

		/**
		 * 判断当前是否洪泛，可选递增当前 tick 计数。
		 * Whether currently flooding; optionally increments current tick count.
		 *
		 * @param increment 是否递增计数 / whether to increment
		 * @return 判定结果 / flood result
		 */
		public Result isFlooding(final boolean increment) {
			final int currentTick = getCurrentTick();

			if (currentTick - _lastTick >= _ticks.length) {
				_lastTick = currentTick;
				Arrays.fill(_ticks, (short) 0);
			} else if (_lastTick > currentTick) {
				log.warn(I18n.get("log.fa8be18a9ad5", currentTick, _lastTick,
						new IllegalStateException()));
				_lastTick = currentTick;
			} else
				while (currentTick != _lastTick) {
					_lastTick++;
					_ticks[_lastTick % _ticks.length] = 0;
				}

			if (increment) {
				_ticks[_lastTick % _ticks.length]++;
			}
			for (FloodFilter filter : _filters) {
				int previousSum = 0;
				int currentSum = 0;

				for (int i = 0; i <= filter.getTickLimit(); i++) {
					int value = _ticks[(_lastTick - i) % _ticks.length];

					if (i != 0) {
						previousSum += value;
					}
					if (i != filter.getTickLimit()) {
						currentSum += value;
					}
				}

				if (previousSum > filter.getRejectLimit() || currentSum > filter.getRejectLimit()) {
					return Result.REJECTED;
				}
				if (previousSum > filter.getWarnLimit() || currentSum > filter.getWarnLimit()) {
					return Result.WARNED;
				}
			}
			return Result.ACCEPTED;
		}
	}

	/**
	 * 洪泛判定结果。
	 * Flood evaluation result.
	 */
	public static enum Result {
		ACCEPTED, WARNED, REJECTED;

		/**
		 * 取更严重的结果。
		 * Returns the more severe of two results.
		 *
		 * @param r1 结果 1 / result 1
		 * @param r2 结果 2 / result 2
		 * @return 更严重的结果 / the more severe result
		 */
		public static Result max(final Result r1, final Result r2) {
			if (r1.ordinal() > r2.ordinal()) {
				return r1;
			}
			return r2;
		}
	}

	private static final long ZERO = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

	private final Map<String, LogEntry> _entries = new HashMap<String, LogEntry>();
	private final ReentrantLock _lock = new ReentrantLock();

	private final int _tickLength;

	private final int _tickAmount;

	private final FloodFilter[] _filters;

	/**
	 * 构造洪泛管理器，并注册定时清理任务。
	 * Constructs the flood manager and registers a periodic flush task.
	 *
	 * @param msecPerTick 每个 tick 的毫秒数 / milliseconds per tick
	 * @param filters 过滤器列表 / flood filters
	 */
	public FloodManager(final int msecPerTick, final FloodFilter... filters) {
		_tickLength = msecPerTick;
		_filters = filters;

		int max = 1;

		for (FloodFilter filter : _filters) {
			max = Math.max(filter.getTickLimit() + 1, max);
		}
		_tickAmount = max;

		NetFlusher.add(new Runnable() {
			@Override
			public void run() {
				flush();
			}
		}, 60000);
	}

	/**
	 * 清理长期不活跃的日志条目。
	 * Removes long-inactive log entries.
	 */
	private void flush() {
		_lock.lock();
		try {
			for (Iterator<LogEntry> it = _entries.values().iterator(); it.hasNext();) {
				if (it.next().isActive()) {
					continue;
				}
				it.remove();
			}
		} finally {
			_lock.unlock();
		}
	}

	/**
	 * 判断指定 key 是否洪泛。
	 * Whether the given key is flooding.
	 *
	 * @param key 统计键（如 IP） / key (e.g. IP)
	 * @param increment 是否递增计数 / whether to increment
	 * @return 判定结果 / flood result
	 */
	public Result isFlooding(final String key, final boolean increment) {
		if (key == null || key.isEmpty()) {
			return Result.REJECTED;
		}
		_lock.lock();
		try {
			LogEntry entry = _entries.get(key);
			if (entry == null) {
				entry = new LogEntry();
				_entries.put(key, entry);
			}
			return entry.isFlooding(increment);

		} finally {
			_lock.unlock();
		}
	}
}
