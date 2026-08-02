package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.closeDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.completeQuest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.hasItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.spawnNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.syncQuestState;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestExecutionCoordinatorTest {
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "coordinator", "fixture");

	@Test
	void requiredFailureRollsBackAndDoesNotRunAfterCommit() {
		List<String> calls = new ArrayList<>();
		Connection connection = connection(calls);
		CompiledQuestDefinition definition = definition();
		QuestEvent event = talkToNpc(700001);
		QuestTransition transition = definition.definition().transitions().get(0);
		boolean[] stateApplied = {false};
		boolean[] afterCommit = {false};
		QuestActionPort actions = new QuestActionPort() {
			@Override
			public void preflight(Connection ignored, QuestSnapshot snapshot, List<QuestAction> required) throws SQLException {
				throw new SQLException("inventory unavailable");
			}

			@Override
			public QuestTransactionParticipant apply(Connection ignored, QuestSnapshot snapshot, List<QuestAction> required) {
				throw new AssertionError("apply must not run after failed preflight");
			}
		};
		assertThrows(SQLException.class, () -> new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(connection, 7,
				definition, event, transition, snapshotPort(), actions, new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					stateApplied[0] = true;
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
				}
			},
						(ignoredAction, ignoredSnapshot, ignoredPlan) -> afterCommit[0] = true));
		assertEquals(List.of("setAutoCommit:false", "rollback"), calls);
		assertEquals(false, stateApplied[0]);
		assertEquals(false, afterCommit[0]);
	}

	@Test
	void commitRunsAfterCommitOnlyAndReportsBestEffortFailure() throws Exception {
		List<String> calls = new ArrayList<>();
		Connection connection = connection(calls);
		CompiledQuestDefinition definition = definition();
		QuestEvent event = talkToNpc(700001);
		QuestExecutionResult result = new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(connection, 7,
				definition, event, definition.definition().transitions().get(0), snapshotPort(), new RecordingActionPort(calls),
				new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("state");
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("publish");
				}
			},
				(ignoredAction, ignoredSnapshot, ignoredPlan) -> { calls.add("after"); throw new IllegalStateException("packet"); });

		assertEquals(QuestExecutionStatus.COMMITTED, result.status());
		assertEquals(List.of("setAutoCommit:false", "preflight:1", "apply:1", "state", "commit",
			"required-commit", "publish", "after"), calls);
		assertEquals(1, result.afterCommitFailures().size());
	}

	@Test
	void typedPortFalseIsReportedAsAfterCommitFailure() throws Exception {
		CompiledQuestDefinition definition = definition();
		TypedQuestAfterCommitPort typedPort = new TypedQuestAfterCommitPort(new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				return false;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}
		});

		QuestExecutionResult result = new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(
			connection(new ArrayList<>()), 7, definition, talkToNpc(700001),
			definition.definition().transitions().get(0), snapshotPort(), new RecordingActionPort(new ArrayList<>()),
			new NoOpStatePort(), typedPort);

		assertEquals(QuestExecutionStatus.COMMITTED, result.status());
		assertEquals(1, result.afterCommitFailures().size());
		assertTrue(result.afterCommitFailures().get(0) instanceof QuestAfterCommitException);
	}

	@Test
	void terminalCleanupRunsLastAndItsFailureIsReported() throws Exception {
		List<String> calls = new ArrayList<>();
		CompiledQuestDefinition definition = terminalDefinition();
		QuestExecutionCoordinator coordinator = new QuestExecutionCoordinator(new PlayerSerialExecutor(),
			(playerId, questId) -> {
				calls.add("cleanup:" + playerId + ":" + questId);
				throw new IllegalStateException("cleanup failed");
			});

		QuestExecutionResult result = coordinator.execute(connection(calls), 7, definition, talkToNpc(700001),
			definition.definition().transitions().get(0), snapshotPort(), new RecordingActionPort(calls),
			new NoOpStatePort(), (action, snapshot, plan) -> calls.add("after"));

		assertEquals(QuestExecutionStatus.COMMITTED, result.status());
		assertEquals(List.of("after", "cleanup:7:1004"), calls.subList(calls.size() - 2, calls.size()));
		assertEquals(1, result.afterCommitFailures().size());
		assertEquals("cleanup failed", result.afterCommitFailures().get(0).getMessage());
	}

	@Test
	void typedCurrencyPreflightFailureRollsBackBeforeAnyRequiredApply() {
		List<String> calls = new ArrayList<>();
		Connection connection = connection(calls);
		CompiledQuestDefinition definition = rewardDefinition();
		QuestEvent event = talkToNpc(700001);
		QuestInventoryPort inventory = new QuestInventoryPort() {
			@Override
			public void preflight(Connection ignored, QuestSnapshot snapshot, List<QuestAction.RemoveItem> removals) {
				calls.add("inventory-preflight");
			}

			@Override
			public QuestTransactionParticipant apply(Connection ignored, QuestSnapshot snapshot,
					List<QuestAction.RemoveItem> removals) {
				calls.add("inventory-apply");
				return QuestTransactionParticipant.none();
			}
		};
		QuestCurrencyPort currency = new QuestCurrencyPort() {
			@Override
			public void preflight(Connection ignored, QuestSnapshot snapshot, List<QuestAction.GrantReward> rewards)
					throws SQLException {
				calls.add("currency-preflight");
				throw new SQLException("currency unavailable");
			}

			@Override
			public QuestTransactionParticipant apply(Connection ignored, QuestSnapshot snapshot,
					List<QuestAction.GrantReward> rewards) {
				calls.add("currency-apply");
				return QuestTransactionParticipant.none();
			}
		};
		QuestRewardPort rewards = new QuestRewardPort() {
			@Override
			public void preflight(Connection ignored, QuestSnapshot snapshot, List<QuestAction.GrantReward> rewardList) {
				calls.add("reward-preflight");
			}

			@Override
			public QuestTransactionParticipant apply(Connection ignored, QuestSnapshot snapshot,
					List<QuestAction.GrantReward> rewardList) {
				calls.add("reward-apply");
				return QuestTransactionParticipant.none();
			}
		};

		assertThrows(SQLException.class, () -> new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(connection, 7,
				definition, event, definition.definition().transitions().get(0), snapshotPort(),
				new CompositeQuestActionPort(inventory, currency, rewards),
				new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("state");
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("publish");
				}
			},
				(ignoredAction, ignoredSnapshot, ignoredPlan) -> calls.add("after")));
		assertEquals(List.of("setAutoCommit:false", "inventory-preflight", "currency-preflight", "rollback"), calls);
	}

	@Test
	void rejectsSnapshotOwnershipMismatchBeforeRequiredActions() {
		List<String> calls = new ArrayList<>();
		CompiledQuestDefinition definition = definition();
		QuestTransition transition = definition.definition().transitions().get(0);
		QuestEvent event = talkToNpc(700001);
		QuestEventPort wrongSnapshot = (connection, playerId, questId, ignored) ->
				new QuestSnapshot(99, questId + 1, QuestStatus.START, 0, Map.of(182400001, 5));

		assertThrows(IllegalStateException.class, () -> new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(
				connection(calls), 7, definition, event, transition, wrongSnapshot,
				new RecordingActionPort(calls), new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("state");
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("publish");
				}
			},
				(ignoredAction, ignoredSnapshot, ignoredPlan) -> calls.add("after")));
		assertEquals(List.of("setAutoCommit:false", "rollback"), calls);
	}

	@Test
	void commitFailureRollsBackAppliedRequiredParticipant() {
		List<String> calls = new ArrayList<>();
		CompiledQuestDefinition definition = definition();
		QuestActionPort actions = new QuestActionPort() {
			@Override
			public void preflight(Connection ignored, QuestSnapshot snapshot, List<QuestAction> required) {
				calls.add("preflight");
			}

			@Override
			public QuestTransactionParticipant apply(Connection ignored, QuestSnapshot snapshot,
					List<QuestAction> required) {
				calls.add("required-apply");
				return QuestTransactionParticipant.of(() -> calls.add("required-commit"),
					() -> calls.add("required-rollback"));
			}
		};

		assertThrows(SQLException.class, () -> new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(
			connection(calls, true), 7, definition, talkToNpc(700001), definition.definition().transitions().get(0),
			snapshotPort(), actions, new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("state");
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("publish");
				}

				@Override
				public void rollback(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("state-rollback");
				}
			}, (ignoredAction, ignoredSnapshot, ignoredPlan) -> calls.add("after")));
		assertEquals(List.of("setAutoCommit:false", "preflight", "required-apply", "state", "commit",
			"rollback", "state-rollback", "required-rollback"), calls);
	}

	@Test
	void rejectsTransitionFromAnotherDefinitionBeforeOpeningTransaction() {
		List<String> calls = new ArrayList<>();
		CompiledQuestDefinition definition = definition();
		QuestTransition foreignTransition = rewardDefinition().definition().transitions().get(0);

		assertThrows(IllegalArgumentException.class, () -> new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(
				connection(calls), 7, definition, talkToNpc(700001), foreignTransition, snapshotPort(),
				new RecordingActionPort(calls), new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("state");
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
					calls.add("publish");
				}
			},
				(ignoredAction, ignoredSnapshot, ignoredPlan) -> calls.add("after")));
		assertEquals(List.of(), calls);
	}

	@Test
	void spawnRunsThroughAfterCommitOnlyAfterSuccessfulCommit() throws Exception {
		List<String> calls = new ArrayList<>();
		boolean[] spawned = {false};
		CompiledQuestDefinition definition = spawnDefinition();
		QuestExecutionResult result = new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(
				connection(calls), 7, definition, talkToNpc(700001), definition.definition().transitions().get(0),
				snapshotPort(), new RecordingActionPort(calls), new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
				}
			}, spawnAfterCommitPort(spawned));

		assertEquals(QuestExecutionStatus.COMMITTED, result.status());
		assertTrue(spawned[0]);
	}

	@Test
	void failedCommitNeverSpawns() {
		List<String> calls = new ArrayList<>();
		boolean[] spawned = {false};
		CompiledQuestDefinition definition = spawnDefinition();
		// 注入 commit 失败:afterCommit 不得执行,spawn 绝不发生。
		assertThrows(SQLException.class, () -> new QuestExecutionCoordinator(new PlayerSerialExecutor()).execute(
				connection(calls, true), 7, definition, talkToNpc(700001), definition.definition().transitions().get(0),
				snapshotPort(), new RecordingActionPort(calls), new QuestStatePort() {
				@Override
				public void apply(Connection ignored, int ignoredPlayer, QuestMutationPlan ignoredPlan) {
				}

				@Override
				public void publish(int ignoredPlayer, QuestMutationPlan ignoredPlan) {
				}
			}, spawnAfterCommitPort(spawned)));
		assertFalse(spawned[0]);
	}

	private static QuestAfterCommitPort spawnAfterCommitPort(boolean[] spawned) {
		QuestSpawnPort spawnPort = new QuestSpawnPort() {
			@Override
			public boolean spawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, int templateId,
					com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation location) {
				spawned[0] = true;
				return true;
			}

			@Override
			public boolean despawnNpc(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
				return false;
			}
		};
		return new TypedQuestAfterCommitPort(new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				return true;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}
		}, null, null, spawnPort);
	}

	private static CompiledQuestDefinition spawnDefinition() {
		return quest(1003)
				.evidence(EVIDENCE)
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).goTo("start")
				.afterCommit(spawnNpc("guardian", 310040000, 204830, 1f, 2f, 3f, (byte) 0)).compile();
	}

	private static QuestEventPort snapshotPort() {
		return (connection, playerId, questId, event) -> new QuestSnapshot(playerId, questId, QuestStatus.START, 0,
				Map.of(182400001, 5));
	}

	private static CompiledQuestDefinition definition() {
		return quest(1001)
				.evidence(EVIDENCE)
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("var1", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(hasItem(182400001, 5))
				.then(removeItem(182400001, 5)).then(setVariable("var1", 1)).goTo("reward")
				.afterCommit(closeDialog()).compile();
	}

	private static CompiledQuestDefinition rewardDefinition() {
		return quest(1002)
				.evidence(EVIDENCE)
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("var1", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(hasItem(182400001, 1))
				.then(removeItem(182400001, 1)).then(QuestDsl.grantReward("AP", 0, 5))
				.then(setVariable("var1", 1)).goTo("reward").compile();
	}

	private static CompiledQuestDefinition terminalDefinition() {
		return quest(1004)
			.evidence(EVIDENCE)
			.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var1", 0)))
			.node("complete", project(QuestStatus.COMPLETE, vars("var1", 1)))
			.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).then(completeQuest(0)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION)).afterCommit(closeDialog()).compile();
	}

	private static final class NoOpStatePort implements QuestStatePort {
		@Override
		public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
		}

		@Override
		public void publish(int playerId, QuestMutationPlan plan) {
		}
	}

	private static Connection connection(List<String> calls) {
		return connection(calls, false);
	}

	private static Connection connection(List<String> calls, boolean failCommit) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getAutoCommit" -> true;
					case "setAutoCommit" -> { calls.add("setAutoCommit:" + args[0]); yield null; }
					case "commit" -> {
						calls.add("commit");
						if (failCommit) throw new SQLException("injected commit failure");
						yield null;
					}
					case "rollback" -> { calls.add("rollback"); yield null; }
					default -> method.getReturnType().isPrimitive() && method.getReturnType() == boolean.class ? false : null;
				});
	}

	private static final class RecordingActionPort implements QuestActionPort {
		private final List<String> calls;

		private RecordingActionPort(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			calls.add("preflight:" + actions.size());
		}

		@Override
		public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			calls.add("apply:" + actions.size());
			return QuestTransactionParticipant.of(() -> calls.add("required-commit"),
				() -> calls.add("required-rollback"));
		}
	}
}
