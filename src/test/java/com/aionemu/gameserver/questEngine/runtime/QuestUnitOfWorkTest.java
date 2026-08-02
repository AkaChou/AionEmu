package com.aionemu.gameserver.questEngine.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestUnitOfWorkTest {
	@Test
	void requiredCommitRunsAfterCommitOnlyAndIsolatesBestEffortFailure() throws Exception {
		List<String> calls = new ArrayList<>();
		Connection connection = connection(calls);
		QuestUnitOfWork unit = QuestUnitOfWork.open(connection);
		unit.afterCommit(() -> calls.add("after"));
		unit.afterCommit(() -> { throw new IllegalStateException("protocol"); });

		assertFalse(calls.contains("after"));
		unit.commit();
		assertEquals(List.of("setAutoCommit:false", "commit"), calls);
		unit.runAfterCommit();

		assertEquals(List.of("setAutoCommit:false", "commit", "after"), calls);
		assertTrue(unit.committed());
		assertEquals(1, unit.afterCommitFailures().size());
	}

	@Test
	void closeRollsBackWhenRequiredMutationFailsBeforeCommit() throws Exception {
		List<String> calls = new ArrayList<>();
		try (QuestUnitOfWork ignored = QuestUnitOfWork.open(connection(calls))) {
			ignored.afterCommit(() -> calls.add("must-not-run"));
		}
		assertEquals(List.of("setAutoCommit:false", "rollback"), calls);
	}

	private static Connection connection(List<String> calls) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getAutoCommit" -> true;
					case "setAutoCommit" -> { calls.add("setAutoCommit:" + args[0]); yield null; }
					case "commit" -> { calls.add("commit"); yield null; }
					case "rollback" -> { calls.add("rollback"); yield null; }
					case "toString" -> "test-connection";
					default -> defaultValue(method.getReturnType());
				});
	}

	private static Object defaultValue(Class<?> returnType) {
		if (!returnType.isPrimitive()) {
			return null;
		}
		if (returnType == boolean.class) {
			return false;
		}
		if (returnType == int.class || returnType == short.class || returnType == byte.class) {
			return 0;
		}
		if (returnType == long.class) {
			return 0L;
		}
		if (returnType == float.class) {
			return 0F;
		}
		if (returnType == double.class) {
			return 0D;
		}
		if (returnType == char.class) {
			return '\0';
		}
		return null;
	}
}
