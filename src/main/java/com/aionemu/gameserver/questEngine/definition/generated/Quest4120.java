package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest4120 {
	private Quest4120() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(4120)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Instance/Group] The song Of death", 1137102, 46, 2147483647, Set.of("ASMODIANS"), "IMPORTANT", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 6937236L), new QuestReward("TITLE", 97, 1L), new QuestReward("ITEM", 186000019, 40L), new QuestReward("ITEM", 188053405, 4L), new QuestReward("SELECTABLE_ITEM", 112101614, 1L), new QuestReward("SELECTABLE_ITEM", 112301716, 1L), new QuestReward("SELECTABLE_ITEM", 112301718, 1L), new QuestReward("SELECTABLE_ITEM", 112501665, 1L), new QuestReward("SELECTABLE_ITEM", 112501667, 1L), new QuestReward("SELECTABLE_ITEM", 112601581, 1L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		builder.node("k1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205150, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214698)).from("started").goTo("k1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 31, 0)).from("k1").goTo("k1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 1009, 0)).from("k1").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 6937236L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 97, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000019, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053405, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 112101614, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 6937236L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 97, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000019, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053405, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 112301716, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 6937236L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 97, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000019, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053405, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 112301718, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 6937236L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 97, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000019, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053405, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 112501665, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 6937236L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 97, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000019, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053405, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 112501667, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205225, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 6937236L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 97, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000019, 40L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188053405, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 112601581, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
