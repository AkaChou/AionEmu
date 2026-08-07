package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest2394 {
	private Quest2394() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(2394)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("A Dying Wish", 1103594, 36, 2147483647, Set.of("ASMODIANS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 31580L), new QuestReward("EXP", 0, 1496758L), new QuestReward("TITLE", 64, 1L), new QuestReward("SELECTABLE_ITEM", 100001050, 1L), new QuestReward("SELECTABLE_ITEM", 100100795, 1L), new QuestReward("SELECTABLE_ITEM", 100200930, 1L), new QuestReward("SELECTABLE_ITEM", 100900792, 1L), new QuestReward("SELECTABLE_ITEM", 101300764, 1L), new QuestReward("SELECTABLE_ITEM", 101700837, 1L), new QuestReward("SELECTABLE_ITEM", 100500815, 1L), new QuestReward("SELECTABLE_ITEM", 100600870, 1L), new QuestReward("SELECTABLE_ITEM", 101500820, 1L), new QuestReward("SELECTABLE_ITEM", 101800694, 1L), new QuestReward("SELECTABLE_ITEM", 101900697, 1L), new QuestReward("SELECTABLE_ITEM", 102000731, 1L), new QuestReward("SELECTABLE_ITEM", 102100612, 1L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 2393, 0)), Map.of());
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
		builder.node("v1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var0", 1))));
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
		addTransition27(builder);
		addTransition28(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4762));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.UseItem(182204130, 0)).from("started").then(new QuestAction.RemoveItem(182204130, 1)).then(new QuestAction.GiveItem(182204131, 1)).goTo("started");
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204381, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204381, 10000, 0)).from("started").goTo("v1");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("orlan", 790021, new QuestSpawnLocation.PlayerPosition((byte) 8)));
		builder.afterCommit(new AfterCommitAction.StartWalking("orlan"));
		builder.afterCommit(new AfterCommitAction.StartFollow("orlan"));
		builder.afterCommit(new AfterCommitAction.BroadcastNpcEmotion("orlan", QuestNpcEmotion.START_EMOTE2));
		builder.afterCommit(new AfterCommitAction.WatchFollowZone("orlan", "HALABANA_HOT_SPRINGS_220020000"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.NpcReachTarget()).from("v1").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.NpcLostTarget()).from("v1").goTo("started");
		builder.afterCommit(new AfterCommitAction.DespawnNpc("orlan"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("v1").goTo("started");
		builder.afterCommit(new AfterCommitAction.DespawnNpc("orlan"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LogOut(null)).from("v1").goTo("started");
		builder.afterCommit(new AfterCommitAction.DespawnNpc("orlan"));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100001050, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100100795, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100200930, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100900792, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101300764, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101700837, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100500815, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 100600870, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101500820, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101800694, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 101900697, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 102000731, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204343, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 31580L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 1496758L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 64, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 102100612, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
