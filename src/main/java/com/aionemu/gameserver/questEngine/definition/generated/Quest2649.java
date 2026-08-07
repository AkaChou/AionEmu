package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest2649 {
	private Quest2649() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(2649)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Recovering The Keepsake", 1103949, 42, 2147483647, Set.of("ASMODIANS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182204486, 3), new QuestItemRequirement(182204487, 7)), List.of(new QuestReward("GOLD", 0, 51200L), new QuestReward("EXP", 0, 2815297L), new QuestReward("ITEM", 162000048, 5L), new QuestReward("ITEM", 186000010, 4L)), List.of(new QuestDrop(218939, 182204486, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(218940, 182204486, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(218941, 182204487, 80, true, 0, QuestDropScope.GROUP), new QuestDrop(218942, 182204487, 80, true, 0, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		addTransition25(builder);
		addTransition26(builder);
		addTransition27(builder);
		addTransition28(builder);
		addTransition29(builder);
		addTransition30(builder);
		addTransition31(builder);
		addTransition32(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 10000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 39, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182204486, 3, true)).when(new QuestCondition.HasItem(182204487, 7, true)).then(new QuestAction.RemoveItem(182204486, 3)).then(new QuestAction.RemoveItem(182204487, 7)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 39, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 20002, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182204486, 3, true)).when(new QuestCondition.HasItem(182204487, 7, true)).then(new QuestAction.RemoveItem(182204486, 3)).then(new QuestAction.RemoveItem(182204487, 7)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 20002, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 10255, 0)).from("started").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 51200L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 2815297L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000010, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
