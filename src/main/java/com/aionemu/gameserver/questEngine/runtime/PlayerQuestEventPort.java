package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Real {@link QuestEventPort}: freezes the pre-event player facts for one owner. */
public final class PlayerQuestEventPort implements QuestEventPort {
	private final QuestPlayerPort players;
	private final QuestStartEligibilityPort startEligibilityPort;

	public PlayerQuestEventPort(QuestPlayerPort players) {
		this(players, null);
	}

	public PlayerQuestEventPort(QuestPlayerPort players, QuestStartEligibilityPort startEligibilityPort) {
		this.players = Objects.requireNonNull(players, "players");
		this.startEligibilityPort = startEligibilityPort;
	}

	@Override
	public QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event)
			throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(event, "event");
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		QuestSnapshot snapshot = QuestShadowCapture.snapshotOf(player, questId);
		snapshot = enrich(snapshot, event);
		if (startEligibilityPort != null) {
			snapshot = snapshot.withStartEligibility(startEligibilityPort.snapshot(playerId, questId));
		}
		PlayerCommonData commonData = player.getCommonData();
		if (commonData != null) {
			snapshot = snapshot.withStartingClass(
				PlayerClass.getStartingClassFor(commonData.getPlayerClass()));
		}
		if (event instanceof QuestEvent.TalkToNpc talk) {
			return snapshot.withInteractionObjectId(talk.interactionObjectId());
		}
		return snapshot;
	}

	/** Applies only server-attached event facts; definition events have no mutable facts. */
	static QuestSnapshot enrich(QuestSnapshot snapshot, QuestEvent event) {
		if (event instanceof QuestEvent.KillRanked ranked && ranked.facts() != null) {
			return snapshot.withPvpFacts(ranked.facts());
		}
		if (event instanceof QuestEvent.KillInWorld world && world.facts() != null) {
			return snapshot.withPvpFacts(world.facts());
		}
		return snapshot;
	}
}
