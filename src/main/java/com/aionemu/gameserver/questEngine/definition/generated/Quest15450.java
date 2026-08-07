package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest15450 {
	private Quest15450() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(15450)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Urgent Order] Mongros' Request", 1801767, 70, 2147483647, Set.of("ELYOS"), "PUBLIC", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 53023500L), new QuestReward("GP", 0, 12L), new QuestReward("ITEM", 186000414, 3L), new QuestReward("ITEM", 188100391, 100L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 2, 0, 2, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("k1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 2))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.AtDistance(805393, null)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(400010000, null)).from("started").goTo("k1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(400010000, null)).from("k1").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(805393, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 53023500L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 12L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000414, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
