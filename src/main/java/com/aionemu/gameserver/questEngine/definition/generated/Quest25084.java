package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest25084 {
	private Quest25084() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(25084)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Is This a Trap?", 1800859, 62, 2147483647, Set.of("ASMODIANS"), "IMPORTANT", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182215730, 10)), List.of(new QuestReward("GOLD", 0, 137760L), new QuestReward("EXP", 0, 9804928L), new QuestReward("ITEM", 186000231, 4L), new QuestReward("ITEM", 186000237, 30L), new QuestReward("ITEM", 162000124, 16L)), List.of(new QuestDrop(219750, 182215730, 100, true, 0, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 3, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 3))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4762));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804925, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804925, 39, 0)).from("started").priority(0).when(new QuestCondition.QuestVariableIs("var0", 0)).when(new QuestCondition.HasItem(182215730, 10, true)).then(new QuestAction.RemoveItem(182215730, 10)).then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804925, 39, 0)).from("started").priority(1).when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 31, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804926, 10001, 0)).from("s1").then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(220038)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("DF5_SENSORYAREA_Q25084_220080000")).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("trap1", 220038, new QuestSpawnLocation.Fixed(220080000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 1471.4f, 58.2f, 202.3f, (byte) 19)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("trap2", 220038, new QuestSpawnLocation.Fixed(220080000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 1475.5f, 58.3f, 202.3f, (byte) 53)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(804927, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 137760L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 9804928L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000231, 4L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000237, 30L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000124, 16L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
