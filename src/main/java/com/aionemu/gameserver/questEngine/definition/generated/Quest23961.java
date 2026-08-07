package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest23961 {
	private Quest23961() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(23961)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Instanced Dungeon] Main Mission to Retrieve the Core Fragment", 1803250, 66, 2147483647, Set.of("ASMODIANS"), "SEEN_MARKER", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 232740L), new QuestReward("EXP", 0, 13262400L), new QuestReward("ITEM", 188100391, 100L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var0", 0))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("unaccepted").when(new QuestCondition.WorldIs(302350000, true)).when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835222, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835222, 1012, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.PlayMovie(963));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1012));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835222, 1013, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1013));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835222, 10255, 0)).from("started").then(new QuestAction.SetStatus(QuestStatus.REWARD)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(835224, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 232740L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 13262400L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
