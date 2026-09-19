package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 enter-zone 迁移任务必须保留 legacy 接取 owner 的合同。
 * Locks migrated enter-zone quests to their legacy start-owner contract.
 */
class QuestEnterZoneStartOwnerRegressionTest {
	private static final Map<Integer, QuestEvent> START_ROUTES = Map.ofEntries(
		Map.entry(1393, new QuestEvent.TalkToNpc(204041, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(14123, new QuestEvent.TalkToNpc(203933, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(15322, new QuestEvent.AtDistance(805330)),
		Map.entry(16800, new QuestEvent.TalkToNpc(806075, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())),
		Map.entry(17500, new QuestEvent.TalkToNpc(806262, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())),
		Map.entry(18300, new QuestEvent.TalkToNpc(804699, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(21080, new QuestEvent.TalkToNpc(799231, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(25322, new QuestEvent.AtDistance(805342)),
		Map.entry(27500, new QuestEvent.TalkToNpc(806264, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(28300, new QuestEvent.TalkToNpc(801904, QuestDialogAction.QUEST_ACCEPT_1.id())));

	@Test
	void affectedQuestsDoNotAutoStartOnEnterZone() throws Exception {
		for (int questId : START_ROUTES.keySet()) {
			QuestDefinition definition = load(questId).definition();
			List<QuestTransition> autoStarts = definition.transitions().stream()
				.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
				.filter(candidate -> candidate.event() instanceof QuestEvent.EnterZone)
				.filter(candidate -> targetStatus(definition, candidate.targetNode()) == QuestStatus.START)
				.toList();
			assertTrue(autoStarts.isEmpty(),
				() -> "quest " + questId + " still auto-starts on enter-zone: " + autoStarts);
		}
	}

	@Test
	void affectedQuestsExposeTheLegacyStartOwnerRoute() throws Exception {
		for (Map.Entry<Integer, QuestEvent> entry : START_ROUTES.entrySet()) {
			QuestDefinition definition = load(entry.getKey()).definition();
			QuestTransition start = definition.transitions().stream()
				.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
				.filter(candidate -> "started".equals(candidate.targetNode()))
				.filter(candidate -> entry.getValue().equals(candidate.event()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + entry.getKey()
					+ " is missing its legacy start route " + entry.getValue()));
			if (entry.getKey() == 21080) {
				assertTrue(start.actions().contains(new QuestAction.GiveItem(182207939, 1)),
					"quest 21080 start must grant the windstream letter");
			}
		}
	}

	private static QuestStatus targetStatus(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst()
			.orElseThrow()
			.projection()
			.status();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + questId + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
