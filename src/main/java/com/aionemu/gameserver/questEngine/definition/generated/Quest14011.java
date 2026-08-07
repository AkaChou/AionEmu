package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest14011 {
	private Quest14011() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(14011)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Fragments in the Sky", 1129873, 11, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 4230L), new QuestReward("EXP", 0, 34483L), new QuestReward("SELECTABLE_ITEM", 114101695, 1L), new QuestReward("SELECTABLE_ITEM", 114301816, 1L), new QuestReward("SELECTABLE_ITEM", 114301818, 1L), new QuestReward("SELECTABLE_ITEM", 114501725, 1L), new QuestReward("SELECTABLE_ITEM", 114501727, 1L), new QuestReward("SELECTABLE_ITEM", 114601574, 1L)), List.of(), Set.of("GLADIATOR", "SONGWEAVER", "RANGER", "SPIRIT_MASTER", "CHANTER", "TECHNIST", "TEMPLAR", "GUNSLINGER", "AETHERTECH", "SORCERER", "ASSASSIN", "CLERIC"), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("unfinished", 1020, 0), new QuestStartCondition("noacquired", 1020, 0)), Map.of());
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
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("s3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("s4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 4))));
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
		addTransition12(builder);
		addTransition13(builder);
		addTransition14(builder);
		addTransition15(builder);
		addTransition16(builder);
		addTransition17(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.ZoneMissionEnd()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).when(new QuestCondition.QuestsFinished(Set.of(14010))).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 31, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 10000, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203122, 31, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203122, 10001, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.PlayMovie(24));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(700091)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(700091)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(700091)).from("s4").when(new QuestCondition.QuestVariableIs("var0", 4)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 4230L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 34483L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 114101695, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 4230L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 34483L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 114301816, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 4230L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 34483L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 114301818, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 4230L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 34483L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 114501725, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 4230L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 34483L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 114501727, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 4230L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 34483L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 114601574, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203109, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 4230L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 34483L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
