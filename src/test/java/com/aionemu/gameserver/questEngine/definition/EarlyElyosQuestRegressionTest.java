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
