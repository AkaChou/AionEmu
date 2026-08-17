package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 锁定任务 28821 的管家报告与无奖励领取动作仅在 REWARD 状态生效。
 * Locks quest 28821 so butler reporting and no-reward completion are active only in the REWARD state.
 */
class Quest28821DialogRouteRegressionTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/28821.xml");
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	private static final Set<Integer> BUTLERS = Set.of(810022, 810023, 810024, 810025, 810026);

	@Test
	void clientVisibleStartPagesReportWithAction1009InsteadOfAction23() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		assertEquals(List.of(1009), oracle.visibleActions(28821, 1003).stream()
			.map(ClientResourceOracle.ClientAction::actionId).toList());
		assertEquals(List.of(1009), oracle.visibleActions(28821, 2375).stream()
			.map(ClientResourceOracle.ClientAction::actionId).toList());
		assertFalse(List.of(1011, 1003, 1004, 2375, 5, 9, 1008, 12, 11).stream()
			.flatMap(page -> oracle.visibleActions(28821, page).stream())
			.anyMatch(action -> action.actionId() == 23));
	}

	@Test
	void noRewardActionCannotBeClaimedBeforeRewardState() throws Exception {
		QuestDefinition definition;
		try (InputStream input = Files.newInputStream(XML)) {
			definition = QuestDefinitionXmlCompiler.compile(input).definition();
		}

		assertFalse(definition.transitions().stream().anyMatch(transition ->
			"started".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 23));
		assertEquals(BUTLERS, definition.transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 1009)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
		assertEquals(BUTLERS, definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 23)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}
}
