package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.dao.IDFactoryAwareDAO;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxCodec;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord.Status;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand;

class QuestGraphActionOutboxDAOSqlTest {

	@Test
	void usesIndependentDaoWithExactHashedKeyAndTypedPayload() {
		assertTrue(DAO.class.isAssignableFrom(com.aionemu.gameserver.dao.QuestGraphActionOutboxDAO.class));
		assertFalse(IDFactoryAwareDAO.class.isAssignableFrom(com.aionemu.gameserver.dao.QuestGraphActionOutboxDAO.class));
		assertTrue(QuestGraphActionOutboxDAO.SELECT_QUERY.contains("`operation_hash` = ?"));
		assertTrue(QuestGraphActionOutboxDAO.SELECT_COLUMNS.contains("`operation_hash`"));
		assertTrue(QuestGraphActionOutboxDAO.SELECT_COLUMNS.contains("`command_payload`"));
		assertTrue(QuestGraphActionOutboxDAO.INSERT_QUERY.contains("'ACCEPTED', 0, NULL"));
		assertTrue(Arrays.asList(new GameDAOClassProvider().daoClasses()).contains(QuestGraphActionOutboxDAO.class));

		assertArrayEquals(QuestGraphActionOutboxDAO.hash("stable-key"), QuestGraphActionOutboxDAO.hash("stable-key"));
		assertTrue(QuestGraphActionOutboxDAO.hash("stable-key").length == 32);
		assertTrue(QuestGraphActionOutboxDAO.matchesOperationHash("stable-key", QuestGraphActionOutboxDAO.hash("stable-key")));
		assertFalse(QuestGraphActionOutboxDAO.matchesOperationHash("different", QuestGraphActionOutboxDAO.hash("stable-key")));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphActionOutboxDAO.hash("bad\uD800"));
		assertThrows(IllegalArgumentException.class,
			() -> new QuestGraphActionOutboxDAO().find(7, "x".repeat(TeleportOutboxCommand.MAX_OPERATION_KEY_LENGTH + 1)));
	}

	@Test
	void claimCompletionAcknowledgementAndDeletionAreGenerationAndStateGuarded() {
		assertTrue(QuestGraphActionOutboxDAO.CLAIM_QUERY.contains("`claim_generation` = `claim_generation` + 1"));
		assertTrue(QuestGraphActionOutboxDAO.CLAIM_QUERY.contains("`status` = 'ACCEPTED'"));
		assertTrue(QuestGraphActionOutboxDAO.CLAIM_QUERY.contains("`lease_until` <= ?"));
		assertTrue(QuestGraphActionOutboxDAO.CLAIM_QUERY.contains("BINARY `operation_key` = BINARY ?"));
		assertTrue(QuestGraphActionOutboxDAO.RECOVERY_CLAIM_QUERY.contains("`status` IN ('ACCEPTED', 'CLAIMED')"));
		assertFalse(QuestGraphActionOutboxDAO.RECOVERY_CLAIM_QUERY.contains("`lease_until` <= ?"));

		assertTrue(QuestGraphActionOutboxDAO.COMPLETE_QUERY.contains("`status` = 'CLAIMED'"));
		assertTrue(QuestGraphActionOutboxDAO.COMPLETE_QUERY.contains("`claim_generation` = ?"));
		assertTrue(QuestGraphActionOutboxDAO.COMPLETE_QUERY.contains("`lease_until` > ?"));
		assertTrue(QuestGraphActionOutboxDAO.CURRENT_CLAIM_QUERY.contains("`claim_generation` = ?"));
		assertTrue(QuestGraphActionOutboxDAO.CURRENT_CLAIM_QUERY.contains("`lease_until` > ?"));
		assertTrue(QuestGraphActionOutboxDAO.SELECT_DELIVERY_HEAD_QUERY.endsWith("ORDER BY `outbox_sequence` LIMIT 1 FOR UPDATE"));
		assertTrue(QuestGraphActionOutboxDAO.COMPLETE_QUERY.contains("IF(`graph_acked` = 1"));
		assertTrue(QuestGraphActionOutboxDAO.PERSIST_PLAYER_LOCATION_QUERY.contains("`x` = ?"));
		assertTrue(QuestGraphActionOutboxDAO.PERSIST_PLAYER_LOCATION_QUERY.contains("`world_id` = ?"));
		assertTrue(QuestGraphActionOutboxDAO.PERSIST_PLAYER_LOCATION_QUERY.endsWith("WHERE `id` = ?"));

		assertTrue(QuestGraphActionOutboxDAO.ACK_GRAPH_QUERY.contains("`graph_acked` = 1"));
		assertTrue(QuestGraphActionOutboxDAO.ACK_GRAPH_QUERY.contains("`completed_at` IS NULL"));
		assertTrue(QuestGraphActionOutboxDAO.DELETE_ACKED_QUERY.contains("`status` = 'GRAPH_ACKED'"));
		assertTrue(QuestGraphActionOutboxDAO.DELETE_ACKED_QUERY.contains("`completed_at` IS NOT NULL"));
		assertTrue(QuestGraphActionOutboxDAO.DELETE_ACKED_QUERY.contains("`command_payload` = ?"));
		assertTrue(QuestGraphActionOutboxDAO.LIST_PENDING_QUERY.endsWith("ORDER BY `outbox_sequence`"));
		assertFalse(QuestGraphActionOutboxDAO.LIST_PENDING_QUERY.contains("`status` <> 'GRAPH_ACKED'"));
	}

	@Test
	void classifiesOnlyMysqlDuplicateKeyAsExactAcceptCandidate() {
		assertTrue(QuestGraphActionOutboxDAO.isDuplicateKey(new SQLException("duplicate", "23000", 1062)));
		assertFalse(QuestGraphActionOutboxDAO.isDuplicateKey(new SQLException("foreign key", "23000", 1452)));

		TeleportOutboxCommand accepted = command("operation", 4, 210010000);
		QuestGraphActionOutboxRecord existing = QuestGraphActionOutboxRecord.accepted(accepted, 1, 100);
		assertTrue(QuestGraphActionOutboxDAO.hasExactPayload(existing, QuestGraphActionOutboxCodec.encode(accepted)));
		assertFalse(QuestGraphActionOutboxDAO.hasExactPayload(existing,
			QuestGraphActionOutboxCodec.encode(command("operation", 4, 220010000))));
	}

	@Test
	void retriesCompletionOnlyForTheSameCompletedClaimGeneration() {
		TeleportOutboxCommand command = command("operation", 4, 210010000);
		QuestGraphActionOutboxRecord completed = new QuestGraphActionOutboxRecord(command, 1, Status.COMPLETED, 3,
			null, 100, 150L, false);

		assertTrue(QuestGraphActionOutboxDAO.isCompletedGeneration(completed, 3));
		assertFalse(QuestGraphActionOutboxDAO.isCompletedGeneration(completed, 2));
		assertFalse(QuestGraphActionOutboxDAO.isCompletedGeneration(
			new QuestGraphActionOutboxRecord(command, 1, Status.CLAIMED, 3, 200L, 100, null, false), 3));
	}

	@Test
	void playerLocationPersistenceFailureRollsBackTheCompletionTransaction() {
		AtomicInteger autoCommitDisabled = new AtomicInteger();
		AtomicInteger commits = new AtomicInteger();
		AtomicInteger rollbacks = new AtomicInteger();
		Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[] {Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "setAutoCommit" -> {
					assertEquals(false, args[0]);
					autoCommitDisabled.incrementAndGet();
					yield null;
				}
				case "commit" -> {
					commits.incrementAndGet();
					yield null;
				}
				case "rollback" -> {
					rollbacks.incrementAndGet();
					yield null;
				}
				default -> throw new UnsupportedOperationException(method.getName());
			});

		assertThrows(IllegalStateException.class, () -> QuestGraphActionOutboxDAO.inTransaction(connection, () -> {
			throw new IllegalStateException("player location persistence failed");
		}));
		assertEquals(1, autoCommitDisabled.get());
		assertEquals(0, commits.get());
		assertEquals(1, rollbacks.get());
	}

	private static TeleportOutboxCommand command(String operationKey, long baseRevision, int worldId) {
		return new TeleportOutboxCommand(7, 2634, baseRevision, "accept", 2, worldId, 30001,
			TeleportOutboxCommand.InstanceRecoveryMode.EXACT, 1, 2, 3, (byte) 4, operationKey);
	}
}
