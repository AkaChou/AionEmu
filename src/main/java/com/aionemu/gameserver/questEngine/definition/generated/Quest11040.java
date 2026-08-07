package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest11040 {
	private Quest11040() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(11040)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Squamp On The Cooking Plate", 1125040, 51, 2147483647, Set.of("ELYOS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 5647915L), new QuestReward("SELECTABLE_ITEM", 100001037, 1L), new QuestReward("SELECTABLE_ITEM", 100100782, 1L), new QuestReward("SELECTABLE_ITEM", 100200917, 1L), new QuestReward("SELECTABLE_ITEM", 100900779, 1L), new QuestReward("SELECTABLE_ITEM", 101300751, 1L), new QuestReward("SELECTABLE_ITEM", 101700824, 1L), new QuestReward("SELECTABLE_ITEM", 100500802, 1L), new QuestReward("SELECTABLE_ITEM", 100600857, 1L), new QuestReward("SELECTABLE_ITEM", 101500807, 1L), new QuestReward("SELECTABLE_ITEM", 101800681, 1L), new QuestReward("SELECTABLE_ITEM", 101900684, 1L), new QuestReward("SELECTABLE_ITEM", 102000718, 1L), new QuestReward("SELECTABLE_ITEM", 102100603, 1L), new QuestReward("ITEM", 162000062, 30L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 1, 0, 1, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("v1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 1))));
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
		addTransition23(builder);
		addTransition24(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799036, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4762));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799036, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799036, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799036, 31, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799036, 10000, 0)).from("started").goTo("v1");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("squamp", 799036, new QuestSpawnLocation.PlayerPosition((byte) 0)));
		builder.afterCommit(new AfterCommitAction.StartFollow("squamp"));
		builder.afterCommit(new AfterCommitAction.WatchFollowZone("squamp", "DIMAIA_FOUNTAINFALL_210050000"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.NpcReachTarget()).from("v1").goTo("reward");
		builder.afterCommit(new AfterCommitAction.DespawnNpc("squamp"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.NpcLostTarget()).from("v1").goTo("started");
		builder.afterCommit(new AfterCommitAction.DespawnNpc("squamp"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("v1").goTo("started");
		builder.afterCommit(new AfterCommitAction.DespawnNpc("squamp"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799035, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 5647915L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100001037, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100782, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200917, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900779, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300751, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700824, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500802, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600857, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500807, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
