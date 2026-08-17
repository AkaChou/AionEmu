package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestRecipeOwnership;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeQuestActionPortTest {
	@Test
	void partitionsClosedActionsAndSharesTheExactConnection() throws Exception {
		Connection connection = connection();
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(),
				Map.of(QuestRewardKind.AP, 10L));
		List<String> calls = new ArrayList<>();
		QuestInventoryPort inventory = new RecordingInventory(connection, calls);
		QuestCurrencyPort currency = new RecordingCurrency(connection, calls);
		QuestRewardPort rewards = new RecordingRewards(connection, calls);
		QuestCraftPort craft = new RecordingCraft(connection, calls);
		CompositeQuestActionPort port = new CompositeQuestActionPort(inventory, currency, rewards, craft);
		List<QuestAction> actions = List.of(new QuestAction.RemoveItem(182400001, 2),
				new QuestAction.SetVariable("step", 1), new QuestAction.GrantReward("AP", 0, 5),
				new QuestAction.GrantReward("ITEM", 188050000, 1),
				new QuestAction.SetStatus(QuestStatus.REWARD),
				new QuestAction.LearnRecipe(155004001, QuestRecipeOwnership.QUEST_OWNED));

		port.preflight(connection, snapshot, actions);
		QuestTransactionParticipant participant = port.apply(connection, snapshot, actions);
		participant.afterCommit();

		assertEquals(List.of("inventory.preflight", "currency.preflight", "reward.preflight", "craft.preflight",
				"inventory.apply", "currency.apply", "reward.apply", "craft.apply",
				"inventory.commit", "currency.commit", "reward.commit", "craft.commit"), calls);
	}

	@Test
	void craftActionsFailClosedWhenCompositionOmitsCraftPort() {
		Connection connection = connection();
		CompositeQuestActionPort port = new CompositeQuestActionPort(new RecordingInventory(connection, new ArrayList<>()),
			new RecordingCurrency(connection, new ArrayList<>()), new RecordingRewards(connection, new ArrayList<>()));
		assertThrows(SQLException.class, () -> port.preflight(connection,
			new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(), Map.of(QuestRewardKind.AP, 10L)),
			List.of(new QuestAction.RemoveItem(182400001, 1),
				new QuestAction.GrantReward("AP", 0, 1),
				new QuestAction.GrantReward("ITEM", 188050000, 1),
				new QuestAction.ForgetRecipe(155004001))));
	}

	@Test
	void routesCurrencyDebitsThroughTheSameTransactionalCurrencyPort() throws Exception {
		Connection connection = connection();
		List<String> calls = new ArrayList<>();
		CompositeQuestActionPort port = new CompositeQuestActionPort(
			new RecordingInventory(connection, calls), new RecordingCurrency(connection, calls),
			new RecordingRewards(connection, calls));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(),
			Map.of(QuestRewardKind.GOLD, 10L));
		List<QuestAction> actions = List.of(
			new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 3));

		port.preflight(connection, snapshot, actions);
		QuestTransactionParticipant participant = port.apply(connection, snapshot, actions);
		participant.afterCommit();

		assertEquals(List.of("currency.debit.preflight", "currency.debit.apply", "currency.debit.commit"), calls);
	}

	@Test
	void routesCurrencySetsThroughTheSameTransactionalCurrencyPort() throws Exception {
		Connection connection = connection();
		List<String> calls = new ArrayList<>();
		CompositeQuestActionPort port = new CompositeQuestActionPort(
			new RecordingInventory(connection, calls), new RecordingCurrency(connection, calls),
			new RecordingRewards(connection, calls));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(),
			Map.of(QuestRewardKind.DP, 10L));

		List<QuestAction> actions = List.of(new QuestAction.SetCurrency(QuestRewardKind.DP, 0));
		port.preflight(connection, snapshot, actions);
		QuestTransactionParticipant participant = port.apply(connection, snapshot, actions);
		participant.afterCommit();

		assertEquals(List.of("currency.set.preflight", "currency.set.apply", "currency.set.commit"), calls);
	}

	@Test
	void rejectsMixedCurrencyOperationFamiliesBeforeAnyPortCall() {
		Connection connection = connection();
		List<String> calls = new ArrayList<>();
		CompositeQuestActionPort port = new CompositeQuestActionPort(
			new RecordingInventory(connection, calls), new RecordingCurrency(connection, calls),
			new RecordingRewards(connection, calls));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(),
			Map.of(QuestRewardKind.DP, 10L));
		List<QuestAction> actions = List.of(new QuestAction.SetCurrency(QuestRewardKind.DP, 0),
			new QuestAction.DecreaseCurrency(QuestRewardKind.DP, 1),
			new QuestAction.GrantReward("DP", 0, 1));

		assertThrows(SQLException.class, () -> port.preflight(connection, snapshot, actions));
		assertThrows(SQLException.class, () -> port.apply(connection, snapshot, actions));
		assertTrue(calls.isEmpty());
	}

	@Test
	void rollsBackCurrencyParticipantWhenLaterDurableApplyFails() throws Exception {
		Connection connection = connection();
		List<String> calls = new ArrayList<>();
		QuestRewardPort failingRewards = new QuestRewardPort() {
			@Override public void preflight(Connection ignored, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			}

			@Override public QuestTransactionParticipant apply(Connection ignored, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) throws SQLException {
				calls.add("reward.apply");
				throw new SQLException("recording durable reward failure");
			}
		};
		CompositeQuestActionPort port = new CompositeQuestActionPort(
			new RecordingInventory(connection, calls), new RecordingCurrency(connection, calls), failingRewards);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(),
			Map.of(QuestRewardKind.GOLD, 10L));

		List<QuestAction> actions = List.of(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 3),
			new QuestAction.GrantReward("ITEM", 188050000, 1));
		port.preflight(connection, snapshot, actions);

		assertThrows(SQLException.class, () -> port.apply(connection, snapshot, actions));
		assertEquals(List.of("currency.debit.preflight", "currency.debit.apply", "reward.apply",
			"currency.debit.rollback"), calls);
	}

	@Test
	void unequipsBeforeInventoryRemovalOnTheSameTransaction() throws Exception {
		Connection connection = connection();
		List<String> calls = new ArrayList<>();
		CompositeQuestActionPort port = new CompositeQuestActionPort(
			new RecordingInventory(connection, calls), new RecordingCurrency(connection, calls),
			new RecordingRewards(connection, calls), null, new RecordingEquipment(connection, calls));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0,
			Map.of(140000003, 0)).withEquipmentFacts(
				new QuestEquipmentFacts(Map.of(), Map.of(140000003, 1)));
		List<QuestAction> actions = List.of(new QuestAction.UnequipItem(140000003),
			new QuestAction.RemoveItem(140000003, 1));

		port.preflight(connection, snapshot, actions);
		QuestTransactionParticipant participant = port.apply(connection, snapshot, actions);
		participant.afterCommit();

		assertEquals(List.of("equipment.preflight", "inventory.preflight", "equipment.apply",
			"inventory.apply", "equipment.commit", "inventory.commit"), calls);
	}

	@Test
	void unequipFailsClosedWhenCompositionOmitsEquipmentPort() {
		CompositeQuestActionPort port = new CompositeQuestActionPort(
			new RecordingInventory(connection(), new ArrayList<>()),
			new RecordingCurrency(connection(), new ArrayList<>()),
			new RecordingRewards(connection(), new ArrayList<>()));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of())
			.withEquipmentFacts(new QuestEquipmentFacts(Map.of(), Map.of(140000003, 1)));
		assertThrows(SQLException.class, () -> port.preflight(connection(), snapshot,
			List.of(new QuestAction.UnequipItem(140000003))));
	}

	@Test
	void routesArchDaevaPromotionAfterDurableRewardsOnTheSameTransaction() throws Exception {
		Connection connection = connection();
		List<String> calls = new ArrayList<>();
		CompositeQuestActionPort port = new CompositeQuestActionPort(
			new RecordingInventory(connection, calls), new RecordingCurrency(connection, calls),
			new RecordingRewards(connection, calls), null, null, new RecordingProgression(connection, calls));
		QuestSnapshot snapshot = new QuestSnapshot(7, 10520, QuestStatus.REWARD, 6, Map.of(), Map.of());
		List<QuestAction> actions = List.of(new QuestAction.GrantReward("EXP", 0, 25849149),
			new QuestAction.PromoteArchDaeva());

		port.preflight(connection, snapshot, actions);
		QuestTransactionParticipant participant = port.apply(connection, snapshot, actions);
		participant.afterCommit();

		assertEquals(List.of("reward.preflight", "progression.preflight", "reward.apply", "progression.apply",
			"reward.commit", "progression.commit"), calls);
	}

	@Test
	void unknownRewardKindFailsClosed() {
		assertThrows(IllegalArgumentException.class, () -> new QuestAction.GrantReward("teleport", 1, 1));
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
				(proxy, method, args) -> method.getReturnType() == boolean.class ? false : null);
	}

	private static final class RecordingInventory implements QuestInventoryPort {
		private final Connection expected;
		private final List<String> calls;
		private RecordingInventory(Connection expected, List<String> calls) { this.expected = expected; this.calls = calls; }
		@Override public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction.RemoveItem> removals,
				List<QuestAction.GiveItem> gives) {
			if (removals.isEmpty() && gives.isEmpty()) return;
			assertSame(expected, connection); assertEquals(1, removals.size()); assertTrue(gives.isEmpty()); calls.add("inventory.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) {
			if (removals.isEmpty() && gives.isEmpty()) return QuestTransactionParticipant.none();
			assertSame(expected, connection); assertEquals(1, removals.size()); assertTrue(gives.isEmpty()); calls.add("inventory.apply");
			return QuestTransactionParticipant.of(() -> calls.add("inventory.commit"), () -> calls.add("inventory.rollback"));
		}
	}

	private static final class RecordingCurrency implements QuestCurrencyPort {
		private final Connection expected;
		private final List<String> calls;
		private RecordingCurrency(Connection expected, List<String> calls) { this.expected = expected; this.calls = calls; }
		@Override public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction.GrantReward> rewards) {
			if (rewards.isEmpty()) return;
			assertSame(expected, connection); assertEquals(1, rewards.size()); calls.add("currency.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			if (rewards.isEmpty()) return QuestTransactionParticipant.none();
			assertSame(expected, connection); assertEquals(1, rewards.size()); calls.add("currency.apply");
			return QuestTransactionParticipant.of(() -> calls.add("currency.commit"), () -> calls.add("currency.rollback"));
		}
		@Override public void preflightDebits(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.DecreaseCurrency> debits) {
			if (debits.isEmpty()) return;
			assertSame(expected, connection); assertEquals(1, debits.size()); calls.add("currency.debit.preflight");
		}
		@Override public QuestTransactionParticipant applyDebits(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.DecreaseCurrency> debits) {
			if (debits.isEmpty()) return QuestTransactionParticipant.none();
			assertSame(expected, connection); assertEquals(1, debits.size()); calls.add("currency.debit.apply");
			return QuestTransactionParticipant.of(() -> calls.add("currency.debit.commit"),
				() -> calls.add("currency.debit.rollback"));
		}
		@Override public void preflightSets(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.SetCurrency> sets) {
			if (sets.isEmpty()) return;
			assertSame(expected, connection); assertEquals(1, sets.size()); calls.add("currency.set.preflight");
		}
		@Override public QuestTransactionParticipant applySets(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.SetCurrency> sets) {
			if (sets.isEmpty()) return QuestTransactionParticipant.none();
			assertSame(expected, connection); assertEquals(1, sets.size()); calls.add("currency.set.apply");
			return QuestTransactionParticipant.of(() -> calls.add("currency.set.commit"),
				() -> calls.add("currency.set.rollback"));
		}
	}

	private static final class RecordingRewards implements QuestRewardPort {
		private final Connection expected;
		private final List<String> calls;
		private RecordingRewards(Connection expected, List<String> calls) { this.expected = expected; this.calls = calls; }
		@Override public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction.GrantReward> rewards) {
			if (rewards.isEmpty()) return;
			assertSame(expected, connection); assertEquals(1, rewards.size()); calls.add("reward.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			if (rewards.isEmpty()) return QuestTransactionParticipant.none();
			assertSame(expected, connection); assertEquals(1, rewards.size()); calls.add("reward.apply");
			return QuestTransactionParticipant.of(() -> calls.add("reward.commit"), () -> calls.add("reward.rollback"));
		}
	}

	private static final class RecordingCraft implements QuestCraftPort {
		private final Connection expected;
		private final List<String> calls;
		private RecordingCraft(Connection expected, List<String> calls) { this.expected = expected; this.calls = calls; }
		@Override public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			assertSame(expected, connection); assertEquals(1, actions.size()); calls.add("craft.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction> actions) {
			assertSame(expected, connection); assertEquals(1, actions.size()); calls.add("craft.apply");
			return QuestTransactionParticipant.of(() -> calls.add("craft.commit"), () -> calls.add("craft.rollback"));
		}
	}

	private static final class RecordingEquipment implements QuestEquipmentPort {
		private final Connection expected;
		private final List<String> calls;
		private RecordingEquipment(Connection expected, List<String> calls) {
			this.expected = expected;
			this.calls = calls;
		}
		@Override public void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.UnequipItem> unequips) {
			assertSame(expected, connection);
			assertEquals(List.of(new QuestAction.UnequipItem(140000003)), unequips);
			calls.add("equipment.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.UnequipItem> unequips) {
			assertSame(expected, connection);
			assertEquals(List.of(new QuestAction.UnequipItem(140000003)), unequips);
			calls.add("equipment.apply");
			return QuestTransactionParticipant.of(() -> calls.add("equipment.commit"),
				() -> calls.add("equipment.rollback"));
		}
	}

	private static final class RecordingProgression implements QuestProgressionPort {
		private final Connection expected;
		private final List<String> calls;

		private RecordingProgression(Connection expected, List<String> calls) {
			this.expected = expected;
			this.calls = calls;
		}

		@Override
		public void preflight(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.PromoteArchDaeva> promotions) {
			assertSame(expected, connection);
			assertEquals(List.of(new QuestAction.PromoteArchDaeva()), promotions);
			calls.add("progression.preflight");
		}

		@Override
		public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.PromoteArchDaeva> promotions) {
			assertSame(expected, connection);
			assertEquals(List.of(new QuestAction.PromoteArchDaeva()), promotions);
			calls.add("progression.apply");
			return QuestTransactionParticipant.of(() -> calls.add("progression.commit"),
				() -> calls.add("progression.rollback"));
		}
	}
}
