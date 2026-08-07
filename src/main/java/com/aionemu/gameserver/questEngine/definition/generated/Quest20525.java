package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest20525 {
	private Quest20525() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(20525)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Agent Peregrine's Call", 1802610, 66, 2147483647, Set.of("ASMODIANS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182216081, 20), new QuestItemRequirement(182216082, 20), new QuestItemRequirement(182216083, 5)), List.of(new QuestReward("EXP", 0, 124274952L), new QuestReward("ITEM", 188055380, 1L), new QuestReward("ITEM", 162000181, 10L)), List.of(new QuestDrop(241508, 182216081, 100, true, 4, QuestDropScope.GROUP), new QuestDrop(241509, 182216081, 100, true, 4, QuestDropScope.GROUP), new QuestDrop(241496, 182216082, 100, true, 4, QuestDropScope.GROUP), new QuestDrop(241497, 182216082, 100, true, 4, QuestDropScope.GROUP), new QuestDrop(244117, 182216083, 100, true, 4, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(new QuestItemRequirement(182216084, 1), new QuestItemRequirement(182216085, 1)), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 20521, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 6, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var1", 4, 4, 0, 15, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
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
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 6))));
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
		addTransition48(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.ZoneMissionEnd()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 31, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 1012, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1012));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 10000, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 31, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1353));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 1354, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1354));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 10001, 0)).from("s1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806228, 31, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1694));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806228, 1695, 0)).from("s2").priority(0).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.QuestVariableIs("var1", 14)).then(new QuestAction.SetVariable("var1", 0)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806228, 1695, 0)).from("s2").priority(1).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.VariableBelow("var1", 14)).then(new QuestAction.IncrementVariable("var1", 1)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806229, 31, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1779));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806229, 1780, 0)).from("s2").priority(0).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.QuestVariableIs("var1", 13)).then(new QuestAction.SetVariable("var1", 0)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806229, 1780, 0)).from("s2").priority(1).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.VariableBelow("var1", 13)).then(new QuestAction.IncrementVariable("var1", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806230, 31, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1864));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806230, 1865, 0)).from("s2").priority(0).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.QuestVariableIs("var1", 11)).then(new QuestAction.SetVariable("var1", 0)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806230, 1865, 0)).from("s2").priority(1).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.VariableBelow("var1", 11)).then(new QuestAction.IncrementVariable("var1", 4)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806231, 31, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1949));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806231, 1950, 0)).from("s2").priority(0).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.QuestVariableIs("var1", 7)).then(new QuestAction.SetVariable("var1", 0)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806231, 1950, 0)).from("s2").priority(1).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.VariableBelow("var1", 7)).then(new QuestAction.IncrementVariable("var1", 8)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 31, 0)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2036));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 2037, 0)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2037));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 10003, 0)).from("s3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 31, 0)).from("s4").when(new QuestCondition.QuestVariableIs("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 2376, 0)).from("s4").when(new QuestCondition.QuestVariableIs("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2376));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 39, 0)).from("s4").priority(0).when(new QuestCondition.QuestVariableIs("var0", 4)).when(new QuestCondition.HasItem(182216081, 20, true)).when(new QuestCondition.HasItem(182216082, 20, true)).when(new QuestCondition.HasItem(182216083, 5, true)).then(new QuestAction.SetVariable("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10000));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 39, 0)).from("s4").priority(1).when(new QuestCondition.QuestVariableIs("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10001));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 31, 0)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 2717, 0)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2717));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806135, 10005, 0)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.GiveItem(182216084, 1)).then(new QuestAction.SetVariable("var0", 6)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.UseItem(182216084, 0)).from("s6").when(new QuestCondition.ZoneIs("DF6_ITEMUSEAREA_Q20525", true)).when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.RemoveItem(182216084, 1)).then(new QuestAction.GiveItem(182216085, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806079, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 124274952L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188055380, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 162000181, 10L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
