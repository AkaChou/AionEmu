package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestCondition.HasItem;
import com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

class Quest1006InventoryGuardTest {
	@Test
	void preservesLegacyInventoryGuards() throws Exception {
		CompiledQuestDefinition definition = loadDefinition();

		QuestTransition giveAscensionItem = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof TalkToNpc talk
				&& talk.npcId() == 790001 && talk.dialogId() == 10000)
			.findFirst().orElseThrow();
		assertTrue(giveAscensionItem.conditions().contains(new HasItem(182200007, 1, false)));

		QuestTransition daminuReport = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof TalkToNpc talk
				&& talk.npcId() == 730008 && talk.dialogId() == 31)
			.findFirst().orElseThrow();
		assertTrue(daminuReport.conditions().contains(new HasItem(182200008, 1, true)));
	}

	@Test
	void reentersInstanceAfterLogoutConsumedEssence() throws Exception {
		CompiledQuestDefinition definition = loadDefinition();
		TalkToNpc enterInstance = new TalkToNpc(790001, 10002);
		List<QuestTransition> entryRoutes = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("s3")
				&& QuestEvent.matches(transition.event(), enterInstance))
			.toList();
		assertEquals(2, entryRoutes.size());

		int s3 = definition.definition().progressLayout().pack(Map.of("var0", 3));
		int s99 = definition.definition().progressLayout().pack(Map.of("var0", 99));
		QuestMutationPlan firstEntry = firstMatchingPlan(definition, entryRoutes,
			new QuestSnapshot(7, 1006, QuestStatus.START, s3, Map.of(182200009, 1)), enterInstance);
		assertEquals(s99, firstEntry.nextPackedVariables());
		assertTrue(firstEntry.requiredActions().contains(new QuestAction.RemoveItem(182200009, 1)));

		QuestEvent.EnterWorld enterWorld = new QuestEvent.EnterWorld();
		List<QuestTransition> recoveryRoutes = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("s99")
				&& QuestEvent.matches(transition.event(), enterWorld))
			.toList();
		QuestSnapshot outsideAfterLogout = new QuestSnapshot(7, 1006, QuestStatus.START, s99,
			Map.of(), Map.of(), true, true, 0, 0, 210010000, 1, 0f, 0f, 0f, (byte) 0);
		QuestMutationPlan recovered = firstMatchingPlan(definition, recoveryRoutes, outsideAfterLogout, enterWorld);
		assertEquals(s3, recovered.nextPackedVariables());

		QuestMutationPlan reentry = firstMatchingPlan(definition, entryRoutes,
			new QuestSnapshot(7, 1006, QuestStatus.START, recovered.nextPackedVariables(), Map.of()),
			enterInstance);
		assertEquals(s99, reentry.nextPackedVariables());
		assertFalse(reentry.requiredActions().stream().anyMatch(QuestAction.RemoveItem.class::isInstance));
		assertEquals(firstEntry.afterCommit(), reentry.afterCommit());
		assertTrue(reentry.afterCommit().stream().anyMatch(AfterCommitAction.TeleportPlayer.class::isInstance));
		assertTrue(reentry.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	private static CompiledQuestDefinition loadDefinition() throws Exception {
		try (InputStream input = Quest1006InventoryGuardTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/1006.xml")) {
			assertNotNull(input);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestMutationPlan firstMatchingPlan(CompiledQuestDefinition definition,
			List<QuestTransition> routes, QuestSnapshot snapshot, QuestEvent event) {
		return routes.stream()
			.map(transition -> QuestMutationPlanner.plan(definition, snapshot, event, transition))
			.flatMap(result -> result.stream())
			.findFirst().orElseThrow();
	}
}
