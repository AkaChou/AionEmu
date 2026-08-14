package com.aionemu.gameserver.skillengine.task;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 交互任务基类：以固定间隔驱动玩家与目标的交互流程。
 * Base interaction task: drives player-target interaction on a fixed schedule.
 */
public abstract class AbstractInteractionTask {

	/**
	 * 周期调度任务句柄。
	 * Scheduled tick task handle.
	 */
	protected Future<?> task;

	/**
	 * 发起交互的玩家。
	 * Player who started the interaction.
	 */
	protected Player requestor;

	/**
	 * 交互响应方（目标对象）。
	 * Interaction responder (target object).
	 */
	protected VisibleObject responder;

	/**
	 * 构造交互任务。
	 * Creates an interaction task.
	 *
	 * @param requestor 发起交互的玩家 / requesting player
	 * @param responder 响应目标，null 时回退为 requestor / responder, falls back to requestor if null
	 */
	public AbstractInteractionTask(Player requestor, VisibleObject responder) {
		this.requestor = requestor;
		if (responder == null) {
			this.responder = requestor;
		} else {
			this.responder = responder;
		}
	}

	/**
	 * 执行一次交互 tick。
	 * Performs one interaction tick.
	 *
	 * @return true 表示任务应停止 / true if the task should stop
	 */
	protected abstract boolean onInteraction();

	/**
	 * 交互正常结束时的回调。
	 * Callback when interaction finishes normally.
	 */
	protected abstract void onInteractionFinish();

	/**
	 * 交互开始时的回调。
	 * Callback when interaction starts.
	 */
	protected abstract void onInteractionStart();

	/**
	 * 交互中止时的回调。
	 * Callback when interaction is aborted.
	 */
	protected abstract void onInteractionAbort();

	/**
	 * 启动周期交互任务。
	 * Starts the periodic interaction task.
	 */
	public void start() {
		onInteractionStart();
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (!validateParticipants()) {
					stop(true);
				}
				boolean stopTask = onInteraction();
				if (stopTask) {
					stop(false);
				}
			}
		}, 1000, 2500);
	}

	/**
	 * 停止任务。
	 * Stops the task.
	 *
	 * @param participantNull 参与者是否已无效（为 true 时不调用 finish 回调） / true if a participant is null (skip finish callback)
	 */
	public void stop(boolean participantNull) {
		if (!participantNull) {
			onInteractionFinish();
		}
		if (task != null && !task.isCancelled()) {
			task.cancel(false);
			task = null;
		}
	}

	/**
	 * 中止交互并停止任务。
	 * Aborts the interaction and stops the task.
	 */
	public void abort() {
		onInteractionAbort();
		stop(false);
	}

	/**
	 * 判断任务是否仍在进行。
	 * Returns whether the task is still in progress.
	 *
	 * @return 是否进行中 / true if running
	 */
	public boolean isInProgress() {
		return task != null && !task.isCancelled();
	}

	/**
	 * 校验参与者是否有效。
	 * Validates that interaction participants are still valid.
	 *
	 * @return 若 requestor is non-null 则为 true / true if requestor is non-null
	 */
	public boolean validateParticipants() {
		return requestor != null;
	}
}
