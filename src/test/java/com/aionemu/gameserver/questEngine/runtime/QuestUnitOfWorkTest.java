package com.aionemu.gameserver.questEngine.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestUnitOfWorkTest {
	@Test
	void requiredCommitRunsAfterCommitOnlyAndIsolatesBestEffortFailure() {
		List<String> calls = new ArrayList<>();
		QuestUnitOfWork unit = QuestUnitOfWork.open(new RecordingTransactionManager(calls));
		unit.afterCommit(() -> calls.add("after"));
		unit.afterCommit(() -> { throw new IllegalStateException("protocol"); });

		assertFalse(calls.contains("after"));
		unit.commit();
		assertEquals(List.of("begin", "commit"), calls);
		unit.runAfterCommit();

		assertEquals(List.of("begin", "commit", "after"), calls);
		assertTrue(unit.committed());
		assertEquals(1, unit.afterCommitFailures().size());
	}

	@Test
	void closeRollsBackWhenRequiredMutationFailsBeforeCommit() {
		List<String> calls = new ArrayList<>();
		try (QuestUnitOfWork ignored = QuestUnitOfWork.open(new RecordingTransactionManager(calls))) {
			ignored.afterCommit(() -> calls.add("must-not-run"));
		}
		assertEquals(List.of("begin", "rollback"), calls);
	}
}
