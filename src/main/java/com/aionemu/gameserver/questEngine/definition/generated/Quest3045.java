package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest3045 {
	private Quest3045() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(3045)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Statues On The March", 1113044, 46, 2147483647, Set.of("ELYOS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 25800L), new QuestReward("EXP", 0, 2352448L), new QuestReward("ITEM", 162000048, 3L), new QuestReward("ITEM", 188050590, 1L), new QuestReward("SELECTABLE_ITEM", 110100933, 1L), new QuestReward("SELECTABLE_ITEM", 110300883, 1L), new QuestReward("SELECTABLE_ITEM", 110500851, 1L), new QuestReward("SELECTABLE_ITEM", 110600836, 1L)), List.of(), Set.of(), "", 0, 1, 1, true, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(new QuestBonus("FOOD", 50, null)), List.of(), List.of(new QuestStartCondition("finished", 3044, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL), new BitField("var1", 6, 6, 0, 63, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
	}

	private static void configureNodeBatch0(QuestDsl.QuestBuilder builder) {
		builder.node("unaccepted", new NodeProjection(QuestStatus.NONE, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 0))));
		builder.node("a0b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 0))));
		builder.node("a0b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 0))));
		builder.node("a1b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 1))));
		builder.node("a1b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 1))));
		builder.node("a2b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 2))));
		builder.node("a2b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 2))));
		builder.node("a2b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 2))));
		builder.node("a2b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 2))));
		builder.node("a3b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 3))));
		builder.node("a3b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 3))));
		builder.node("a3b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 3))));
		builder.node("a3b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 3))));
		builder.node("a4b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 4))));
		builder.node("a4b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 4))));
		builder.node("a4b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 4))));
		builder.node("a4b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 4))));
		builder.node("a5b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 5))));
		builder.node("a5b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 5))));
		builder.node("a5b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 5))));
		builder.node("a5b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 5))));
		builder.node("a6b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 6))));
		builder.node("a6b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 6))));
		builder.node("a6b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 6))));
		builder.node("a6b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 6))));
		builder.node("a7b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 7))));
		builder.node("a7b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 7))));
		builder.node("a7b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 7))));
		builder.node("a7b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 7))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 7))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 0))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a4b0").goTo("a5b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a4b1").goTo("a5b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a4b2").goTo("a5b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a4b3").goTo("a5b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a5b0").goTo("a6b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a5b1").goTo("a6b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a5b2").goTo("a6b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a5b3").goTo("a6b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a6b0").goTo("a7b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a6b1").goTo("a7b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a6b2").goTo("a7b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214286)).from("a6b3").goTo("a7b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a0b0").goTo("a0b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a1b0").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a2b0").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a3b0").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a4b0").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a5b0").goTo("a5b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a6b0").goTo("a6b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a7b0").goTo("a7b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a0b1").goTo("a0b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a1b1").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a2b1").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a3b1").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a4b1").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a5b1").goTo("a5b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a6b1").goTo("a6b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a7b1").goTo("a7b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a0b2").goTo("a0b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a1b2").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a2b2").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a3b2").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a4b2").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a5b2").goTo("a5b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a6b2").goTo("a6b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214282)).from("a7b2").goTo("a7b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 31, 0)).from("a7b3").goTo("a7b3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 1009, 0)).from("a7b3").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 25800L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 2352448L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188050590, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110100933, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 25800L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 2352448L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188050590, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110300883, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 25800L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 2352448L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188050590, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110500851, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(798206, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 25800L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 2352448L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 162000048, 3L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188050590, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 110600836, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
