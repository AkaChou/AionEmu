package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.Objects;

/**
 * Real {@link QuestBroadcastPort}: after commit, dispatches zone-mission-end to
 * each listed quest via the production dispatcher. The dispatcher callback is
 * injected after the dispatcher is constructed (break the composition cycle).
 */
public final class PlayerQuestBroadcastPort implements QuestBroadcastPort {
	@FunctionalInterface
	public interface BroadcastCall {
		boolean broadcast(Player player, int[] questIds);
	}

	private final QuestPlayerPort players;
	private final BroadcastCall broadcast;

	public PlayerQuestBroadcastPort(QuestPlayerPort players, BroadcastCall broadcast) {
		this.players = Objects.requireNonNull(players, "players");
		this.broadcast = Objects.requireNonNull(broadcast, "broadcast");
	}

	@Override
	public boolean broadcastZoneMissionEnd(QuestSnapshot snapshot, QuestMutationPlan plan, int[] questIds) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(questIds, "questIds");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出:无可分发对象,best-effort 跳过。
			return false;
		}
		return broadcast.broadcast(player, questIds);
	}
}