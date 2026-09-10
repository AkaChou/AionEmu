package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestConditionEvaluator;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证卡里加 20 个收藏品任务的阵营陈列柜和客户端对话合同。
 * Verifies the faction-scoped cabinet and client dialog contract for all 20 Kaliga collection quests.
 */
class QuestKaligaCollectionClientDialogAlignmentTest {
	private static final Path QUEST_DIR = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");
	private static final int KALIGA_KEY_ID = 185000102;
	private static final int KALIGA_BOSS_ID = 217006;
	private static final List<QuestCase> CASES = List.of(
		new QuestCase(18618, 730326, Race.ELYOS),
		new QuestCase(18619, 730327, Race.ELYOS),
		new QuestCase(18620, 730328, Race.ELYOS),
		new QuestCase(18621, 730329, Race.ELYOS),
		new QuestCase(18622, 730330, Race.ELYOS),
		new QuestCase(18623, 730331, Race.ELYOS),
		new QuestCase(18624, 730332, Race.ELYOS),
		new QuestCase(18625, 730333, Race.ELYOS),
		new QuestCase(18626, 730334, Race.ELYOS),
		new QuestCase(18627, 730335, Race.ELYOS),
		new QuestCase(28618, 730326, Race.ASMODIANS),
		new QuestCase(28619, 730327, Race.ASMODIANS),
		new QuestCase(28620, 730328, Race.ASMODIANS),
		new QuestCase(28621, 730329, Race.ASMODIANS),
		new QuestCase(28622, 730330, Race.ASMODIANS),
		new QuestCase(28623, 730331, Race.ASMODIANS),
		new QuestCase(28624, 730332, Race.ASMODIANS),
		new QuestCase(28625, 730333, Race.ASMODIANS),
		new QuestCase(28626, 730334, Race.ASMODIANS),
		new QuestCase(28627, 730335, Race.ASMODIANS));

	@Test
	void allFactionsUseTheirCabinetAndClientPages() throws Exception {
		for (QuestCase questCase : CASES) {
			QuestDefinition definition = load(questCase.questId()).definition();
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
			assertEquals(Set.of(questCase.race().name()), definition.metadata().permittedRaces());

			QuestTransition entry = route(definition, "started", "started",
				new QuestEvent.TalkToNpc(questCase.cabinetId(), QuestDialogAction.QUEST_SELECT.id()), null);
			assertEquals(List.of(new QuestCondition.PlayerRaceIs(questCase.race())), entry.conditions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
				entry.afterCommit());

			QuestSnapshot matchingRace = new QuestSnapshot(7, questCase.questId(), QuestStatus.START, 0, Map.of())
				.withRace(questCase.race());
			QuestSnapshot oppositeRace = matchingRace.withRace(opposite(questCase.race()));
			assertTrue(QuestConditionEvaluator.matches(definition.progressLayout(), matchingRace, entry.conditions()),
				"quest " + questCase.questId() + " must match its own faction");
			assertFalse(QuestConditionEvaluator.matches(definition.progressLayout(), oppositeRace, entry.conditions()),
				"quest " + questCase.questId() + " must reject the opposite faction");

			QuestEvent itemCheck = new QuestEvent.TalkToNpc(questCase.cabinetId(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
			QuestTransition success = route(definition, "started", "reward", itemCheck, 0);
			assertEquals(List.of(new QuestCondition.HasItem(KALIGA_KEY_ID, 1)), success.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(KALIGA_KEY_ID, 1)), success.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				success.afterCommit());

			QuestTransition failure = route(definition, "started", "started", itemCheck, 1);
			assertEquals(List.of(), failure.conditions());
			assertEquals(List.of(), failure.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())),
				failure.afterCommit());

			assertFalse(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& transition.event().equals(new QuestEvent.TalkToNpc(KALIGA_BOSS_ID,
						QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()))),
				"quest " + questCase.questId() + " must not check the key at Kaliga");
		}
	}

	private static Race opposite(Race race) {
		return race == Race.ELYOS ? Race.ASMODIANS : Race.ELYOS;
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
		QuestEvent event, Integer priority) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.filter(candidate -> priority == null || priority.equals(candidate.priority()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record QuestCase(int questId, int cabinetId, Race race) {
	}
}
