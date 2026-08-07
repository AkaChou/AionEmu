package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest10527 {
	private Quest10527() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(10527)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Finding the Traces of the Sage", 1802607, 68, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182216105, 1)), List.of(new QuestReward("EXP", 0, 147934512L), new QuestReward("ITEM", 188055377, 1L), new QuestReward("ITEM", 162000181, 20L)), List.of(new QuestDrop(244108, 182216105, 100, true, 12, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(new QuestItemRequirement(182216075, 1)), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 10526, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 15, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var1", 4, 4, 0, 6, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
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
		builder.node("s9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 9))));
		builder.node("s10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 10))));
		builder.node("s11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 11))));
		builder.node("s12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 12))));
		builder.node("s13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 13))));
		builder.node("s14", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 14))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 14))));
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
		addTransition45(builder);
		addTransition46(builder);
		addTransition47(builder);
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
		builder.on(new QuestEvent.TalkToNpc(806075, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 1012, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1012));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 10000, 0)).from("started").then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806291, 31, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1355));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806291, 1356, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1356));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806291, 10001, 0)).from("s1").then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 31, 0)).from("s2").goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1695));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 1696, 0)).from("s2").goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1696));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 1009, 0)).from("s2").then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("FALLOW_RUINS_210100000")).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703313, -1, 0)).from("s4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731705, -1, 0)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("TARHA_KRALL_VILLAGE_210100000")).from("s6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("s7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703314, -1, 0)).from("s7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(731706, -1, 0)).from("s8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("s9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterZone("VALLEY_OF_THE_WAYWARD_210100000")).from("s9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("s10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(703315, -1, 0)).from("s10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("s11");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("vine1", 244107, new QuestSpawnLocation.Fixed(210100000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 2130.926f, 2857.0728f, 246.58539f, (byte) 106)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("vine2", 244107, new QuestSpawnLocation.Fixed(210100000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 2140.2065f, 2846.4036f, 246.90082f, (byte) 61)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("vine3", 244107, new QuestSpawnLocation.Fixed(210100000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 2136.1633f, 2837.4336f, 246.85143f, (byte) 51)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("vine4", 244107, new QuestSpawnLocation.Fixed(210100000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 2115.5115f, 2826.012f, 246.7515f, (byte) 117)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("vine5", 244107, new QuestSpawnLocation.Fixed(210100000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 2104.703f, 2824.9727f, 249.81421f, (byte) 75)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("vine6", 244107, new QuestSpawnLocation.Fixed(210100000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 2122.6865f, 2793.037f, 251.90955f, (byte) 87)));
		builder.afterCommit(new AfterCommitAction.SpawnNpc("vine7", 244107, new QuestSpawnLocation.Fixed(210100000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 2103.6562f, 2800.0583f, 252.26054f, (byte) 3)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(244107)).from("s11").priority(1).when(new QuestCondition.VariableBelow("var1", 6)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("s11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(244107)).from("s11").priority(0).when(new QuestCondition.QuestVariableIs("var1", 6)).then(new QuestAction.SetVariable("var0", 12)).goTo("s12");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("sage", 244108, new QuestSpawnLocation.PlayerPosition((byte) 0)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 31, 0)).from("s12").goTo("s12");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(7182));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 7183, 0)).from("s12").goTo("s12");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(7183));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 39, 0)).from("s12").priority(0).when(new QuestCondition.HasItem(182216105, 1, true)).then(new QuestAction.RemoveItem(182216105, 1)).then(new QuestAction.SetVariable("var0", 13)).goTo("s13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10000));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 39, 0)).from("s12").priority(1).goTo("s12");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10001));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 31, 0)).from("s13").goTo("s13");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(7523));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 7524, 0)).from("s13").goTo("s13");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(7524));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 10013, 0)).from("s13").then(new QuestAction.SetVariable("var0", 14)).then(new QuestAction.GiveItem(182216075, 1)).goTo("s14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.UseItem(182216075, 0)).from("s14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.RemoveItem(182216075, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806075, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 147934512L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055377, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 20L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
