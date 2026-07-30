package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 任务跟随 NPC 周期检查任务：监控玩家/NPC 存活与距离，以及是否到达目的地。
 * Periodic quest follow-NPC check task: monitors player/NPC life and distance, and destination arrival.
 *
 * @author ATracer
 */
public class FollowingNpcCheckTask implements Runnable {

	/** 任务事件环境。 Quest event environment. */
	private final QuestEnv env;
	/** 目的地检查器。 Destination checker. */
	private final DestinationChecker destinationChecker;
	/** 唯一终态 listener。 Single terminal-outcome listener. */
	private final QuestEscortCompletionListener completionListener;
	/** 防止同一 tick 的死亡、超距和到达检查重复投递终态。 / Prevents duplicate terminal delivery from one or later ticks. */
	private boolean completed;

	/**
	 * 构造跟随检查任务。
	 * Constructs a following check task.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param destinationChecker 目的地检查器 / Destination checker
	 */
	FollowingNpcCheckTask(QuestEnv env, DestinationChecker destinationChecker) {
		this(env, destinationChecker, QuestEscortCompletionListener.legacyQuestEngine());
	}

	/** 创建带显式 owner listener 的检查任务。 / Creates a check task with an explicit owner listener. */
	FollowingNpcCheckTask(QuestEnv env, DestinationChecker destinationChecker, QuestEscortCompletionListener completionListener) {
		if (env == null || destinationChecker == null || completionListener == null) {
			throw new IllegalArgumentException("Escort check task dependencies are missing");
		}
		this.env = env;
		this.destinationChecker = destinationChecker;
		this.completionListener = completionListener;
	}

	/**
	 * 执行一次跟随状态检查：死亡/超距失败，到达目的地成功。
	 * Runs one follow-state check: fail on death/out-of-range, succeed on destination reached.
	 */
	@Override
	public synchronized void run() {
		if (completed) {
			return;
		}
		final Player player = env.getPlayer();
		Npc npc = (Npc) destinationChecker.follower;
		if (player.getLifeStats().isAlreadyDead() || npc.getLifeStats().isAlreadyDead()) {
			onFail(env);
			return;
		}
		if (!MathUtil.isIn3dRange(player, npc, 50)) {
			onFail(env);
			return;
		}

		if (destinationChecker.check()) {
			onSuccess(env);
		}
	}

	/**
	 * 跟随成功：停止跟随并通知任务引擎到达目标。
	 * Follow succeeded: stop following and notify the quest engine that the target was reached.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	private void onSuccess(QuestEnv env) {
		completed = true;
		stopFollowing(env);
		completionListener.onReached(env, (Npc) destinationChecker.follower);
	}

	/**
	 * 跟随失败：停止跟随并通知任务引擎丢失目标。
	 * Follow failed: stop following and notify the quest engine that the target was lost.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	protected void onFail(QuestEnv env) {
		completed = true;
		stopFollowing(env);
		completionListener.onLost(env, (Npc) destinationChecker.follower);
	}

	/**
	 * 取消玩家跟随任务并停止 NPC 跟随 AI；非 following AI 则删除 NPC。
	 * Cancels the player's follow task and stops the NPC follow AI; deletes the NPC if its AI is not "following".
	 *
	 * @param env 任务环境 / Quest environment
	 */
	private void stopFollowing(QuestEnv env) {
		Player player = env.getPlayer();
		Npc npc = (Npc) destinationChecker.follower;
		player.getController().cancelTask(TaskId.QUEST_FOLLOW);
		npc.getAi2().onCreatureEvent(AIEventType.STOP_FOLLOW_ME, player);
		if (!npc.getAi2().getName().equals("following")) {
			npc.getController().onDelete();
		}
	}
}
