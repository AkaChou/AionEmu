package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.ItemStorage;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.closeDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end shadow capture: pre-event snapshots are frozen before any owner
 * runs, the legacy invocation is bound to the authoritative event, and capture
 * itself never changes the legacy route or result.
 */
class QuestShadowCaptureTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void captureFreezesPreEventSnapshotBeforeHandlerMutatesState() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);

		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD));
		}

		assertEquals(1, capture.envelopes().size());
		QuestShadowBatchRunner.Envelope envelope = capture.envelopes().get(0);
		assertEquals(event, envelope.event());
		assertEquals(Set.of(QUEST_ID), envelope.observation().owners().keySet());
		assertEquals(PLAYER_ID, envelope.snapshots().get(QUEST_ID).playerId());
		// snapshot is pre-event, not the post-handler REWARD
		QuestSnapshot pre = envelope.snapshots().get(QUEST_ID);
		assertEquals(QuestStatus.START, pre.status());
		assertEquals(0, pre.packedVariables());
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
	}

	@Test
	void captureDeduplicatesOwnerSnapshotsAndBindsOneInvocation() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);

		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID, QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
			});
		}

		assertEquals(1, capture.envelopes().size());
		assertEquals(1, capture.envelopes().get(0).snapshots().size());
	}

	@Test
	void recordWithoutAnOpenScopeProducesNoBinding() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();

		runLegacyOwner(player, capture, () -> {
		});

		assertEquals(0, capture.envelopes().size());
	}

	@Test
	void handlerExceptionStillBindsFailedInvocationAndKeepsLegacyThrow() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);

		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			assertThrows(IllegalStateException.class,
				() -> runLegacyOwner(player, capture, () -> {
					throw new IllegalStateException("owner failure");
				}));
		}

		assertEquals(1, capture.envelopes().size());
		QuestShadowObservation.Owner owner = capture.envelopes().get(0).observation().owners().get(QUEST_ID);
		assertEquals(QuestRouteResult.FAILED, owner.result());
		assertFalse(owner.conditionMatched());
	}

	@Test
	void captureWithNoCandidateOwnersStillLetsLegacyRunUnchanged() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);

		boolean handled;
		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of())) {
			handled = runLegacyOwner(player, capture, () -> player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD));
		}

		assertTrue(handled);
		assertEquals(1, capture.envelopes().size());
		assertEquals(0, capture.envelopes().get(0).snapshots().size());
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
	}

	@Test
	void batchReportFromCaptureIsStableAndCleanForAMatchingDefinition() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));

		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
				QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
				state.setStatus(QuestStatus.REWARD);
				state.setQuestVar(1);
			});
		}

		QuestShadowBatchReport report = capture.report(runner, Set.of(QUEST_ID));
		assertTrue(report.complete());
		assertTrue(report.clean());
		assertEquals(Map.of(), report.differenceCounts());
	}

	@Test
	void protocolOnlyUnacceptedPathIsCleanWithoutLegacyQuestState() throws Exception {
		Player player = emptyPlayer();
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);
		CompiledQuestDefinition unaccepted = QuestDsl.quest(QUEST_ID)
			.evidence(new EvidenceRef("test", "shadow-capture", "unaccepted protocol fixture"))
			.node("unaccepted", project(QuestStatus.NONE, Map.of()))
			.on(talkToNpc(700001)).when(statusIs(QuestStatus.NONE)).goTo("unaccepted")
			.afterCommit(closeDialog()).compile();
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(unaccepted)));

		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			QuestLegacyInvocationBridge bridge = new QuestLegacyInvocationBridge(capture);
			bridge.invoke(player, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
				() -> {
					QuestLegacyObservationContext.afterCommitAction(QUEST_ID,
						new com.aionemu.gameserver.questEngine.definition.AfterCommitAction.CloseDialog());
					return true;
				}, (handled, stateChanged, recorder) -> QuestRouteResult.HANDLED);
		}

		QuestShadowBatchReport report = capture.report(runner, Set.of(QUEST_ID));
		assertTrue(report.complete());
		assertTrue(report.clean());
		assertEquals(Map.of(), report.differenceCounts());
	}

	@Test
	void batchReportWithUnmatchedLegacyShowsResultConsumptionDifference() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));

		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
			}, false);
		}

		QuestShadowBatchReport report = capture.report(runner, Set.of(QUEST_ID));
		assertFalse(report.complete());
		assertFalse(report.clean());
		// 玩家持有 START/step=0 状态,候选条件一致;legacy 返回 NOT_HANDLED
		// 且未改变状态，因此这条候选路径没有获得 legacy 匹配证明。
		assertEquals(1, report.differenceCounts().get(QuestShadowDifferenceKind.CONDITION));
		assertEquals(2, report.differenceCounts().get(QuestShadowDifferenceKind.RESULT_CONSUMPTION));
		assertEquals(1, report.missingCoverage().size());
	}

	@Test
	void drainClearsBindingsSoTheNextBatchStartsFresh() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));

		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
			});
		}
		QuestShadowBatchReport first = capture.drain(runner, Set.of(QUEST_ID));
		assertEquals(1, first.actualInvocations());
		assertEquals(0, capture.envelopes().size());

		// A second physical event accumulates into a fresh batch, not on top of the first.
		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
			});
		}
		QuestShadowBatchReport second = capture.drain(runner, Set.of(QUEST_ID));
		assertEquals(1, second.actualInvocations());
		assertEquals(0, capture.envelopes().size());
	}

	@Test
	void configuredOwnersFreezeStartEligibilityBeforeLegacyExecution() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture(
			(playerId, questId) -> QuestStartEligibility.allowed(), Set.of(QUEST_ID));

		try (QuestShadowCapture.Scope scope = capture.open(player, talkToNpc(700001), List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
			});
		}

		QuestSnapshot frozen = capture.envelopes().get(0).snapshots().get(QUEST_ID);
		assertTrue(frozen.startEligibility().eligible());
	}

	@Test
	void repeatedOwnerFiringCannotFillTheOwnerGate() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestShadowCapture capture = new QuestShadowCapture();
		QuestEvent event = talkToNpc(700001);
		QuestShadowRunner runner = new QuestShadowRunner(new ImmutableQuestCatalog(List.of(definition())));

		// 同一 owner 触发两次
		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
			});
		}
		try (QuestShadowCapture.Scope scope = capture.open(player, event, List.of(QUEST_ID))) {
			runLegacyOwner(player, capture, () -> {
			});
		}

		// 期望覆盖 1001 与 1002;重复触发 1001 不能填满 1002。
		QuestShadowBatchReport report = capture.report(runner, Set.of(QUEST_ID, 1002));
		assertFalse(report.complete());
		assertEquals(Set.of(1002), report.missingOwners());
		assertEquals(Set.of(QUEST_ID), report.coveredOwners());
		assertEquals(1, report.actualInvocations());
	}

	@Test
	void snapshotProjectsInventoryCountsAndCurrencyBalances() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		setField(Player.class, player, "inventory", inventoryWith(
			item(169001001, 2), item(169001001, 1), item(169001002, 3)));

		QuestSnapshot snapshot = QuestShadowCapture.snapshotOf(player, QUEST_ID);
		assertEquals(3, snapshot.itemCount(169001001));
		assertEquals(3, snapshot.itemCount(169001002));
		assertEquals(0, snapshot.itemCount(169001003));
		// 未提供货币存储/军衔 → 空余额,而不是伪造 0
		assertEquals(Map.of(), snapshot.currencies());
	}

	@Test
	void snapshotWithoutInventoryAndRankCapturesStateOnly() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		QuestSnapshot snapshot = QuestShadowCapture.snapshotOf(player, QUEST_ID);
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of(), snapshot.inventory());
		assertEquals(Map.of(), snapshot.currencies());
	}

	@Test
	void snapshotNullInventoryDegradesToEmptyFacts() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		// inventory 字段为 null(模拟登出中的玩家)
		setField(Player.class, player, "inventory", null);
		QuestSnapshot snapshot = QuestShadowCapture.snapshotOf(player, QUEST_ID);
		assertEquals(Map.of(), snapshot.inventory());
		assertEquals(Map.of(), snapshot.currencies());
	}

	@Test
	void snapshotDistinguishesKnownZeroFromUncapturedFacts() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		setField(Player.class, player, "inventory", null);

		QuestSnapshot uncaptured = QuestShadowCapture.snapshotOf(player, QUEST_ID);
		// 未采集:itemCount/balance fail-closed,绝不返回伪造的 0
		assertFalse(uncaptured.inventoryCaptured());
		assertThrows(IllegalStateException.class, () -> uncaptured.itemCount(169001001));
		assertThrows(IllegalStateException.class, () -> uncaptured.balance(QuestRewardKind.GOLD));

		// 已采集但为空:已知为 0,与未采集严格区分
		QuestSnapshot knownZero = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of());
		assertTrue(knownZero.inventoryCaptured());
		assertEquals(0, knownZero.itemCount(169001001));
	}

	private static Item item(int itemId, int count) throws Exception {
		ItemTemplate template = new ObjenesisStd().newInstance(ItemTemplate.class);
		setField(ItemTemplate.class, template, "itemId", itemId);
		Item item = new ObjenesisStd().newInstance(Item.class);
		setField(Item.class, item, "itemTemplate", template);
		setField(Item.class, item, "itemCount", count);
		return item;
	}

	private static Storage inventoryWith(Item... items) throws Exception {
		ItemStorage itemStorage = new ObjenesisStd().newInstance(ItemStorage.class);
		Map<Integer, Item> slots = new java.util.LinkedHashMap<>();
		int slot = 1;
		for (Item item : items) {
			slots.put(slot++, item);
		}
		setField(ItemStorage.class, itemStorage, "items", slots);
		PlayerStorage storage = new ObjenesisStd().newInstance(PlayerStorage.class);
		setField(Storage.class, storage, "itemStorage", itemStorage);
		return storage;
	}

	private static CompiledQuestDefinition definition() {
		return QuestDsl.quest(QUEST_ID)
				.evidence(new EvidenceRef("test", "shadow-capture", "fixture"))
				.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("step", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(variableIs("step", 0))
					.then(setVariable("step", 1)).goTo("reward")
				.compile();
	}

	/**
	 * Simulates one legacy owner invocation. The capture instance is the bridge
	 * sink, so the invocation produced while a capture scope is open is bound to
	 * the authoritative event and pre-event snapshots; outside a scope it lands
	 * in the store only.
	 */
	private static boolean runLegacyOwner(Player player, QuestShadowCapture capture, Runnable mutation) {
		return runLegacyOwner(player, capture, mutation, true);
	}

	private static boolean runLegacyOwner(Player player, QuestShadowCapture capture, Runnable mutation, boolean handled) {
		QuestLegacyInvocationBridge bridge = new QuestLegacyInvocationBridge(capture);
		return bridge.invoke(player, QUEST_ID, "TALK_TO_NPC", QuestDispatchContract.EXCLUSIVE,
			() -> {
				mutation.run();
				return handled;
			}, (value, stateChanged, recorder) -> value ? QuestRouteResult.HANDLED : QuestRouteResult.NOT_HANDLED);
	}

	private static Player playerWithState(QuestStatus status, int packedVariables) throws Exception {
		Player player = emptyPlayer();
		QuestStateList states = new QuestStateList();
		states.addQuest(QUEST_ID, new QuestState(QUEST_ID, status, packedVariables, 0,
				(Timestamp) null, null, null));
		setField(Player.class, player, "questStateList", states);
		return player;
	}

	private static Player emptyPlayer() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
