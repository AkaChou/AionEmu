package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlyElyosQuestRegressionTest {
	@Test
	void ointmentAcceptanceKeepsTheWorkItemOnBothClientAcceptRoutes() {
		CompiledQuestDefinition definition = load(1118);

		for (int dialogId : List.of(1002, 20000)) {
			QuestTransition route = route(definition, "unaccepted", "v0",
				new QuestEvent.TalkToNpc(203059, dialogId));

			assertTrue(route.conditions().contains(new QuestCondition.StartEligible()));
			assertTrue(route.actions().contains(new QuestAction.GiveItem(182200224, 1)),
			"missing ointment for dialog " + dialogId);
			QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, 1118, QuestStatus.NONE, 0, Map.of())
					.withStartEligibility(QuestStartEligibility.allowed()),
				new QuestEvent.TalkToNpc(203059, dialogId), route).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus());
		}
	}

	@Test
	void ointmentDeliveryRequiresAndConsumesTheWorkItem() {
		CompiledQuestDefinition definition = load(1118);
		QuestEvent event = new QuestEvent.TalkToNpc(203079, 1009);
		QuestTransition delivery = route(definition, "v1", "reward", event);

		assertTrue(delivery.conditions().contains(new QuestCondition.HasItem(182200224, 1)));
		assertTrue(delivery.actions().contains(new QuestAction.RemoveItem(182200224, 1)));
		QuestSnapshot missingOintment = new QuestSnapshot(7, 1118, QuestStatus.START, 1, Map.of());
		assertFalse(QuestMutationPlanner.plan(definition, missingOintment, event, delivery).isPresent());

		QuestMutationPlan plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1118, QuestStatus.START, 1, Map.of(182200224, 1)),
			event, delivery).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(List.of(new QuestAction.RemoveItem(182200224, 1)), plan.requiredActions());
	}

	@Test
	void undeliveredArmourOpensTheShugoConversationBeforeTheTransferPage() {
		CompiledQuestDefinition definition = load(1131);
		QuestTransition route = route(definition, "started", "started",
			new QuestEvent.TalkToNpc(799093, 31));

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)), route.afterCommit());
		QuestTransition transfer = route(definition, "started", "shugo",
			new QuestEvent.TalkToNpc(799093, 10000));
		assertTrue(transfer.actions().contains(new QuestAction.GiveItem(182200507, 1)));
		assertTrue(transfer.actions().contains(new QuestAction.RemoveItem(182200506, 1)));
	}

	@Test
	void stolenVillageSealUsesTheItemStackOnlyAfterAcceptance() {
		CompiledQuestDefinition definition = load(1156);

		route(definition, "started", "started",
			new QuestEvent.CanAct(700003, "ACTION_ITEM_USE"));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)),
			route(definition, "started", "started", new QuestEvent.TalkToNpc(700003, -1)).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1353)),
			route(definition, "started", "started", new QuestEvent.TalkToNpc(700003, 1353)).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()),
			route(definition, "started", "k1", new QuestEvent.TalkToNpc(700003, 10000)).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
			route(definition, "k1", "k1", new QuestEvent.TalkToNpc(798003, 31)).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)),
			route(definition, "k1", "reward", new QuestEvent.TalkToNpc(798003, 1009)).afterCommit());
		route(definition, "reward", "complete", new QuestEvent.TalkToNpc(798003, 8));

		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("unaccepted")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& (talk.npcId() == 700003 || talk.npcId() == 798003)));
	}

	@Test
	void recoveredVillageSealUsesTheItemStackOnlyAfterAcceptance() {
		CompiledQuestDefinition definition = load(1158);

		assertObjectGate(definition, "started", 700003);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)),
			route(definition, "started", "started", new QuestEvent.TalkToNpc(700003, -1)).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1353)),
			route(definition, "started", "started", new QuestEvent.TalkToNpc(700003, 1353)).afterCommit());
		QuestTransition seal = route(definition, "started", "k1",
			new QuestEvent.TalkToNpc(700003, 10000));
		assertTrue(seal.actions().contains(new QuestAction.GiveItem(182200502, 1)));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), seal.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("started")
				&& transition.event().equals(new QuestEvent.TalkToNpc(700003, QuestDialogAction.QUEST_SELECT.id()))));
		assertNoUnacceptedObjectRoute(definition, 700003);
	}

	@Test
	void belbuasWineBarrelUsesTheObjectRouteOnlyAfterAcceptance() {
		CompiledQuestDefinition definition = load(1141);

		assertObjectGate(definition, "started", 700122);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
			route(definition, "started", "started", new QuestEvent.TalkToNpc(700122, -1)).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)),
			route(definition, "started", "reward", new QuestEvent.TalkToNpc(700122, 1009)).afterCommit());
		assertObjectGate(definition, "reward", 700122);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(5)),
			route(definition, "reward", "reward", new QuestEvent.TalkToNpc(700122, -1)).afterCommit());
		QuestTransition completion = route(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(700122, 8));
		assertEquals(new AfterCommitAction.CloseDialog(), completion.afterCommit().getLast());

		assertNoUnacceptedObjectRoute(definition, 700122);
	}

	@Test
	void weddingRingRestoresTheBarrelAndBothCollectorRewards() {
		CompiledQuestDefinition definition = load(1162);

		assertObjectGate(definition, "started", 700005);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(3739)),
			route(definition, "started", "started", new QuestEvent.TalkToNpc(700005, -1)).afterCommit());
		QuestTransition ring = route(definition, "started", "ring-found",
			new QuestEvent.TalkToNpc(700005, 10000));
		assertTrue(ring.conditions().contains(new QuestCondition.HasItem(182200563, 1, false)));
		assertTrue(ring.actions().contains(new QuestAction.GiveItem(182200563, 1)));
		assertEquals(new AfterCommitAction.CloseDialog(), ring.afterCommit().getLast());

		QuestTransition main = route(definition, "ring-found", "reward-main",
			new QuestEvent.TalkToNpc(203095, 39));
		QuestTransition alternate = route(definition, "ring-found", "reward-alternate",
			new QuestEvent.TalkToNpc(203093, 39));
		for (QuestTransition collector : List.of(main, alternate)) {
			assertTrue(collector.conditions().contains(new QuestCondition.HasItem(182200563, 1)));
			assertTrue(collector.actions().contains(new QuestAction.RemoveItem(182200563, 1)));
		}
		assertTrue(main.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(5)));
		assertTrue(alternate.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(6)));
		assertEquals(2, definition.definition().metadata().rewardGroups().size());
		assertTrue(route(definition, "reward-main", "complete", new QuestEvent.TalkToNpc(203095, 8))
			.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(route(definition, "reward-alternate", "complete", new QuestEvent.TalkToNpc(203093, 8))
			.actions().contains(new QuestAction.CompleteQuest(1)));
		assertNoUnacceptedObjectRoute(definition, 700005);
	}

	@Test
	void germAndWindmillObjectsRequireAndConsumeTheirWorkItems() {
		for (int[] expected : new int[][]{
			{1311, 203997, 700164, 182201305},
			{1414, 203989, 700175, 182201349}}) {
			int questId = expected[0];
			int startNpc = expected[1];
			int objectNpc = expected[2];
			int workItem = expected[3];
			CompiledQuestDefinition definition = load(questId);

			assertTrue(definition.definition().transitions().stream().anyMatch(transition ->
				transition.sourceNode().equals("unaccepted") && transition.targetNode().equals("started")
					&& transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == startNpc
					&& transition.actions().contains(new QuestAction.GiveItem(workItem, 1))));
			assertObjectGate(definition, "started", objectNpc);
			QuestTransition use = route(definition, "started", "reward",
				new QuestEvent.TalkToNpc(objectNpc, -1));
			assertTrue(use.conditions().contains(new QuestCondition.HasItem(workItem, 1)));
			assertTrue(use.actions().contains(new QuestAction.RemoveItem(workItem, 1)));
			assertTrue(use.afterCommit().contains(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));
			assertNoUnacceptedObjectRoute(definition, objectNpc);
		}
	}

	@Test
	void flowerDeliveryUnlocksTheShrineAndMiserChestCanReopenAtReward() {
		CompiledQuestDefinition flowers = load(1371);
		QuestTransition check = route(flowers, "started", "started",
			new QuestEvent.TalkToNpc(203949, 39));
		assertTrue(check.conditions().contains(new QuestCondition.HasItem(152000601, 5)));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1353)), check.afterCommit());
		QuestTransition delivery = route(flowers, "started", "flowers-delivered",
			new QuestEvent.TalkToNpc(203949, 10000));
		assertTrue(delivery.actions().contains(new QuestAction.RemoveItem(152000601, 5)));
		assertObjectGate(flowers, "flowers-delivered", 730039);
		route(flowers, "flowers-delivered", "reward", new QuestEvent.TalkToNpc(730039, -1));
		assertNoUnacceptedObjectRoute(flowers, 730039);

		CompiledQuestDefinition map = load(1561);
		for (String source : List.of("started", "reward")) {
			assertObjectGate(map, source, 700188);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
				route(map, source, source, new QuestEvent.TalkToNpc(700188, -1)).afterCommit());
		}
		assertTrue(route(map, "started", "reward", new QuestEvent.TalkToNpc(700188, 1009))
			.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(5)));
		assertEquals(new AfterCommitAction.CloseDialog(),
			route(map, "reward", "complete", new QuestEvent.TalkToNpc(700188, 8)).afterCommit().getLast());
	}

	@Test
	void lepharistObjectRequiresFourUsesAndRespawnsBetweenUses() {
		CompiledQuestDefinition definition = load(1612);
		List<String> sources = List.of("started", "used1", "used2", "used3");
		List<String> targets = List.of("used1", "used2", "used3", "reward");
		for (int i = 0; i < sources.size(); i++) {
			assertObjectGate(definition, sources.get(i), 700352);
			QuestTransition use = route(definition, sources.get(i), targets.get(i),
				new QuestEvent.TalkToNpc(700352, -1));
			assertEquals(new AfterCommitAction.DeleteInteractionNpc(true), use.afterCommit().getLast());
			QuestStateSyncMode mode = i == sources.size() - 1
				? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
			assertTrue(use.afterCommit().contains(new AfterCommitAction.SyncQuestState(mode)));
		}
		assertNoUnacceptedObjectRoute(definition, 700352);
	}

	@Test
	void pathLightsMustBeActivatedInOrderWithTheQuestItem() {
		CompiledQuestDefinition definition = load(1626);
		List<String> sources = List.of("started", "lit1", "lit2", "lit3", "lit4", "lit5", "lit6");
		List<String> targets = List.of("lit1", "lit2", "lit3", "lit4", "lit5", "lit6", "reward");
		for (int i = 0; i < sources.size(); i++) {
			int objectNpc = 700221 + i;
			assertObjectGate(definition, sources.get(i), objectNpc);
			QuestTransition use = route(definition, sources.get(i), targets.get(i),
				new QuestEvent.TalkToNpc(objectNpc, -1));
			assertTrue(use.conditions().contains(new QuestCondition.HasItem(182201788, 1)));
			QuestStateSyncMode mode = i == sources.size() - 1
				? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(mode)), use.afterCommit());
			assertNoUnacceptedObjectRoute(definition, objectNpc);
		}
	}

	@Test
	void bollvigStatueAndLeatherSlipperRestoreTheirLegacyGates() {
		CompiledQuestDefinition bollvig = load(1647);
		assertObjectGate(bollvig, "started", 700272);
		QuestTransition statue = route(bollvig, "started", "reward",
			new QuestEvent.TalkToNpc(700272, -1));
		assertTrue(statue.conditions().contains(new QuestCondition.HasItem(182201783, 1)));
		assertTrue(statue.conditions().contains(new QuestCondition.EquippedItem(110100150)));
		assertTrue(statue.conditions().contains(new QuestCondition.EquippedItem(113100144)));
		assertNoUnacceptedObjectRoute(bollvig, 700272);

		CompiledQuestDefinition slipper = load(1691);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)),
			route(slipper, "started", "started", new QuestEvent.TalkToNpc(790005, 31)).afterCommit());
		route(slipper, "started", "spoken-to-diana", new QuestEvent.TalkToNpc(790005, 10000));
		route(slipper, "spoken-to-diana", "returned-to-sneaker",
			new QuestEvent.TalkToNpc(798386, 10001));
		assertObjectGate(slipper, "returned-to-sneaker", 700563);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2034)),
			route(slipper, "returned-to-sneaker", "returned-to-sneaker",
				new QuestEvent.TalkToNpc(700563, -1)).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()),
			route(slipper, "returned-to-sneaker", "reward",
				new QuestEvent.TalkToNpc(700563, 10002)).afterCommit());
		assertNoUnacceptedObjectRoute(slipper, 700563);
	}

	@Test
	void fossilCollectionPublishesProgressButFinalNpcStillChecksBothItems() {
		CompiledQuestDefinition definition = load(1137);
		QuestTransition collection = definition.definition().transitions().stream()
			.filter(route -> route.sourceNode().equals("started")
				&& route.targetNode().equals("started")
				&& route.event().equals(new QuestEvent.CollectItem(182200513, 1)))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			collection.afterCommit());

		QuestTransition report = route(definition, "started", "reward",
			new QuestEvent.TalkToNpc(203111, 39));
		assertTrue(report.conditions().contains(new QuestCondition.HasItem(182200513, 1)));
		assertTrue(report.conditions().contains(new QuestCondition.HasItem(182200512, 1)));
		assertTrue(report.actions().contains(new QuestAction.RemoveItem(182200513, 1)));
		assertTrue(report.actions().contains(new QuestAction.RemoveItem(182200512, 1)));
	}

	@Test
	void nymphGownAlreadyHasTheDirectObjectHandoffAndRewardRoute() {
		CompiledQuestDefinition definition = load(1114);
		QuestTransition handoff = route(definition, "v2", "v3",
			new QuestEvent.TalkToNpc(203075, 2375));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(2375)), handoff.afterCommit());
		QuestTransition gown = route(definition, "v1", "v2",
			new QuestEvent.TalkToNpc(700008, -1));
		assertTrue(gown.actions().contains(new QuestAction.GiveItem(182200217, 1)));
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(route -> route.sourceNode().equals(source) && route.targetNode().equals(target)
				&& route.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertObjectGate(CompiledQuestDefinition definition, String source, int npcId) {
		QuestTransition gate = route(definition, source, source,
			new QuestEvent.CanAct(npcId, "ACTION_ITEM_USE"));
		assertEquals(List.of(), gate.actions());
		assertEquals(List.of(), gate.afterCommit());
	}

	private static void assertNoUnacceptedObjectRoute(CompiledQuestDefinition definition, int npcId) {
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("unaccepted")
				&& ((transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
					|| (transition.event() instanceof QuestEvent.CanAct canAct
						&& canAct.templateId() == npcId))));
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = EarlyElyosQuestRegressionTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new AssertionError("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("failed to load " + resource, e);
		}
	}
}
