package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定格尔克马洛斯任务链 select8_1 页的过期按钮符号修正：5.8 客户端按钮为 SETPRO8(10007)，
 * 旧 handler 的 STEP_TO_11(10010) 已无对应客户端入口；上交 182215591 进 REWARD 的副作用不变。
 * Locks the stale client button symbol fix for the Gelkmaros select8_1 page: the 5.8 client
 * button is SETPRO8(10007) while the legacy STEP_TO_11(10010) has no client entry; handing in
 * 182215591 into REWARD keeps its effects.
 */
class GelkmarosSelect8SymbolContractTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void select8_1ConfirmCompletesTheMissionThroughTheClientSymbol() throws Exception {
		// 20036 是 5.8 客户端禁用占位，正式规则由 20031 唯一承接。
		// 20036 is disabled in the Aion 5.8 client; 20031 is the sole production owner.
		for (int questId : List.of(20031)) {
			QuestDefinition definition;
			try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
				definition = QuestDefinitionXmlCompiler.compile(input).definition();
			}
			List<QuestTransition> confirm = definition.transitions().stream()
				.filter(transition -> transition.sourceNode().equals("s10")
					&& transition.targetNode().equals("reward")
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 799226 && talk.dialogId() == 10007)
				.toList();
			assertEquals(1, confirm.size(), "quest " + questId + " select8_1 confirm route count");
			QuestTransition route = confirm.getFirst();
			assertTrue(route.conditions().contains(new QuestCondition.QuestVariableIs("var0", 10)),
				"quest " + questId + " must require var0=10");
			assertTrue(route.actions().contains(new QuestAction.RemoveItem(182215591, 1)),
				"quest " + questId + " must hand in the Kuluma weapon shard");
			assertTrue(route.actions().contains(new QuestAction.SetVariable("var0", 11))
				&& route.actions().contains(new QuestAction.SetStatus(
					com.aionemu.gameserver.questEngine.model.QuestStatus.REWARD)),
				"quest " + questId + " must advance to var0=11 REWARD");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), route.afterCommit(),
				"quest " + questId + " confirm response");
			// 旧符号 STEP_TO_11(10010) 不再出现在任何路由中（客户端 select8_1 只有 10007 按钮）。
			// The stale STEP_TO_11(10010) symbol must not remain on any route.
			assertTrue(definition.transitions().stream().noneMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 799226 && talk.dialogId() == 10010),
				"quest " + questId + " must drop the stale 10010 symbol");
		}
	}
}
