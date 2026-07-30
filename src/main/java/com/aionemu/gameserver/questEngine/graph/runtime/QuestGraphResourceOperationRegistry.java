package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.QuestGraphResourceOperationDAO;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;

/** Adapter-facing boundary for durable quest graph resource operation identities. */
final class QuestGraphResourceOperationRegistry {

	private final BiFunction<Integer, String, CleanupLease> loader;
	private final Function<CleanupLease, CleanupLease> reserver;
	private final Function<CleanupLease, Boolean> releaser;
	private final boolean durable;

	static QuestGraphResourceOperationRegistry production() {
		return new QuestGraphResourceOperationRegistry(
			(playerId, key) -> DAOManager.getDAO(QuestGraphResourceOperationDAO.class).find(playerId, key),
			lease -> DAOManager.getDAO(QuestGraphResourceOperationDAO.class).reserve(lease),
			lease -> DAOManager.getDAO(QuestGraphResourceOperationDAO.class).release(lease), true);
	}

	static QuestGraphResourceOperationRegistry passthrough() {
		return new QuestGraphResourceOperationRegistry((playerId, key) -> null, Function.identity(), lease -> true, false);
	}

	QuestGraphResourceOperationRegistry(BiFunction<Integer, String, CleanupLease> loader,
			Function<CleanupLease, CleanupLease> reserver, Function<CleanupLease, Boolean> releaser) {
		this(loader, reserver, releaser, true);
	}

	private QuestGraphResourceOperationRegistry(BiFunction<Integer, String, CleanupLease> loader,
			Function<CleanupLease, CleanupLease> reserver, Function<CleanupLease, Boolean> releaser, boolean durable) {
		this.loader = Objects.requireNonNull(loader, "loader");
		this.reserver = Objects.requireNonNull(reserver, "reserver");
		this.releaser = Objects.requireNonNull(releaser, "releaser");
		this.durable = durable;
	}

	CleanupLease find(int playerId, String key) {
		return loader.apply(playerId, key);
	}

	CleanupLease reserve(CleanupLease candidate) {
		return Objects.requireNonNull(reserver.apply(candidate), "resource operation reservation");
	}

	boolean release(CleanupLease expected) {
		CleanupLease current = find(expected.identity().playerId(), expected.resourceKey());
		return current == null || current.equals(expected) && Boolean.TRUE.equals(releaser.apply(expected));
	}

	boolean durable() {
		return durable;
	}
}
