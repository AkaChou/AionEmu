package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest10530 {
	private Quest10530() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(10530)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Trouble Back Home", 1803048, 66, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182216164, 1), new QuestItemRequirement(182216167, 1)), List.of(new QuestReward("GOLD", 0, 155160L), new QuestReward("EXP", 0, 39744000L), new QuestReward("SELECTABLE_ITEM", 160002510, 3L), new QuestReward("SELECTABLE_ITEM", 160002511, 3L), new QuestReward("SELECTABLE_ITEM", 160002512, 3L), new QuestReward("SELECTABLE_ITEM", 160002513, 3L), new QuestReward("SELECTABLE_ITEM", 160002514, 3L), new QuestReward("SELECTABLE_ITEM", 160002515, 3L)), List.of(new QuestDrop(703390, 182216164, 100, true, 0, QuestDropScope.GROUP), new QuestDrop(703387, 182216167, 100, true, 0, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 10529, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 9, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("s3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("s4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("s5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 5))));
		builder.node("s6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 6))));
		builder.node("s7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 7))));
		builder.node("s8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 8))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 8))));
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
		builder.on(new QuestEvent.TalkToNpc(203752, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 1012, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1012));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10000, 0)).from("started").then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203725, 31, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203725, 1353, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1353));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203725, 10001, 0)).from("s1").then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703386, -1, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798440, 31, 0)).from("s3").goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798440, 2035, 0)).from("s3").goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2035));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798440, 10003, 0)).from("s3").then(new QuestAction.SetVariable("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203852, 31, 0)).from("s4").goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203852, 2376, 0)).from("s4").goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2376));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203852, 10004, 0)).from("s4").then(new QuestAction.SetVariable("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806555, 31, 0)).from("s5").goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806555, 2717, 0)).from("s5").goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2717));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806555, 10005, 0)).from("s5").then(new QuestAction.SetVariable("var0", 6)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703394, -1, 0)).from("s6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("s7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703390, -1, 0)).from("s7").goTo("s7");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("burner", 703387, new QuestSpawnLocation.Fixed(110010000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 1540.9883f, 1457.1543f, 572.8622f, (byte) 0)));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703387, -1, 0)).from("s7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).then(new QuestAction.GiveItem(182216167, 1)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 31, 0)).from("s8").goTo("s8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3398));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 3399, 0)).from("s8").goTo("s8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3399));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 39, 0)).from("s8").priority(0).when(new QuestCondition.HasItem(182216164, 1, true)).when(new QuestCondition.HasItem(182216167, 1, true)).then(new QuestAction.RemoveItem(182216164, 1)).then(new QuestAction.RemoveItem(182216167, 1)).then(new QuestAction.SetVariable("var0", 8)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10000));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 39, 0)).from("s8").priority(1).goTo("s8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10001));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 1009, 0)).from("s8").then(new QuestAction.RemoveItem(182216164, 1)).then(new QuestAction.RemoveItem(182216167, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.PlayMovie(118));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 155160L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 39744000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002510, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002511, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002512, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002513, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002514, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("SELECTABLE_ITEM", 160002515, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
