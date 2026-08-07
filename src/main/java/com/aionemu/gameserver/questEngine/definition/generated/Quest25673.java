package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest25673 {
	private Quest25673() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(25673)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Weekly/Infiltration/Alliance] Astera Tower Fragment Retrieval Operation Cooperation", 1803270, 66, 2147483647, Set.of("ASMODIANS"), "QUEST", new RepeatPolicy(255, 0L, false, true), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 67580742L), new QuestReward("AP", 0, 5000L), new QuestReward("GP", 0, 200L), new QuestReward("ITEM", 188100391, 900L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of("WED"), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 25550, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 12, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("k1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("k2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("k3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("k4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("k5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 5))));
		builder.node("k6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 6))));
		builder.node("k7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 7))));
		builder.node("k8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 8))));
		builder.node("k9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 9))));
		builder.node("k10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 10))));
		builder.node("k11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 11))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 12))));
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
		addTransition33(builder);
		addTransition34(builder);
		addTransition35(builder);
		addTransition36(builder);
		addTransition37(builder);
		addTransition38(builder);
		addTransition39(builder);
		addTransition40(builder);
		addTransition41(builder);
		addTransition42(builder);
		addTransition43(builder);
		addTransition44(builder);
		addTransition45(builder);
		addTransition46(builder);
		addTransition47(builder);
		addTransition48(builder);
		addTransition49(builder);
		addTransition50(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.AtDistance(806116, null)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
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
		builder.on(new QuestEvent.TalkToNpc(806116, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806116, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.AtDistance(806672, null)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806672, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 67580742L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("AP", 0, 5000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("GP", 0, 200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188100391, 900L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("started").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k1").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k2").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k3").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k4").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k5").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k6").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k7").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k8").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k9").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k10").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("k11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillInWorld(210100000, null)).from("k11").when(new QuestCondition.PvpVictimLevelDelta(-5, 9)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("LF6_SensoryArea_Q25673")).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("k1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}
}
