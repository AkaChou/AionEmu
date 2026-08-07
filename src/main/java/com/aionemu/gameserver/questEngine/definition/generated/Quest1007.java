package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest1007 {
	private Quest1007() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(1007)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("A Ceremony In Sanctum", 1102007, 10, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(1006), List.of(), List.of(new QuestReward("ITEM", 188057339, 1L)), List.of(), Set.of(), "", 0, 1, 1, true, true, false, 1, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		builder.node("s1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("s2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("reward10", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 10))));
		builder.node("reward20", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 20))));
		builder.node("reward30", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 30))));
		builder.node("reward40", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 40))));
		builder.node("reward50", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 50))));
		builder.node("reward60", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 60))));
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
		addTransition51(builder);
		addTransition52(builder);
		addTransition53(builder);
		addTransition54(builder);
		addTransition55(builder);
		addTransition56(builder);
		addTransition57(builder);
		addTransition58(builder);
		addTransition59(builder);
		addTransition60(builder);
		addTransition61(builder);
		addTransition62(builder);
		addTransition63(builder);
		addTransition64(builder);
		addTransition65(builder);
		addTransition66(builder);
		addTransition67(builder);
		addTransition68(builder);
		addTransition69(builder);
		addTransition70(builder);
		addTransition71(builder);
		addTransition72(builder);
		addTransition73(builder);
		addTransition74(builder);
		addTransition75(builder);
		addTransition76(builder);
		addTransition77(builder);
		addTransition78(builder);
		addTransition79(builder);
		addTransition80(builder);
		addTransition81(builder);
		addTransition82(builder);
		addTransition83(builder);
		addTransition84(builder);
		addTransition85(builder);
		addTransition86(builder);
		addTransition87(builder);
		addTransition88(builder);
		addTransition89(builder);
		addTransition90(builder);
		addTransition91(builder);
		addTransition92(builder);
		addTransition93(builder);
		addTransition94(builder);
		addTransition95(builder);
		addTransition96(builder);
		addTransition97(builder);
		addTransition98(builder);
		addTransition99(builder);
		addTransition100(builder);
		addTransition101(builder);
		addTransition102(builder);
		addTransition103(builder);
		addTransition104(builder);
		addTransition105(builder);
		addTransition106(builder);
		addTransition107(builder);
		addTransition108(builder);
		addTransition109(builder);
		addTransition110(builder);
		addTransition111(builder);
		addTransition112(builder);
		addTransition113(builder);
		addTransition114(builder);
		addTransition115(builder);
		addTransition116(builder);
		addTransition117(builder);
		addTransition118(builder);
		addTransition119(builder);
		addTransition120(builder);
		addTransition121(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(790001, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(790001, 10000, 0)).from("started").goTo("s1");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 110010000, 1313.0f, 1512.0f, 568.0f, (byte) 0));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203725, 31, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203725, 1353, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.PlayMovie(92));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203725, 10001, 0)).from("s1").goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 31, 0)).from("s2").goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 1694, 0)).from("s2").goTo("s2");
		builder.afterCommit(new AfterCommitAction.PlayMovie(91));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10002, 0)).from("s2").priority(1).when(new QuestCondition.PlayerClassIs(PlayerClass.WARRIOR)).goTo("reward10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10002, 0)).from("s2").priority(2).when(new QuestCondition.PlayerClassIs(PlayerClass.SCOUT)).goTo("reward20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10002, 0)).from("s2").priority(3).when(new QuestCondition.PlayerClassIs(PlayerClass.MAGE)).goTo("reward30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10002, 0)).from("s2").priority(4).when(new QuestCondition.PlayerClassIs(PlayerClass.PRIEST)).goTo("reward40");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10002, 0)).from("s2").priority(5).when(new QuestCondition.PlayerClassIs(PlayerClass.TECHNIST)).goTo("reward50");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203752, 10002, 0)).from("s2").priority(6).when(new QuestCondition.PlayerClassIs(PlayerClass.MUSE)).goTo("reward60");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, -1, 0)).from("reward10").goTo("reward10");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 1009, 0)).from("reward10").goTo("reward10");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 8, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 9, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 10, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 11, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 12, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 13, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 14, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 15, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 16, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 17, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 18, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 19, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 20, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 21, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 22, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203758, 23, 0)).from("reward10").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, -1, 0)).from("reward20").goTo("reward20");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 1009, 0)).from("reward20").goTo("reward20");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(6));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 8, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 9, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 10, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 11, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 12, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 13, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 14, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 15, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 16, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 17, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 18, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 19, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 20, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 21, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 22, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203759, 23, 0)).from("reward20").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(1)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, -1, 0)).from("reward30").goTo("reward30");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 1009, 0)).from("reward30").goTo("reward30");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(7));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 8, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 9, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 10, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 11, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 12, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 13, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 14, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 15, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 16, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 17, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 18, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 19, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 20, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 21, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 22, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203760, 23, 0)).from("reward30").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(2)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, -1, 0)).from("reward40").goTo("reward40");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3057));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 1009, 0)).from("reward40").goTo("reward40");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(8));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 8, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 9, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 10, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 11, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 12, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 13, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 14, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 15, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 16, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 17, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 18, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 19, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 20, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 21, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 22, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203761, 23, 0)).from("reward40").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(3)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, -1, 0)).from("reward50").goTo("reward50");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3398));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 1009, 0)).from("reward50").goTo("reward50");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(45));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 8, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 9, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 10, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 11, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 12, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 13, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 14, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 15, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 16, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 17, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 18, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 19, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 20, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 21, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 22, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801212, 23, 0)).from("reward50").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(4)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, -1, 0)).from("reward60").goTo("reward60");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3739));
	}

	private static void addTransition105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 1009, 0)).from("reward60").goTo("reward60");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(46));
	}

	private static void addTransition106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 8, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 9, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 10, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 11, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 12, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 13, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 14, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 15, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 16, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 17, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 18, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 19, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 20, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 21, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 22, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(801213, 23, 0)).from("reward60").then(new QuestAction.GrantReward("ITEM", 188057339, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(5)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
