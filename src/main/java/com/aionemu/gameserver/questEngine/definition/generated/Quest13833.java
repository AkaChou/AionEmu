package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest13833 {
	private Quest13833() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(13833)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Stigmore", 1801122, 50, 2147483647, Set.of("ELYOS"), "PRIMARY", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 731094L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 1, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(new QuestItemRequirement(182216121, 1)), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		builder.node("started", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 0))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var0", 0))));
	}

	private static void addTransitions(QuestDsl.QuestBuilder builder) {
		addTransitionBatch0(builder);
		addTransitionBatch1(builder);
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
		addTransition122(builder);
		addTransition123(builder);
		addTransition124(builder);
		addTransition125(builder);
		addTransition126(builder);
		addTransition127(builder);
	}

	private static void addTransitionBatch1(QuestDsl.QuestBuilder builder) {
		addTransition128(builder);
		addTransition129(builder);
		addTransition130(builder);
		addTransition131(builder);
		addTransition132(builder);
		addTransition133(builder);
		addTransition134(builder);
		addTransition135(builder);
		addTransition136(builder);
		addTransition137(builder);
		addTransition138(builder);
		addTransition139(builder);
		addTransition140(builder);
		addTransition141(builder);
		addTransition142(builder);
		addTransition143(builder);
		addTransition144(builder);
		addTransition145(builder);
		addTransition146(builder);
		addTransition147(builder);
		addTransition148(builder);
		addTransition149(builder);
		addTransition150(builder);
		addTransition151(builder);
		addTransition152(builder);
		addTransition153(builder);
		addTransition154(builder);
		addTransition155(builder);
		addTransition156(builder);
		addTransition157(builder);
		addTransition158(builder);
		addTransition159(builder);
		addTransition160(builder);
		addTransition161(builder);
		addTransition162(builder);
		addTransition163(builder);
		addTransition164(builder);
		addTransition165(builder);
		addTransition166(builder);
		addTransition167(builder);
		addTransition168(builder);
		addTransition169(builder);
		addTransition170(builder);
		addTransition171(builder);
		addTransition172(builder);
		addTransition173(builder);
		addTransition174(builder);
		addTransition175(builder);
		addTransition176(builder);
		addTransition177(builder);
		addTransition178(builder);
		addTransition179(builder);
		addTransition180(builder);
		addTransition181(builder);
		addTransition182(builder);
		addTransition183(builder);
		addTransition184(builder);
		addTransition185(builder);
		addTransition186(builder);
		addTransition187(builder);
		addTransition188(builder);
		addTransition189(builder);
		addTransition190(builder);
		addTransition191(builder);
		addTransition192(builder);
		addTransition193(builder);
		addTransition194(builder);
		addTransition195(builder);
		addTransition196(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).then(new QuestAction.GiveItem(182216121, 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("started").when(new QuestCondition.WorldIs(110010000, true)).when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.GiveItem(182216121, 1)).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.UseItem(182216121, 0)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(0).when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("ITEM", 140001103, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(1).when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("ITEM", 140001124, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(2).when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("ITEM", 140001156, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(3).when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("ITEM", 140001137, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(4).when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("ITEM", 140001176, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(5).when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("ITEM", 140001197, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(6).when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("ITEM", 140001228, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(7).when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("ITEM", 140001214, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(8).when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("ITEM", 140001247, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(9).when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("ITEM", 140001282, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition171(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition172(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition173(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition174(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition175(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition176(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition177(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition178(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition179(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition180(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(10).when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("ITEM", 140001265, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition181(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 8, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition182(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 9, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition183(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 10, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition184(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 11, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition185(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 12, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition186(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 13, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition187(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 14, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition188(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 15, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition189(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 16, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition190(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 17, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition191(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 18, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition192(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 19, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition193(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 20, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition194(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 21, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition195(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 22, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition196(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203711, 23, 0)).from("reward").priority(11).then(new QuestAction.GrantReward("EXP", 0, 731094L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.RemoveItem(182216121, -1)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
