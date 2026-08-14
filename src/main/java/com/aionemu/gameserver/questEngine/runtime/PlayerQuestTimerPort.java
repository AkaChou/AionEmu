package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy;
import com.aionemu.gameserver.services.QuestService;

import java.util.Objects;

/**
 * Real {@link QuestTimerPort}: after commit, starts/cancels quest timers through
 * {@link QuestService}. 计时器作用于玩家 (非 NPC),超时由权威 {@code QuestTimerEnd} /
 * {@code InvisibleTimerEnd} 事件回调。
 */
public final class PlayerQuestTimerPort implements QuestTimerPort {
	public enum Command { START_QUEST, START_INVISIBLE, CANCEL_QUEST }

	/** 可注入的计时器调用委托（生产 = QuestService，测试 = 记录器）。 / Injectable timer invocation delegate (production = QuestService, tests = recorder). */
	@FunctionalInterface
	public interface TimerInvoker {
		boolean apply(Player player, int questId, int seconds, Command command,
			QuestTimerPolicy policy, QuestTimerPolicy.Identity identity);
	}

	private final QuestPlayerPort players;
	private final TimerInvoker invoke;

	public PlayerQuestTimerPort(QuestPlayerPort players) {
		this(players, (player, questId, seconds, command, policy, identity) -> {
			QuestEnv env = new QuestEnv(null, player, questId, 0);
			return switch (command) {
				case START_QUEST -> QuestService.questTimerStart(env, seconds, policy);
				case START_INVISIBLE -> QuestService.invisibleTimerStart(env, seconds, policy);
				case CANCEL_QUEST -> QuestService.questTimerEnd(env, identity);
			};
		});
	}

	public PlayerQuestTimerPort(QuestPlayerPort players, TimerInvoker invoke) {
		this.players = Objects.requireNonNull(players, "players");
		this.invoke = Objects.requireNonNull(invoke, "invoke");
	}

	@Override
	public boolean startQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
			QuestTimerPolicy policy) {
		return run(snapshot, seconds, Command.START_QUEST, policy, null);
	}

	@Override
	public boolean startInvisibleTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
			QuestTimerPolicy policy) {
		return run(snapshot, seconds, Command.START_INVISIBLE, policy, null);
	}

	@Override
	public boolean cancelQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestTimerPolicy.Identity identity) {
		return run(snapshot, 0, Command.CANCEL_QUEST, null, identity);
	}

	private boolean run(QuestSnapshot snapshot, int seconds, Command command,
			QuestTimerPolicy policy, QuestTimerPolicy.Identity identity) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (seconds <= 0 && command != Command.CANCEL_QUEST) {
			throw new IllegalArgumentException("seconds must be positive");
		}
		if (command == Command.CANCEL_QUEST) {
			Objects.requireNonNull(identity, "identity");
		} else {
			Objects.requireNonNull(policy, "policy");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可调度对象，best-effort 跳过。 / Commit succeeded but player logged out: nothing to schedule, best-effort skip.
			return false;
		}
		return invoke.apply(player, snapshot.questId(), seconds, command, policy, identity);
	}
}
