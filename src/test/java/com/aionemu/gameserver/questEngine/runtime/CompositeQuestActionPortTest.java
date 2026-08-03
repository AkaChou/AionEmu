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
			assertSame(expected, connection); assertEquals(1, removals.size()); assertTrue(gives.isEmpty()); calls.add("inventory.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) {
			assertSame(expected, connection); assertEquals(1, removals.size()); assertTrue(gives.isEmpty()); calls.add("inventory.apply");
			return QuestTransactionParticipant.of(() -> calls.add("inventory.commit"), () -> calls.add("inventory.rollback"));
		}
	}

	private static final class RecordingCurrency implements QuestCurrencyPort {
		private final Connection expected;
		private final List<String> calls;
		private RecordingCurrency(Connection expected, List<String> calls) { this.expected = expected; this.calls = calls; }
		@Override public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction.GrantReward> rewards) {
			assertSame(expected, connection); assertEquals(1, rewards.size()); calls.add("currency.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			assertSame(expected, connection); assertEquals(1, rewards.size()); calls.add("currency.apply");
			return QuestTransactionParticipant.of(() -> calls.add("currency.commit"), () -> calls.add("currency.rollback"));
		}
	}

	private static final class RecordingRewards implements QuestRewardPort {
		private final Connection expected;
		private final List<String> calls;
		private RecordingRewards(Connection expected, List<String> calls) { this.expected = expected; this.calls = calls; }
		@Override public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction.GrantReward> rewards) {
			assertSame(expected, connection); assertEquals(1, rewards.size()); calls.add("reward.preflight");
		}
		@Override public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
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
}
