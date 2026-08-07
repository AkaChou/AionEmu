package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest25563 {
	private Quest25563() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(25563)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Weekly] Broken Guardian Accomodations", 1802196, 66, 2147483647, Set.of("ASMODIANS"), "SEEN_MARKER", new RepeatPolicy(255, 0L, false, true), Set.of(), List.of(new QuestItemRequirement(182215977, 1)), List.of(new QuestReward("GOLD", 0, 465480L), new QuestReward("EXP", 0, 117914968L), new QuestReward("GP", 0, 300L), new QuestReward("ITEM", 188100391, 100L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of("WED"), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 25550, 0)), Map.of());
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.UseItem(182215977, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).then(new QuestAction.SetVariable("var0", 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 8, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 9, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 10, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 11, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 12, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 13, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 14, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 15, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 16, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 17, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 18, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 19, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 20, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 21, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 22, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 23, 0)).from("reward").then(new QuestAction.RemoveItem(182215977, 1)).then(new QuestAction.GrantReward("GOLD", 0, 465480L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 117914968L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 300L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 100L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
