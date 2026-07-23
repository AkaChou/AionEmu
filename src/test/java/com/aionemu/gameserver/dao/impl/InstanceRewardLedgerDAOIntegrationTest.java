package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class InstanceRewardLedgerDAOIntegrationTest {

	private static final String HASH_A = "a".repeat(64);
	private static final String HASH_B = "b".repeat(64);
	private static final String PAYLOAD = "{}";

	@Test
	void mysqlSerializesDuplicateSettlementAndRejectsPayloadDrift() throws Exception {
		Assumptions.assumeTrue(Boolean.getBoolean("aion.mysql.integration"),
				"Enable with -Daion.mysql.integration=true");
		Properties database = databaseProperties();
		long instanceUid = Long.MAX_VALUE - Math.floorMod(System.nanoTime(), 1_000_000_000L);
		int playerId = 2_147_483_000;
		InstanceRewardLedgerDAO dao = new InstanceRewardLedgerDAO();
		try {
			assertConcurrentSettlement(database, dao, instanceUid, playerId);
			assertPendingRetry(database, dao, instanceUid, playerId);
			assertPayloadDriftRejected(database, dao, instanceUid, playerId);
		} finally {
			try (Connection connection = connection(database);
					PreparedStatement delete = connection.prepareStatement(
							"DELETE FROM instance_reward_ledger WHERE instance_uid=? AND player_id=?")) {
				delete.setLong(1, instanceUid);
				delete.setInt(2, playerId);
				delete.executeUpdate();
			}
		}
	}

	private static void assertConcurrentSettlement(Properties database, InstanceRewardLedgerDAO dao,
			long instanceUid, int playerId) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Boolean>> results = List.of(
					executor.submit(() -> settle(database, dao, instanceUid, playerId, "concurrent", ready, start)),
					executor.submit(() -> settle(database, dao, instanceUid, playerId, "concurrent", ready, start)));
			assertTrue(ready.await(10, TimeUnit.SECONDS));
			start.countDown();
			int alreadyCompleted = 0;
			for (Future<Boolean> result : results) {
				if (result.get(10, TimeUnit.SECONDS)) {
					alreadyCompleted++;
				}
			}
			assertEquals(1, alreadyCompleted);
		} finally {
			executor.shutdownNow();
		}
	}

	private static boolean settle(Properties database, InstanceRewardLedgerDAO dao, long instanceUid,
			int playerId, String rewardKey, CountDownLatch ready, CountDownLatch start) throws Exception {
		try (Connection connection = connection(database)) {
			connection.setAutoCommit(false);
			ready.countDown();
			start.await();
			boolean completed = dao.lockOrCreate(connection, instanceUid, playerId, rewardKey, HASH_A, PAYLOAD, 1);
			if (!completed) {
				dao.complete(connection, instanceUid, playerId, rewardKey, 2);
			}
			connection.commit();
			return completed;
		}
	}

	private static void assertPendingRetry(Properties database, InstanceRewardLedgerDAO dao, long instanceUid,
			int playerId) throws SQLException {
		try (Connection connection = connection(database)) {
			connection.setAutoCommit(false);
			assertFalse(dao.lockOrCreate(connection, instanceUid, playerId, "retry", HASH_A, PAYLOAD, 1));
			connection.commit();
		}
		try (Connection connection = connection(database)) {
			connection.setAutoCommit(false);
			assertFalse(dao.lockOrCreate(connection, instanceUid, playerId, "retry", HASH_A, PAYLOAD, 1));
			dao.complete(connection, instanceUid, playerId, "retry", 2);
			connection.commit();
		}
		try (Connection connection = connection(database)) {
			connection.setAutoCommit(false);
			assertTrue(dao.lockOrCreate(connection, instanceUid, playerId, "retry", HASH_A, PAYLOAD, 1));
			connection.commit();
		}
	}

	private static void assertPayloadDriftRejected(Properties database, InstanceRewardLedgerDAO dao,
			long instanceUid, int playerId) throws SQLException {
		try (Connection connection = connection(database)) {
			connection.setAutoCommit(false);
			assertThrows(SQLException.class,
					() -> dao.lockOrCreate(connection, instanceUid, playerId, "concurrent", HASH_B, PAYLOAD, 1));
			connection.rollback();
		}
	}

	private static Connection connection(Properties database) throws SQLException {
		Connection connection = DriverManager.getConnection(database.getProperty("database.url"),
				database.getProperty("database.user"), database.getProperty("database.password"));
		connection.createStatement().execute("SET FOREIGN_KEY_CHECKS=0");
		return connection;
	}

	private static Properties databaseProperties() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(Path.of("aion/config/network/database.properties"))) {
			properties.load(input);
		}
		return properties;
	}
}
