package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest24053 {
	private Quest24053() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(24053)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("The Mauling of the Mau", 1129926, 42, 2147483647, Set.of("ASMODIANS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 12241520L), new QuestReward("ITEM", 162000050, 50L), new QuestReward("SELECTABLE_ITEM", 114101708, 1L), new QuestReward("SELECTABLE_ITEM", 114301841, 1L), new QuestReward("SELECTABLE_ITEM", 114301843, 1L), new QuestReward("SELECTABLE_ITEM", 114501750, 1L), new QuestReward("SELECTABLE_ITEM", 114501752, 1L), new QuestReward("SELECTABLE_ITEM", 114601587, 1L)), List.of(), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("unfinished", 2061, 0), new QuestStartCondition("noacquired", 2061, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 3, 0, 4, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("step1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("step2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("step3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("step4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 4))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.ZoneMissionEnd()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 1012, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 10000, 0)).from("started").goTo("step1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204795, 31, 0)).from("step1").goTo("step1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204795, 10001, 0)).from("step1").goTo("step2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 31, 0)).from("step2").goTo("step2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204796, 10002, 0)).from("step2").goTo("step3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 31, 0)).from("step1").goTo("step1");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 1012, 0)).from("step1").goTo("step1");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 31, 0)).from("step2").goTo("step2");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 1012, 0)).from("step2").goTo("step2");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 31, 0)).from("step3").goTo("step3");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 1012, 0)).from("step3").goTo("step3");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("DF3_SENSORYAREA_Q24053_220040000")).from("step3").goTo("step4");
		builder.afterCommit(new AfterCommitAction.PlayMovie(253));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 31, 0)).from("step4").goTo("step4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 1012, 0)).from("step4").goTo("step4");
		builder.afterCommit(new AfterCommitAction.PlayMovie(252));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 10255, 0)).from("step4").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204787, 10000, 0)).from("step4").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(204700, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 12241520L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000050, 50L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114101708, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301841, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114301843, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501750, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114501752, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 114601587, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
