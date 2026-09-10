package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证生产任务的自动接取入口不打开接受任务窗口。
 * Verifies that production automatic-start entries do not open the quest-accept window.
 */
class QuestAutoStartDialogAuditTest {
	@Test
	void automaticStartRoutesOnlyRefreshVisibilityWithoutQuestDialog() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest_definition"));
		int checked = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			Map<String, QuestStatus> statuses = compiled.definition().nodes().stream()
				.collect(java.util.stream.Collectors.toMap(QuestNode::label, node -> node.projection().status()));
			for (QuestTransition transition : compiled.definition().transitions()) {
				if (!isAutomaticStart(transition, statuses)) {
					continue;
				}
				checked++;
				String context = "quest=" + compiled.id() + " event="
					+ transition.event().getClass().getSimpleName() + " source=" + transition.sourceNode()
					+ " target=" + transition.targetNode();
				assertTrue(transition.conditions().contains(new QuestCondition.StartEligible()), context);
				assertTrue(transition.afterCommit().contains(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), context);
				assertFalse(transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), context);
			}
		}
		assertTrue(checked > 0, "production catalog must contain automatic-start routes");
	}

	private static boolean isAutomaticStart(QuestTransition transition, Map<String, QuestStatus> statuses) {
		boolean automaticEvent = transition.event() instanceof QuestEvent.LevelUp
			|| transition.event() instanceof QuestEvent.ZoneMissionEnd;
		return automaticEvent
			&& statuses.get(transition.sourceNode()) == QuestStatus.NONE
			&& statuses.get(transition.targetNode()) == QuestStatus.START;
	}
}
