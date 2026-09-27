package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest2110WorkItemRegressionTest {
	@Test
	void retailQuestHasNoWorkItemAndAcceptsWithoutGiving182203110() {
		// 客户端/真实证据:quest.xml 2110 块 work-item 的 itemId=0、count=0,
		// 接取不发 182203110,reward 转移无 has-item/remove-item。
		CompiledQuestDefinition definition = load();
		assertEquals(List.of(), definition.definition().metadata().questWorkItems());

		List<QuestTransition> starts = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203522 && transition.sourceNode().equals("unaccepted")
				&& transition.targetNode().equals("started"))
			.toList();
		assertEquals(2, starts.size());
		assertTrue(starts.stream().noneMatch(transition -> transition.actions()
			.contains(new QuestAction.GiveItem(182203110, 1))));

		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）。真端行 2110 只有
		// acquired=Kaindal / reward=Motgar、零 item_check（Quest_SimpleTalk.xml:2789-2792；奖励 =
		// quest.xml:26993-26995 的 exp + 单道具，一档），因此交付 = QUEST_SELECT(31) 空门直翻领奖态
		// 并下发第 1 档奖励窗；报告页 SELECT5(2375) 与 1009 中转随页链退场。
		// P0-3 S1: the canonical delivery is QUEST_SELECT(31) with an empty gate flipping REWARD and
		// showing the first reward window; the SELECT5(2375) report page and the 1009 hop are gone.
		QuestTransition delivery = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("started")
				&& transition.targetNode().equals("reward")
				&& transition.event().equals(new QuestEvent.TalkToNpc(203533, 31)))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD,
			definition.definition().nodes().stream().filter(node -> node.label().equals("reward"))
			.findFirst().orElseThrow().projection().status());
		assertTrue(delivery.conditions().stream()
			.noneMatch(QuestCondition.HasItem.class::isInstance));
		assertTrue(delivery.actions().stream()
			.noneMatch(action -> action instanceof QuestAction.RemoveItem remove
				&& remove.itemId() == 182203110));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			delivery.afterCommit(), "canonical delivery shows the first reward window");
		// P0c-23 裁定（真端对、XML 错）+ P0-3 S1：报告流不再经 select5 页与 1009 中转——遗留 XML 的
		// started→started 1009 自环（npc-report 双 prio 手工形）与 select5 报告页一并退役；
		// reportNpcExit 关窗 CloseDialog 保持。
		// P0c-23 adjudication + P0-3 S1: the report flow no longer goes through the select5 page or the
		// 1009 hop; the legacy dual-priority self-loop and the report page are retired alike, while the
		// reportNpcExit CloseDialog stays.
		assertTrue(definition.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("started") && transition.targetNode().equals("started")
				&& transition.event().equals(new QuestEvent.TalkToNpc(203533, 1008))
				&& transition.afterCommit().equals(List.of(new AfterCommitAction.CloseDialog()))),
			"report NPC close ends the dialog");
		assertTrue(definition.definition().transitions().stream().noneMatch(transition ->
			transition.sourceNode().equals("started") && transition.targetNode().equals("started")
				&& transition.event().equals(new QuestEvent.TalkToNpc(203533, 1009))),
			"no started 1009 fallback loop on the report flow");
		assertTrue(definition.definition().transitions().stream()
			.flatMap(transition -> transition.afterCommit().stream())
			.noneMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog dialog
				&& dialog.dialogId() == QuestDialogPage.SELECT5.id()),
			"canonical removed the select5 report page");
	}

	private static CompiledQuestDefinition load() {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(2110);
	}
}
