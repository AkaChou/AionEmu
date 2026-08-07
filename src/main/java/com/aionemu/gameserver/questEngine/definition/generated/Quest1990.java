package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest1990 {
	private Quest1990() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(1990)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("A Sage's Gift", 1102990, 30, 2147483647, Set.of("ELYOS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(new QuestItemRequirement(182206050, 150)), List.of(new QuestReward("EXP", 0, 874200L)), List.of(new QuestDrop(210821, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210845, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(211754, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(211651, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(211652, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210897, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210936, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210939, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210878, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210898, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210937, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210955, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210875, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210879, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210899, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210849, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210876, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210873, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210900, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210919, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210880, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210901, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210918, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210902, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210920, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210942, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210964, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210874, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210908, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210917, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(210928, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(211653, 182206050, 50, true, 0, QuestDropScope.GROUP), new QuestDrop(211041, 182206050, 50, true, 0, QuestDropScope.GROUP)), Set.of(), "", 0, 1, 1, true, false, false, 1, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 1989, 0)), Map.ofEntries(Map.entry("FIGHTER", List.of(new QuestReward("ITEM", 100200673, 1L), new QuestReward("ITEM", 100000723, 1L), new QuestReward("ITEM", 100100568, 1L), new QuestReward("ITEM", 100900554, 1L), new QuestReward("ITEM", 101300538, 1L))), Map.entry("KNIGHT", List.of(new QuestReward("ITEM", 100000723, 1L), new QuestReward("ITEM", 100100568, 1L), new QuestReward("ITEM", 100900554, 1L), new QuestReward("ITEM", 115000826, 1L))), Map.entry("RANGER", List.of(new QuestReward("ITEM", 100200673, 1L), new QuestReward("ITEM", 100000723, 1L), new QuestReward("ITEM", 101700594, 1L))), Map.entry("ASSASSIN", List.of(new QuestReward("ITEM", 100200673, 1L), new QuestReward("ITEM", 100000723, 1L), new QuestReward("ITEM", 101700594, 1L))), Map.entry("WIZARD", List.of(new QuestReward("ITEM", 100600608, 1L), new QuestReward("ITEM", 100500572, 1L))), Map.entry("ELEMENTALIST", List.of(new QuestReward("ITEM", 100600608, 1L), new QuestReward("ITEM", 100500572, 1L))), Map.entry("PRIEST", List.of(new QuestReward("ITEM", 100100568, 1L), new QuestReward("ITEM", 101500566, 1L), new QuestReward("ITEM", 115000826, 1L))), Map.entry("CHANTER", List.of(new QuestReward("ITEM", 100100568, 1L), new QuestReward("ITEM", 101500566, 1L), new QuestReward("ITEM", 115000826, 1L))), Map.entry("GUNSLINGER", List.of(new QuestReward("ITEM", 101800569, 1L), new QuestReward("ITEM", 101900562, 1L))), Map.entry("SONGWEAVER", List.of(new QuestReward("ITEM", 102000592, 1L))), Map.entry("AETHERTECH", List.of(new QuestReward("ITEM", 102100517, 1L)))));
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("step", 0, 3, 0, 3, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("abex", 3, 5, 0, 30, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("ettin", 8, 5, 0, 30, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("worg", 13, 5, 0, 30, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("hunt-complete", 18, 6, 0, 60, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("step", 0))));
		builder.node("started0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 0))));
		builder.node("started1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 1))));
		builder.node("started2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("step", 2))));
		builder.node("started2-complete", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("ettin", 30), Map.entry("worg", 30), Map.entry("abex", 30), Map.entry("step", 2), Map.entry("hunt-complete", 60))));
		builder.node("started3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("ettin", 30), Map.entry("worg", 30), Map.entry("abex", 30), Map.entry("step", 3), Map.entry("hunt-complete", 60))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("ettin", 30), Map.entry("worg", 30), Map.entry("abex", 30), Map.entry("step", 3), Map.entry("hunt-complete", 60))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("ettin", 30), Map.entry("worg", 30), Map.entry("abex", 30), Map.entry("step", 3), Map.entry("hunt-complete", 60))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 31, 0)).from("unaccepted").when(new QuestCondition.EquipmentSetEquipped(Set.of(378, 9, 8, 7, 6), 5, true)).goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4762));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 31, 0)).from("unaccepted").when(new QuestCondition.EquipmentSetEquipped(Set.of(378, 9, 8, 7, 6), 5, false)).goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4848));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 31, 0)).from("started0").goTo("started0");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 39, 0)).from("started0").priority(0).when(new QuestCondition.HasItem(182206050, 150, true)).then(new QuestAction.RemoveItem(182206050, 150)).goTo("started1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10000));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 39, 0)).from("started0").priority(1).goTo("started0");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10001));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10001, 0)).from("started1").goTo("started2");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(211870, 211869))).from("started2").priority(0).when(new QuestCondition.VariableBelow("abex", 30)).when(new QuestCondition.VariableAtLeast("ettin", 30)).when(new QuestCondition.VariableAtLeast("worg", 30)).then(new QuestAction.IncrementVariable("abex", 1)).then(new QuestAction.SetVariable("hunt-complete", 60)).goTo("started2-complete");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(211046)).from("started2").priority(0).when(new QuestCondition.VariableAtLeast("abex", 30)).when(new QuestCondition.VariableBelow("ettin", 30)).when(new QuestCondition.VariableAtLeast("worg", 30)).then(new QuestAction.IncrementVariable("ettin", 1)).then(new QuestAction.SetVariable("hunt-complete", 60)).goTo("started2-complete");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(211872, 211871))).from("started2").priority(0).when(new QuestCondition.VariableAtLeast("abex", 30)).when(new QuestCondition.VariableAtLeast("ettin", 30)).when(new QuestCondition.VariableBelow("worg", 30)).then(new QuestAction.IncrementVariable("worg", 1)).then(new QuestAction.SetVariable("hunt-complete", 60)).goTo("started2-complete");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(211870, 211869))).from("started2").priority(1).when(new QuestCondition.VariableBelow("abex", 30)).then(new QuestAction.IncrementVariable("abex", 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(211046)).from("started2").priority(1).when(new QuestCondition.VariableBelow("ettin", 30)).then(new QuestAction.IncrementVariable("ettin", 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(211872, 211871))).from("started2").priority(1).when(new QuestCondition.VariableBelow("worg", 30)).then(new QuestAction.IncrementVariable("worg", 1)).goTo("started2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(211870, 211869))).from("started2").priority(0).when(new QuestCondition.VariableAtLeast("abex", 30)).when(new QuestCondition.VariableAtLeast("ettin", 30)).when(new QuestCondition.VariableAtLeast("worg", 30)).then(new QuestAction.SetVariable("hunt-complete", 60)).goTo("started2-complete");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(211046)).from("started2").priority(0).when(new QuestCondition.VariableAtLeast("abex", 30)).when(new QuestCondition.VariableAtLeast("ettin", 30)).when(new QuestCondition.VariableAtLeast("worg", 30)).then(new QuestAction.SetVariable("hunt-complete", 60)).goTo("started2-complete");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(211872, 211871))).from("started2").priority(0).when(new QuestCondition.VariableAtLeast("abex", 30)).when(new QuestCondition.VariableAtLeast("ettin", 30)).when(new QuestCondition.VariableAtLeast("worg", 30)).then(new QuestAction.SetVariable("hunt-complete", 60)).goTo("started2-complete");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 31, 0)).from("started2-complete").goTo("started2-complete");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1693));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10002, 0)).from("started2-complete").then(new QuestAction.SetVariable("step", 3)).goTo("started3");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 31, 0)).from("started3").goTo("started3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 2035, 0)).from("started3").priority(0).when(new QuestCondition.DpAtMax()).when(new QuestCondition.HasItem(186000040, 1, true)).then(new QuestAction.RemoveItem(186000040, 1)).then(new QuestAction.SetCurrency(QuestRewardKind.DP, 0L)).goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 2035, 0)).from("started3").priority(1).goTo("started3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2120));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100200673, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100000723, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100100568, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 11, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100900554, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 12, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 101300538, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100000723, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100100568, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100900554, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 11, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.TEMPLAR)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 115000826, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100200673, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100000723, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.RANGER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 101700594, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100200673, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100000723, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.ASSASSIN)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 101700594, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100600608, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SORCERER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100500572, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100600608, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SPIRIT_MASTER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100500572, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100100568, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 101500566, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CLERIC)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 115000826, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 100100568, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 101500566, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 10, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.CHANTER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 115000826, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 101800569, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 9, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.GUNSLINGER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 101900562, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.SONGWEAVER)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 102000592, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203771, 8, 0)).from("reward").when(new QuestCondition.AdvancedClassIs(PlayerClass.AETHERTECH)).then(new QuestAction.GrantReward("EXP", 0, 874200L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 102100517, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
