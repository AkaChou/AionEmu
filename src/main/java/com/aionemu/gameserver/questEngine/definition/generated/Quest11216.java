package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest11216 {
	private Quest11216() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(11216)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("How Many Draks Does It Take To Map?", 1125142, 54, 2147483647, Set.of("ELYOS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182206827, 1), new QuestItemRequirement(182206828, 1), new QuestItemRequirement(182206829, 1), new QuestItemRequirement(182206830, 1)), List.of(new QuestReward("GOLD", 0, 74250L), new QuestReward("EXP", 0, 6240944L)), List.of(new QuestDrop(700624, 182206827, 100, true, 1, QuestDropScope.GROUP), new QuestDrop(700625, 182206828, 100, true, 1, QuestDropScope.GROUP), new QuestDrop(700626, 182206829, 100, true, 1, QuestDropScope.GROUP), new QuestDrop(700627, 182206830, 100, true, 1, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(new QuestItemRequirement(182206825, 1), new QuestItemRequirement(182206826, 1)), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var0", 2))));
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
		addTransition33(builder);
		addTransition34(builder);
		addTransition35(builder);
		addTransition36(builder);
		addTransition37(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.UseItem(182206825, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.QuestDialog(1002)).from("unaccepted").when(new QuestCondition.StartEligible()).then(new QuestAction.RemoveItem(182206825, 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.QuestDialog(1003)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 31, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 31, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 1012, 0)).from("started").goTo("v1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.CanAct(700624, "ACTION_ITEM_USE")).from("v1").goTo("v1");
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.CanAct(700625, "ACTION_ITEM_USE")).from("v1").goTo("v1");
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.CanAct(700626, "ACTION_ITEM_USE")).from("v1").goTo("v1");
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.CanAct(700627, "ACTION_ITEM_USE")).from("v1").goTo("v1");
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700624, -1, 0)).from("v1").priority(0).when(new QuestCondition.HasItem(182206827, 1, true)).goTo("v1");
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700624, -1, 0)).from("v1").priority(1).then(new QuestAction.GiveItem(182206827, 1)).goTo("v1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700625, -1, 0)).from("v1").priority(0).when(new QuestCondition.HasItem(182206828, 1, true)).goTo("v1");
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700625, -1, 0)).from("v1").priority(1).then(new QuestAction.GiveItem(182206828, 1)).goTo("v1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700626, -1, 0)).from("v1").priority(0).when(new QuestCondition.HasItem(182206829, 1, true)).goTo("v1");
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700626, -1, 0)).from("v1").priority(1).then(new QuestAction.GiveItem(182206829, 1)).goTo("v1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700627, -1, 0)).from("v1").priority(0).when(new QuestCondition.HasItem(182206830, 1, true)).goTo("v1");
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700627, -1, 0)).from("v1").priority(1).then(new QuestAction.GiveItem(182206830, 1)).goTo("v1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 39, 0)).from("v1").priority(0).when(new QuestCondition.HasItem(182206827, 1, true)).when(new QuestCondition.HasItem(182206828, 1, true)).when(new QuestCondition.HasItem(182206829, 1, true)).when(new QuestCondition.HasItem(182206830, 1, true)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 39, 0)).from("v1").priority(1).goTo("v1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10001));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799017, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 74250L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 6240944L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
