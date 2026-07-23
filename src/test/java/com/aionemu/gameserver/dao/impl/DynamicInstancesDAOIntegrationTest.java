package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;

class DynamicInstancesDAOIntegrationTest {

	private static final List<String> PHASES = List.of("preparation", "battle", "settlement");

	@Test
	void mysqlRecoversEveryLifecyclePhaseAfterForcedJvmTermination() throws Exception {
		Assumptions.assumeTrue(Boolean.getBoolean("aion.mysql.integration"),
				"Enable with -Daion.mysql.integration=true");
		configureDatabase();
		try (ServiceContext.Scope ignored = ServiceContext.use("dynamic-instance-integration")) {
			DatabaseFactory.init();
			DynamicInstancesDAO dao = new DynamicInstancesDAO();
			try {
				for (String phase : PHASES) {
					assertForcedRecovery(dao, phase);
				}
			} finally {
				DatabaseFactory.shutdown();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		configureDatabase();
		try (ServiceContext.Scope ignored = ServiceContext.use("dynamic-instance-integration")) {
			DatabaseFactory.init();
			long now = System.currentTimeMillis();
			int runtimeId = 1_500_000_000 + Math.floorMod((int) System.nanoTime(), 500_000_000);
			DynamicInstance instance = new DynamicInstance(0, 300030000, 33, 8, runtimeId,
					DynamicInstance.OWNER_NONE, 0, (byte) 0, DynamicInstance.ACTIVE, (byte) 0,
					now, 0, 0, 0, 1, state(args[0]).encode(), now);
			long uid = new DynamicInstancesDAO().create(instance);
			Files.writeString(Path.of(args[1]), Long.toString(uid));
			Thread.sleep(Duration.ofDays(1));
		}
	}

	private static void assertForcedRecovery(DynamicInstancesDAO dao, String phase) throws Exception {
		Path marker = Files.createTempFile("aion-instance-recovery-", ".uid");
		Process process = new ProcessBuilder(javaBinary(), "-cp", testClasspath(),
				DynamicInstancesDAOIntegrationTest.class.getName(), phase, marker.toString())
				.redirectErrorStream(true).start();
		long uid = 0;
		try {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
			while (Files.size(marker) == 0 && process.isAlive() && System.nanoTime() < deadline) {
				Thread.sleep(25);
			}
			assertTrue(process.isAlive(), () -> "Recovery child exited early: " + output(process));
			assertTrue(Files.size(marker) > 0, "Recovery child did not persist an instance");
			uid = Long.parseLong(Files.readString(marker));
			process.destroyForcibly();
			assertTrue(process.waitFor(10, TimeUnit.SECONDS));
			assertFalse(process.isAlive());

			long expectedUid = uid;
			DynamicInstance restored = dao.loadRecoverable(System.currentTimeMillis()).stream()
					.filter(instance -> instance.getInstanceUid() == expectedUid).findFirst().orElse(null);
			assertNotNull(restored);
			assertEquals(state(phase).snapshot(), InstanceRuntimeState.decode(restored.getStateJson()).snapshot());
		} finally {
			if (process.isAlive()) {
				process.destroyForcibly();
				process.waitFor(10, TimeUnit.SECONDS);
			}
			if (uid > 0) {
				delete(uid);
			}
			Files.deleteIfExists(marker);
		}
	}

	private static InstanceRuntimeState state(String phase) {
		int index = PHASES.indexOf(phase);
		if (index < 0) {
			throw new IllegalArgumentException("Unknown instance phase " + phase);
		}
		InstanceRuntimeState state = new InstanceRuntimeState();
		state.put("phase", phase);
		state.put("retail-condition.variable.stage", index);
		state.put("retail-pattern:npc.int.wave", index + 1);
		state.put("door.1", index > 0);
		state.put("spawn.reward", index == 2);
		state.put("score.team", index * 100);
		state.put("deadline.finish", 4_102_444_800_000L);
		return state;
	}

	private static void delete(long uid) throws Exception {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"DELETE FROM dynamic_instances WHERE instance_uid=?")) {
			statement.setLong(1, uid);
			statement.executeUpdate();
		}
	}

	private static String javaBinary() {
		return Path.of(System.getProperty("java.home"), "bin", "java").toString();
	}

	private static String testClasspath() {
		return System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
	}

	private static String output(Process process) {
		try {
			return new String(process.getInputStream().readAllBytes());
		} catch (IOException e) {
			return e.toString();
		}
	}

	private static void configureDatabase() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(Path.of("aion/config/network/database.properties"))) {
			properties.load(input);
		}
		DatabaseConfig.DATABASE_DRIVER = properties.getProperty("database.driver");
		DatabaseConfig.DATABASE_URL = properties.getProperty("database.url");
		DatabaseConfig.DATABASE_USER = properties.getProperty("database.user");
		DatabaseConfig.DATABASE_PASSWORD = properties.getProperty("database.password");
		DatabaseConfig.DATABASE_MAXCONNECTIONS = Integer.parseInt(properties.getProperty("database.maxconnections"));
		DatabaseConfig.HIKARI_MAX_LIFETIME = Long.parseLong(properties.getProperty("database.hikari.maxLifetime"));
		DatabaseConfig.HIKARI_CONNECTION_TEST_QUERY = properties.getProperty("database.hikari.connectionTestQuery");
	}
}
