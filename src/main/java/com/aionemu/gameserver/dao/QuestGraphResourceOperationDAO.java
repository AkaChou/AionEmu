package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;

/** Durable operation-key to typed resource-identity registry for quest graph world effects. */
public abstract class QuestGraphResourceOperationDAO implements IDFactoryAwareDAO {
	public static final int MAX_OPERATION_KEY_LENGTH = 1024;

	@Override
	public String getClassName() {
		return QuestGraphResourceOperationDAO.class.getName();
	}

	/** Returns the materialized resource identity for an operation, or {@code null}. */
	public abstract CleanupLease find(int playerId, String operationKey);

	/** Atomically inserts a materialized identity, or returns the exact identity already owning the key. */
	public abstract CleanupLease reserve(CleanupLease candidate);

	/** Deletes only the exact operation identity supplied by the caller. */
	public abstract boolean release(CleanupLease expected);

	/** Returns every object identity retained by the durable operation registry. */
	public abstract Map<Integer, CleanupLease> getUsedResourceLeases();

	/** Indicates that an insert was rejected because another operation owns the candidate object id. */
	public static final class ObjectIdReservationConflictException extends IllegalStateException {
		public ObjectIdReservationConflictException(Throwable cause) {
			super("Quest graph resource object id is already reserved by another operation", cause);
		}
	}
}
