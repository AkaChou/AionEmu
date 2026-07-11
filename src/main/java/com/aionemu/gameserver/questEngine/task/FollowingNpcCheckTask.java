package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
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

	/**
	 * 构造跟随检查任务。
	 * Constructs a following check task.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param destinationChecker 目的地检查器 / Destination checker
	 */
	FollowingNpcCheckTask(QuestEnv env, DestinationChecker destinationChecker) {
		this.env = env;
		this.destinationChecker = destinationChecker;
	}

	/**
	 * 执行一次跟随状态检查：死亡/超距失败，到达目的地成功。
	 * Runs one follow-state check: fail on death/out-of-range, succeed on destination reached.
	 */
	@Override
	public void run() {
		final Player player = env.getPlayer();
		Npc npc = (Npc) destinationChecker.follower;
		if (player.getLifeStats().isAlreadyDead() || npc.getLifeStats().isAlreadyDead()) {
			onFail(env);
		}
		if (!MathUtil.isIn3dRange(player, npc, 50)) {
			onFail(env);
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
	private final void onSuccess(QuestEnv env) {
		stopFollowing(env);
		GameEngineServices.questEngine().onNpcReachTarget(env);
	}

	/**
	 * 跟随失败：停止跟随并通知任务引擎丢失目标。
	 * Follow failed: stop following and notify the quest engine that the target was lost.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	protected void onFail(QuestEnv env) {
		stopFollowing(env);
		GameEngineServices.questEngine().onNpcLostTarget(env);
	}

	/**
	 * 取消玩家跟随任务并停止 NPC 跟随 AI；非 following AI 则删除 NPC。
	 * Cancels the player's follow task and stops the NPC follow AI; deletes the NPC if its AI is not "following".
	 *
	 * @param env 任务环境 / Quest environment
	 */
	private final void stopFollowing(QuestEnv env) {
		Player player = env.getPlayer();
		Npc npc = (Npc) destinationChecker.follower;
		player.getController().cancelTask(TaskId.QUEST_FOLLOW);
		npc.getAi2().onCreatureEvent(AIEventType.STOP_FOLLOW_ME, player);
		if (!npc.getAi2().getName().equals("following")) {
			npc.getController().onDelete();
		}
	}
}
