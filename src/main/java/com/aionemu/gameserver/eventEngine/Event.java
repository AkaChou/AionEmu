package com.aionemu.gameserver.eventEngine;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 游戏事件抽象基类：支持优先级、冷却、广播与重置。
 * Abstract game event base with priority, cooldown, announce helpers and reset.
 *
 * @author wanke
 */
public abstract class Event implements Runnable {

	/** 最高优先级 / Maximum priority. */
	public static final int MAX_PRIORITY = 10;
	/** 最低优先级 / Minimum priority. */
	public static final int MIN_PRIORITY = 0;
	/** 默认优先级 / Default priority. */
	public static final int DEFAULT_PRIORITY = 5;

	/**
	 * 当前优先级（越大越优先）。
	 * Current priority (higher runs sooner).
	 */
	private int priority = DEFAULT_PRIORITY;

	/**
	 * 事件是否已结束。
	 * Whether the event has finished.
	 */
	private boolean finished = false;

	/**
	 * 调度入口：执行事件主体。
	 * Scheduler entry: runs the event body.
	 */
	public final void run() {
		execute();
	}

	/**
	 * 事件主体逻辑，由子类实现。
	 * Event body implemented by subclasses.
	 */
	abstract protected void execute();

	/**
	 * 重置完成标记并回调 {@link #onReset()}。
	 * Clears the finished flag and invokes {@link #onReset()}.
	 */
	public final void reset() {
		finished = false;
		onReset();
	}

	/**
	 * 重置子类状态。
	 * Resets subclass-specific state.
	 */
	abstract protected void onReset();

	/**
	 * 标记事件完成。
	 * Marks the event as finished.
	 */
	protected void finish() {
		finished = true;
	}

	/**
	 * 尝试取消事件。
	 * Attempts to cancel the event.
	 *
	 * @param mayInterruptIfRunning 是否允许中断运行中任务 / whether to interrupt if running
	 * @return 是否取消成功 / whether cancel succeeded
	 */
	public abstract boolean cancel(boolean mayInterruptIfRunning);

	/**
	 * 事件冷却时间（毫秒），默认 30 秒。
	 * Cooldown in millis after finish; default 30 seconds.
	 *
	 * cooldown millis
	 */
	public int getCooldown() {
		return 30 * 1000;
	}

	/**
	 * 获取优先级。
	 * Returns the priority.
	 *
	 * priority
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * 设置优先级，自动钳制在 {@link #MIN_PRIORITY}～{@link #MAX_PRIORITY}。
	 * Sets priority, clamped to {@link #MIN_PRIORITY}..{@link #MAX_PRIORITY}.
	 *
	 * @param priority 目标优先级 / desired priority
	 */
	public void setPriority(int priority) {
		if (priority > MAX_PRIORITY) {
			priority = MAX_PRIORITY;
		}
		if (priority < MIN_PRIORITY) {
			priority = MIN_PRIORITY;
		}
		this.priority = priority;
	}

	/**
	 * 事件是否已结束。
	 * Whether the event has finished.
	 *
	 * finished flag
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * 向玩家发送事件公告。
	 * Sends an event announce to a player.
	 *
	 * @param pl 玩家 / player
	 * message
	 */
	protected void announce(Player pl, String msg) {
		announce(pl, msg, 0);
	}

	/**
	 * 向玩家集合发送事件公告。
	 * Sends an event announce to a player collection.
	 *
	 * players
	 * message
	 */
	protected void announce(Collection<Player> players, String msg) {
		for (Player pl : players) {
			announce(pl, msg, 0);
		}
	}

	/**
	 * 向玩家发送事件公告，可延迟。
	 * Sends an event announce to a player, optionally delayed.
	 *
	 * @param pl 玩家 / player
	 * message
	 * @param delay 延迟毫秒，0 表示立即 / delay millis, 0 means immediate
	 */
	protected void announce(final Player pl, final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					PacketSendUtility.sendSys3Message(pl, "Event", msg);
				}
			}, delay);
		} else {
			PacketSendUtility.sendSys3Message(pl, "Event", msg);
		}
	}

	/**
	 * 向全服（非战场）玩家广播事件公告。
	 * Broadcasts an event announce to all non-battleground players.
	 *
	 * message
	 */
	protected void announceAll(String msg) {
		announceAll(msg, 0);
	}

	/**
	 * 向全服（非战场）玩家广播事件公告，可延迟。
	 * Broadcasts an event announce to all non-battleground players, optionally delayed.
	 *
	 * message
	 * @param delay 延迟毫秒，0 表示立即 / delay millis, 0 means immediate
	 */
	protected void announceAll(final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player pl) {
							if (pl.getBattleground() == null) {
								PacketSendUtility.sendSys3Message(pl, "Event", msg);
							}
						}
					});
				}
			}, delay);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player pl) {
					if (pl.getBattleground() == null) {
						PacketSendUtility.sendSys3Message(pl, "Event", msg);
					}
				}
			});
		}
	}
}
