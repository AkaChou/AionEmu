package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest20500 {
	private Quest20500() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(20500)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Enshar Expedition", 1800500, 55, 2147483647, Set.of("ASMODIANS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 22459785L), new QuestReward("ITEM", 186000231, 25L), new QuestReward("ITEM", 186000237, 60L), new QuestReward("ITEM", 188057352, 1L), new QuestReward("SELECTABLE_ITEM", 114101313, 1L), new QuestReward("SELECTABLE_ITEM", 114301319, 1L), new QuestReward("SELECTABLE_ITEM", 114301531, 1L), new QuestReward("SELECTABLE_ITEM", 114501277, 1L), new QuestReward("SELECTABLE_ITEM", 114501629, 1L), new QuestReward("SELECTABLE_ITEM", 114601219, 1L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 3, 0, 2, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.of()));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("unaccepted").when(new QuestCondition.WorldIs(220080000, true)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804718, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804718, 1012, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1012));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804718, 10255, 0)).from("started").then(new QuestAction.SetVariable("var0", 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
		builder.afterCommit(new AfterCommitAction.BroadcastZoneMissionEnd(new int[] {20501, 20502, 20503, 20504, 20505, 20506, 20507}));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 22459785L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 25L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 60L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188057352, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101313, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 22459785L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 25L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 60L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188057352, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301319, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 22459785L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 25L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 60L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188057352, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301531, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 22459785L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 25L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 60L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188057352, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501277, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 22459785L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 25L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 60L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188057352, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501629, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804719, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 22459785L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 25L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 60L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188057352, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601219, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
