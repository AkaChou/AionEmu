package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.Objects;

/** Production player-stat protocol refresh after a committed quest reward. */
public final class PlayerQuestStatsPort implements QuestStatsPort {
	private final QuestPlayerPort players;

	public PlayerQuestStatsPort(QuestPlayerPort players) {
		this.players = Objects.requireNonNull(players, "players");
	}

	@Override
	public boolean refresh(QuestSnapshot snapshot, QuestMutationPlan plan) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		return true;
	}
}
