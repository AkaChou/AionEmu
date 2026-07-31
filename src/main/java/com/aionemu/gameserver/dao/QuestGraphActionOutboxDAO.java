package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand;

/** Durable accepted-outbox boundary for quest graph teleport actions. */
public abstract class QuestGraphActionOutboxDAO implements DAO {

	@Override
	public String getClassName() {
		return QuestGraphActionOutboxDAO.class.getName();
	}

	/** Inserts a command or returns the byte-exact command already accepted under the operation key. */
	public abstract QuestGraphActionOutboxRecord acceptExact(TeleportOutboxCommand command, long acceptedAt);

	public abstract QuestGraphActionOutboxRecord find(int playerId, String operationKey);

	/** Claims an accepted or expired-lease command and returns its incremented generation. */
	public abstract QuestGraphActionOutboxRecord claim(int playerId, String operationKey, long now, long leaseUntil);

	/** Supersedes any incomplete generation when the owning player reconnects and the old connection can no longer deliver it. */
	public abstract QuestGraphActionOutboxRecord reclaimForRecovery(int playerId, String operationKey, long now, long leaseUntil);

	/** Completes only the currently leased generation before its lease expires. */
	public abstract boolean complete(int playerId, String operationKey, long claimGeneration, long completedAt);

	/** Verifies that a generation still owns the unexpired physical-delivery lease. */
	public abstract boolean isCurrentClaim(int playerId, String operationKey, long claimGeneration, long now);

	/** Records graph-journal acknowledgement independently of delivery completion. */
	public abstract boolean ackGraph(int playerId, String operationKey);

	/** Lists all rows that still require delivery, acknowledgement, or terminal GC. */
	public abstract List<QuestGraphActionOutboxRecord> listPendingForPlayer(int playerId);

	/** Deletes only the terminal completed-and-graph-acked row. */
	public abstract boolean deleteAcked(int playerId, String operationKey);

	public static final class OperationConflictException extends IllegalStateException {
		public OperationConflictException(String operationKey) {
			super("Quest graph action outbox operation key has a different command payload: " + operationKey);
		}
	}
}
