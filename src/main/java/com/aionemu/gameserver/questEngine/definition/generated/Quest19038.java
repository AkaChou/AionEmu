package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest19038 {
	private Quest19038() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(19038)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Master] Cook's Potential", 1124538, 29, 2147483647, Set.of("ELYOS"), "SIGNIFICANT", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 291412L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, 40001, 499, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(new QuestItemRequirement(152202200, 1), new QuestItemRequirement(152202201, 1), new QuestItemRequirement(152202202, 1), new QuestItemRequirement(152202203, 1)), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 1944, 0), new QuestStartCondition("unfinished", 19008, 0), new QuestStartCondition("noacquired", 19008, 0), new QuestStartCondition("finished", 3952, 0), new QuestStartCondition("unfinished", 19014, 0), new QuestStartCondition("noacquired", 19014, 0), new QuestStartCondition("unfinished", 19020, 0), new QuestStartCondition("noacquired", 19020, 0), new QuestStartCondition("unfinished", 19026, 0), new QuestStartCondition("noacquired", 19026, 0), new QuestStartCondition("unfinished", 19032, 0), new QuestStartCondition("noacquired", 19032, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 4, 0, 11, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
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
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 11))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4762));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("started").priority(1).when(new QuestCondition.QuestVariableIs("var0", 0)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s1").priority(1).when(new QuestCondition.QuestVariableIs("var0", 1)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s3").priority(1).when(new QuestCondition.QuestVariableIs("var0", 3)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s4").priority(1).when(new QuestCondition.QuestVariableIs("var0", 4)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s6").priority(1).when(new QuestCondition.QuestVariableIs("var0", 6)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s7").priority(1).when(new QuestCondition.QuestVariableIs("var0", 7)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s7");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s9").priority(1).when(new QuestCondition.QuestVariableIs("var0", 9)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s9");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s10").priority(1).when(new QuestCondition.QuestVariableIs("var0", 10)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s10");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("started").priority(0).when(new QuestCondition.QuestVariableIs("var0", 0)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s3").priority(0).when(new QuestCondition.QuestVariableIs("var0", 3)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s6").priority(0).when(new QuestCondition.QuestVariableIs("var0", 6)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s9").priority(0).when(new QuestCondition.QuestVariableIs("var0", 9)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).goTo("s9");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s1").priority(0).when(new QuestCondition.QuestVariableIs("var0", 1)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).when(new QuestCondition.RecipeKnown(155002239, false)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4081));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s4").priority(0).when(new QuestCondition.QuestVariableIs("var0", 4)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).when(new QuestCondition.RecipeKnown(155002240, false)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4166));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s7").priority(0).when(new QuestCondition.QuestVariableIs("var0", 7)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).when(new QuestCondition.RecipeKnown(155002241, false)).goTo("s7");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4251));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 31, 0)).from("s10").priority(0).when(new QuestCondition.QuestVariableIs("var0", 10)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).when(new QuestCondition.RecipeKnown(155002242, false)).goTo("s10");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4336));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10009, 0)).from("started").priority(0).when(new QuestCondition.QuestVariableIs("var0", 0)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202200, 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10009, 0)).from("s1").priority(0).when(new QuestCondition.QuestVariableIs("var0", 1)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202200, 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10009, 0)).from("started").priority(1).when(new QuestCondition.QuestVariableIs("var0", 0)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10009, 0)).from("s1").priority(1).when(new QuestCondition.QuestVariableIs("var0", 1)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10019, 0)).from("s3").priority(0).when(new QuestCondition.QuestVariableIs("var0", 3)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202201, 1)).then(new QuestAction.SetVariable("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10019, 0)).from("s4").priority(0).when(new QuestCondition.QuestVariableIs("var0", 4)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202201, 1)).then(new QuestAction.SetVariable("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10019, 0)).from("s3").priority(1).when(new QuestCondition.QuestVariableIs("var0", 3)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10019, 0)).from("s4").priority(1).when(new QuestCondition.QuestVariableIs("var0", 4)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10029, 0)).from("s6").priority(0).when(new QuestCondition.QuestVariableIs("var0", 6)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202202, 1)).then(new QuestAction.SetVariable("var0", 8)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10029, 0)).from("s7").priority(0).when(new QuestCondition.QuestVariableIs("var0", 7)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202202, 1)).then(new QuestAction.SetVariable("var0", 8)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10029, 0)).from("s6").priority(1).when(new QuestCondition.QuestVariableIs("var0", 6)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10029, 0)).from("s7").priority(1).when(new QuestCondition.QuestVariableIs("var0", 7)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s7");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10039, 0)).from("s9").priority(0).when(new QuestCondition.QuestVariableIs("var0", 9)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202203, 1)).then(new QuestAction.SetVariable("var0", 11)).goTo("s11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10039, 0)).from("s10").priority(0).when(new QuestCondition.QuestVariableIs("var0", 10)).when(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 6500L)).then(new QuestAction.GiveItem(152202203, 1)).then(new QuestAction.SetVariable("var0", 11)).goTo("s11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10039, 0)).from("s9").priority(1).when(new QuestCondition.QuestVariableIs("var0", 9)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s9");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203785, 10039, 0)).from("s10").priority(1).when(new QuestCondition.QuestVariableIs("var0", 10)).when(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 6500L)).goTo("s10");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4400));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 31, 0)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1097));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 31, 0)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1438));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 31, 0)).from("s8").when(new QuestCondition.QuestVariableIs("var0", 8)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1779));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 31, 0)).from("s11").when(new QuestCondition.QuestVariableIs("var0", 11)).goTo("s11");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2120));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10010, 0)).from("s2").priority(0).when(new QuestCondition.QuestVariableIs("var0", 2)).when(new QuestCondition.HasItem(182206773, 1, true)).then(new QuestAction.RemoveItem(182206773, 1)).then(new QuestAction.SetVariable("var0", 3)).goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1182));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10010, 0)).from("s2").priority(1).when(new QuestCondition.QuestVariableIs("var0", 2)).goTo("s2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2716));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10020, 0)).from("s5").priority(0).when(new QuestCondition.QuestVariableIs("var0", 5)).when(new QuestCondition.HasItem(182206774, 1, true)).then(new QuestAction.RemoveItem(182206774, 1)).then(new QuestAction.SetVariable("var0", 6)).goTo("s6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1523));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10020, 0)).from("s5").priority(1).when(new QuestCondition.QuestVariableIs("var0", 5)).goTo("s5");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3057));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10030, 0)).from("s8").priority(0).when(new QuestCondition.QuestVariableIs("var0", 8)).when(new QuestCondition.HasItem(182206775, 1, true)).then(new QuestAction.RemoveItem(182206775, 1)).then(new QuestAction.SetVariable("var0", 9)).goTo("s9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1864));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10030, 0)).from("s8").priority(1).when(new QuestCondition.QuestVariableIs("var0", 8)).goTo("s8");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3398));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10040, 0)).from("s11").priority(0).when(new QuestCondition.QuestVariableIs("var0", 11)).when(new QuestCondition.HasItem(182206776, 1, true)).then(new QuestAction.RemoveItem(182206776, 1)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10040, 0)).from("s11").priority(1).when(new QuestCondition.QuestVariableIs("var0", 11)).goTo("s11");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(3057));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.FailCraft(182206773)).from("s2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 1)).goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.FailCraft(182206774)).from("s5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 4)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.FailCraft(182206775)).from("s8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 7)).goTo("s7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.FailCraft(182206776)).from("s11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 10)).goTo("s10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203784, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 291412L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
