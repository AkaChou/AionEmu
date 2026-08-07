package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest30113 {
	private Quest30113() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(30113)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Group] Fan Flames Of Vengeance", 1114245, 53, 2147483647, Set.of("ASMODIANS"), "IMPORTANT", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 53570L), new QuestReward("EXP", 0, 8798808L), new QuestReward("SELECTABLE_ITEM", 167000518, 2L), new QuestReward("SELECTABLE_ITEM", 167000555, 2L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var1", 6, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var2", 12, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 3), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 4), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 5), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 6), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 7), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 8), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 9), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0c10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 10), Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 3), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 4), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 5), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 6), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 7), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 8), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 9), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 10), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a1b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 3), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 4), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 5), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 6), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 7), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 8), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 9), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 10), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 3), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 4), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 5), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 6), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 7), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 8), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 9), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 10), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var2", 10), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 0))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1008, 0)).from("a0b0c0").goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c0").goTo("a1b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c1").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c2").goTo("a1b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c3").goTo("a1b0c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c4").goTo("a1b0c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c5").goTo("a1b0c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c6").goTo("a1b0c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c7").goTo("a1b0c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c8").goTo("a1b0c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c9").goTo("a1b0c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b0c10").goTo("a1b0c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c2").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c3").goTo("a1b1c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c4").goTo("a1b1c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c5").goTo("a1b1c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c6").goTo("a1b1c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c7").goTo("a1b1c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c8").goTo("a1b1c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c9").goTo("a1b1c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(215786, 246009))).from("a0b1c10").goTo("a1b1c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c2").goTo("a0b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c3").goTo("a0b1c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c4").goTo("a0b1c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c5").goTo("a0b1c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c6").goTo("a0b1c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c7").goTo("a0b1c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c8").goTo("a0b1c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c9").goTo("a0b1c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a0b0c10").goTo("a0b1c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c2").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c3").goTo("a1b1c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c4").goTo("a1b1c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c5").goTo("a1b1c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c6").goTo("a1b1c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c7").goTo("a1b1c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c8").goTo("a1b1c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c9").goTo("a1b1c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(246007, 215784))).from("a1b0c10").goTo("a1b1c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c0").goTo("a0b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c1").goTo("a0b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c2").goTo("a0b0c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c3").goTo("a0b0c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c4").goTo("a0b0c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c5").goTo("a0b0c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c6").goTo("a0b0c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c7").goTo("a0b0c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c8").goTo("a0b0c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b0c9").goTo("a0b0c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c0").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c1").goTo("a0b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c2").goTo("a0b1c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c3").goTo("a0b1c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c4").goTo("a0b1c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c5").goTo("a0b1c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c6").goTo("a0b1c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c7").goTo("a0b1c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c8").goTo("a0b1c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a0b1c9").goTo("a0b1c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c0").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c1").goTo("a1b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c2").goTo("a1b0c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c3").goTo("a1b0c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c4").goTo("a1b0c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c5").goTo("a1b0c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c6").goTo("a1b0c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c7").goTo("a1b0c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c8").goTo("a1b0c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b0c9").goTo("a1b0c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c0").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c1").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c2").goTo("a1b1c3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c3").goTo("a1b1c4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c4").goTo("a1b1c5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c5").goTo("a1b1c6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c6").goTo("a1b1c7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c7").goTo("a1b1c8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c8").goTo("a1b1c9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215820)).from("a1b1c9").goTo("a1b1c10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 31, 0)).from("a1b1c10").goTo("a1b1c10");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1009, 0)).from("a1b1c10").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 53570L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8798808L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 167000518, 2L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799337, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 53570L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 8798808L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 167000555, 2L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
