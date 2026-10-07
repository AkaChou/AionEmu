package com.aionemu.gameserver.model.gameobjects.player;

import java.util.Arrays;

/**
 * 玩家「重复异常状态递减链」运行时状态（内存态，不持久化）。
 * Per-player repeated-abnormal decay chain state (in-memory only, never persisted).
 * <p>该类型只服务 {@link Player}：按数据表位置记录每种受控状态（真端表仅 PARALYZE / SLEEP /
 * FEAR）的累积命中步数与上次命中时刻；数组首次写入才分配，未记录、从未命中或已出窗口
 * 一律返回 0（视为全新链），无需定时任务归零——陈旧时间戳会让链自然失效。
 * This type only serves {@link Player}: it stores, per table position, the accumulated hit step
 * and last-hit timestamp of each tracked state (only PARALYZE / SLEEP / FEAR in the retail table).
 * The backing arrays stay {@code null} until the first write; an absent, never-hit or
 * window-expired record reads as step 0 (a fresh chain), so no timer-based reset is needed.</p>
 * <p>步数语义 / Step semantics：{@code 1..5} 为已成功施加的次数（{@link #MAX_STEP} 封顶），
 * 读取端用第 N 档（time_value[N]% / resist_value[N]）；读-写跨调用非原子，与技能引擎现状一致。
 * {@code 1..5} counts successful applications (capped by {@link #MAX_STEP}); the resist phase
 * reads tier N. The read-modify-write across calls is not atomic, matching the engine's reality.</p>
 */
final class PlayerRepeatedAbnormalStatus {

	/** 真端步数上限：档位 1..5 读取，5 为封顶 / Retail step ceiling: tiers 1..5, capped at 5. */
	static final int MAX_STEP = 5;

	/** 各状态的累积步数，索引 = 数据表位置 / accumulated steps indexed by table position. */
	private byte[] steps;
	/** 各状态的上次命中时刻（epoch 毫秒，0 = 从未命中）/ last-hit epoch millis per state, 0 = never. */
	private long[] lastHitTimes;

	/**
	 * 读取当前有效步数：无记录、从未命中或已出窗口（闭区间）时为 0。
	 * Returns the effective step; 0 when absent, never hit, or window-expired (boundary inclusive).
	 * @param index 数据表位置 / table position
	 * @param now 当前时刻（毫秒）/ current time in millis
	 * @param windowMillis 命中窗口（毫秒）/ hit window in millis
	 * @return 有效步数 0..5 / effective step 0..5
	 */
	synchronized int currentStep(int index, long now, long windowMillis) {
		if (steps == null || index < 0 || index >= steps.length) {
			return 0;
		}
		long last = lastHitTimes[index];
		if (last == 0 || now - last > windowMillis) {
			return 0;
		}
		return steps[index];
	}

	/**
	 * 记录一次成功施加：步数写入 {@code 1..MAX_STEP}，并刷新命中时刻。
	 * Records one successful application: the step is stored in {@code 1..MAX_STEP} and the timestamp refreshed.
	 * @param index 数据表位置 / table position
	 * @param nextStep 期望的新步数（由读取端 step+1 传入）/ desired new step (read step + 1)
	 * @param now 施加时刻（毫秒）/ application time in millis
	 */
	synchronized void record(int index, int nextStep, long now) {
		if (index < 0) {
			return;
		}
		ensureCapacity(index);
		steps[index] = (byte) Math.min(MAX_STEP, Math.max(1, nextStep));
		lastHitTimes[index] = now;
	}

	/**
	 * 清空全部链状态并释放数组。
	 * Clears all chain state and releases the arrays.
	 */
	synchronized void clear() {
		steps = null;
		lastHitTimes = null;
	}

	/**
	 * 返回是否已分配过链数组（测试断言惰性分配用）。
	 * Returns whether the chain arrays have been allocated (asserted by tests).
	 * @return 是否已分配 / whether allocated
	 */
	synchronized boolean isTracked() {
		return steps != null;
	}

	private void ensureCapacity(int index) {
		if (steps != null && index < steps.length) {
			return;
		}
		int capacity = Math.max(index + 1, steps == null ? 0 : steps.length);
		steps = steps == null ? new byte[capacity] : Arrays.copyOf(steps, capacity);
		lastHitTimes = lastHitTimes == null ? new long[capacity] : Arrays.copyOf(lastHitTimes, capacity);
	}
}
