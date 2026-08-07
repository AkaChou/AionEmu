package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest14051 {
	private Quest14051() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(14051)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Root of the Problem", 1129909, 36, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182215337, 3), new QuestItemRequirement(182215338, 3)), List.of(new QuestReward("EXP", 0, 4272047L), new QuestReward("SELECTABLE_ITEM", 100001744, 1L), new QuestReward("SELECTABLE_ITEM", 100101341, 1L), new QuestReward("SELECTABLE_ITEM", 100201510, 1L), new QuestReward("SELECTABLE_ITEM", 100501321, 1L), new QuestReward("SELECTABLE_ITEM", 100601438, 1L), new QuestReward("SELECTABLE_ITEM", 100901382, 1L), new QuestReward("SELECTABLE_ITEM", 101301274, 1L), new QuestReward("SELECTABLE_ITEM", 101501364, 1L), new QuestReward("SELECTABLE_ITEM", 101701375, 1L), new QuestReward("SELECTABLE_ITEM", 101801225, 1L), new QuestReward("SELECTABLE_ITEM", 101901134, 1L), new QuestReward("SELECTABLE_ITEM", 102001253, 1L), new QuestReward("SELECTABLE_ITEM", 102101079, 1L), new QuestReward("ITEM", 162000050, 40L), new QuestReward("ITEM", 186000004, 20L)), List.of(new QuestDrop(211078, 182215337, 100, true, 2, QuestDropScope.GROUP), new QuestDrop(211087, 182215337, 100, true, 2, QuestDropScope.GROUP), new QuestDrop(211079, 182215338, 100, true, 2, QuestDropScope.GROUP), new QuestDrop(211089, 182215338, 100, true, 2, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 14050, 0), new QuestStartCondition("unfinished", 1063, 0), new QuestStartCondition("noacquired", 1063, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 2, 0, 3, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("s3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var0", 3))));
	}

	private static void addTransitions(QuestDsl.QuestBuilder builder) {
		addTransitionBatch0(builder);
	}

	private static void addTransitionBatch0(QuestDsl.QuestBuilder builder) {
		addTransition0(builder);
		addTransition1(builder);
		addTransition2(builder);
		addTransition3(builder);
		addTransition4(builder);
		addTransition5(builder);
		addTransition6(builder);
		addTransition7(builder);
		addTransition8(builder);
		addTransition9(builder);
		addTransition10(builder);
		addTransition11(builder);
		addTransition12(builder);
		addTransition13(builder);
		addTransition14(builder);
		addTransition15(builder);
		addTransition16(builder);
		addTransition17(builder);
		addTransition18(builder);
		addTransition19(builder);
		addTransition20(builder);
		addTransition21(builder);
		addTransition22(builder);
		addTransition23(builder);
		addTransition24(builder);
		addTransition25(builder);
		addTransition26(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.ZoneMissionEnd()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204500, 31, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204500, 10000, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204549, 31, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204549, 39, 0)).from("s1").priority(0).when(new QuestCondition.QuestVariableIs("var0", 1)).when(new QuestCondition.HasItem(182215337, 3, true)).when(new QuestCondition.HasItem(182215338, 3, true)).then(new QuestAction.GiveItem(182215339, 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10000));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204549, 39, 0)).from("s1").priority(1).when(new QuestCondition.QuestVariableIs("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10001));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204549, 10001, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204549, 31, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204549, 10002, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730026, 31, 0)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730026, 10003, 0)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.RemoveItem(182215339, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100001744, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100101341, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100201510, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100501321, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100601438, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100901382, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101301274, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101501364, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101701375, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101801225, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101901134, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 102001253, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(730024, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4272047L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000004, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 102101079, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
