package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest1494 {
	private Quest1494() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(1494)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Group] Korumonerk's Worry", 1102594, 38, 2147483647, Set.of("ELYOS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(1495), List.of(new QuestItemRequirement(182201414, 1), new QuestItemRequirement(186000028, 50)), List.of(new QuestReward("EXP", 0, 2108696L), new QuestReward("TITLE", 42, 1L), new QuestReward("SELECTABLE_ITEM", 110100876, 1L), new QuestReward("SELECTABLE_ITEM", 110300831, 1L), new QuestReward("SELECTABLE_ITEM", 110500801, 1L), new QuestReward("SELECTABLE_ITEM", 110600786, 1L), new QuestReward("SELECTABLE_ITEM", 110301523, 1L), new QuestReward("SELECTABLE_ITEM", 110551043, 1L), new QuestReward("ITEM", 162000048, 5L)), List.of(new QuestDrop(211039, 182201414, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(211864, 182201414, 10, true, 0, QuestDropScope.GROUP), new QuestDrop(211865, 182201414, 10, true, 0, QuestDropScope.GROUP), new QuestDrop(211019, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(211036, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(211020, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(211037, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(211818, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(211819, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212021, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212022, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(211035, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212025, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212029, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212033, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212036, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212039, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212042, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212045, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212048, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212051, 182201414, 1, true, 0, QuestDropScope.GROUP), new QuestDrop(212054, 182201414, 1, true, 0, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		builder.on(new QuestEvent.TalkToNpc(798151, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 10000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 39, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182201414, 1, true)).when(new QuestCondition.HasItem(186000028, 50, true)).then(new QuestAction.RemoveItem(182201414, 1)).then(new QuestAction.RemoveItem(186000028, 50)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 39, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 20002, 0)).from("started").priority(0).when(new QuestCondition.HasItem(182201414, 1, true)).when(new QuestCondition.HasItem(186000028, 50, true)).then(new QuestAction.RemoveItem(182201414, 1)).then(new QuestAction.RemoveItem(186000028, 50)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 20002, 0)).from("started").priority(1).goTo("started");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 10255, 0)).from("started").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 2108696L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 42, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110100876, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 2108696L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 42, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110300831, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 2108696L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 42, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110500801, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 2108696L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 42, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110600786, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 2108696L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 42, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110301523, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798151, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 2108696L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 42, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000048, 5L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110551043, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
