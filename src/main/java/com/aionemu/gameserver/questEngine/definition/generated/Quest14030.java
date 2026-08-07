package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest14030 {
	private Quest14030() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(14030)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Retrieved Memory", 1129931, 50, 2147483647, Set.of("ELYOS"), "MISSION", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 8755085L), new QuestReward("TITLE", 49, 1L), new QuestReward("SELECTABLE_ITEM", 120000888, 1L), new QuestReward("SELECTABLE_ITEM", 120000889, 1L), new QuestReward("ITEM", 186000005, 1L), new QuestReward("ITEM", 188052781, 1L)), List.of(), Set.of(), "", 0, 1, 1, true, true, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("unfinished", 1099, 0), new QuestStartCondition("noacquired", 1099, 0)), Map.of());
	}

	private static void configureNodes(QuestDsl.QuestBuilder builder) {
		configureProgress(builder);
		configureNodeBatch0(builder);
	}

	private static void configureProgress(QuestDsl.QuestBuilder builder) {
		builder.progress(new BitField("var0", 0, 6, 0, 57, PersistenceMode.PERSISTENT, ProgressScope.LOCAL));
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
		builder.node("s15", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 15))));
		builder.node("s16", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 16))));
		builder.node("s17", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 17))));
		builder.node("s18", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 18))));
		builder.node("s19", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 19))));
		builder.node("s20", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 20))));
		builder.node("s21", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 21))));
		builder.node("s22", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 22))));
		builder.node("s23", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 23))));
		builder.node("s24", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 24))));
		builder.node("s25", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 25))));
		builder.node("s26", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 26))));
		builder.node("s27", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 27))));
		builder.node("s28", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 28))));
		builder.node("s29", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 29))));
		builder.node("s30", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 30))));
		builder.node("s31", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 31))));
		builder.node("s32", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 32))));
		builder.node("s33", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 33))));
		builder.node("s34", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 34))));
		builder.node("s35", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 35))));
		builder.node("s36", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 36))));
		builder.node("s37", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 37))));
		builder.node("s38", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 38))));
		builder.node("s39", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 39))));
		builder.node("s40", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 40))));
		builder.node("s41", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 41))));
		builder.node("s42", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 42))));
		builder.node("s43", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 43))));
		builder.node("s44", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 44))));
		builder.node("s45", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 45))));
		builder.node("s46", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 46))));
		builder.node("s47", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 47))));
		builder.node("s48", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 48))));
		builder.node("s49", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 49))));
		builder.node("s50", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 50))));
		builder.node("s51", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 51))));
		builder.node("s52", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 52))));
		builder.node("s53", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 53))));
		builder.node("s54", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 54))));
		builder.node("s55", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 55))));
		builder.node("s56", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 56))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 57))));
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.LevelUp()).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203700, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203700, 10000, 0)).from("started").goTo("s1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(790001, 31, 0)).from("s1").goTo("s1");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(790001, 10001, 0)).from("s1").goTo("s2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(214578)).from("s2").goTo("s3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(790001, 31, 0)).from("s3").goTo("s3");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2034));
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(790001, 10003, 0)).from("s3").then(new QuestAction.GiveItem(182215387, 1)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 400010000, 2417.6367f, 2517.138f, 1434.491f, (byte) 37));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700551, -1, 0)).from("s4").goTo("s4");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(new QuestInstanceTarget.NextAvailable(310120000), 310120000, 52.0f, 174.0f, 229.0f, (byte) 0));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205119, 31, 0)).from("s4").goTo("s4");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(2375));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(205119, 10004, 0)).from("s4").then(new QuestAction.RemoveItem(182215387, 1)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.ApplyEffect(281, 0));
		builder.afterCommit(new AfterCommitAction.FlightTeleport(1001));
		builder.afterCommit(new AfterCommitAction.StartInvisibleTimer(43, new QuestTimerPolicy(new QuestTimerPolicy.Identity("14030-flyback", QuestTimerPolicy.Scope.PLAYER_QUEST), QuestTimerPolicy.Persistence.SESSION, QuestTimerPolicy.OverwritePolicy.REPLACE, QuestTimerPolicy.Delivery.AT_MOST_ONCE)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.InvisibleTimerEnd()).from("s4").goTo("s5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s5").goTo("s6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s6").goTo("s7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s7").goTo("s8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s8").goTo("s9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s9").goTo("s10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s10").goTo("s11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s11").goTo("s12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s12").goTo("s13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s13").goTo("s14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s14").goTo("s15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s15").goTo("s16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s16").goTo("s17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s17").goTo("s18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s18").goTo("s19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s19").goTo("s20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s20").goTo("s21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s21").goTo("s22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s22").goTo("s23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s23").goTo("s24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s24").goTo("s25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s25").goTo("s26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s26").goTo("s27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s27").goTo("s28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s28").goTo("s29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s29").goTo("s30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s30").goTo("s31");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s31").goTo("s32");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s32").goTo("s33");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s33").goTo("s34");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s34").goTo("s35");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s35").goTo("s36");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s36").goTo("s37");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s37").goTo("s38");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s38").goTo("s39");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s39").goTo("s40");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s40").goTo("s41");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s41").goTo("s42");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s42").goTo("s43");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s43").goTo("s44");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s44").goTo("s45");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s45").goTo("s46");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s46").goTo("s47");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s47").goTo("s48");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s48").goTo("s49");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s49").goTo("s50");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s50").goTo("s51");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s51").goTo("s52");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s52").goTo("s53");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s53").goTo("s54");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpcSet(Set.of(205022, 215400, 215399, 215398, 215397, 215396, 205021))).from("s54").goTo("s55");
		builder.afterCommit(new AfterCommitAction.SpawnNpc("14030-boss-215400", 215400, new QuestSpawnLocation.Fixed(310120000, QuestInstanceTarget.CurrentOrDefault.INSTANCE, 299.4378f, 289.15744f, 206.48138f, (byte) 75)));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215400)).from("s55").goTo("s56");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s5").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s6").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s7").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s8").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s9").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s10").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s11").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s12").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s13").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s14").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s15").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s16").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s17").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s18").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s19").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s20").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s21").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s22").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s23").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s24").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s25").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s26").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s27").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s28").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s29").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s30").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s31").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s32").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s33").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s34").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s35").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s36").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s37").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s38").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s39").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s40").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s41").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s42").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s43").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s44").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s45").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s46").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s47").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s48").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s49").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s50").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s51").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s52").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s53").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s54").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.Die()).from("s55").when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s5").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s6").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s7").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s8").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s9").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s10").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s11").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s12").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s13").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s14").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s15").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s16").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s17").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s18").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s19").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s20").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s21").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s22").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s23").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s24").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s25").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s26").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s27").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s28").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s29").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s30").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s31").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s32").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s33").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s34").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s35").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s36").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s37").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s38").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s39").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s40").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s41").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s42").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s43").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s44").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s45").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s46").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s47").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s48").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s49").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s50").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s51").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s52").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s53").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s54").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.EnterWorld()).from("s55").when(new QuestCondition.WorldIs(310120000, false)).when(new QuestCondition.VariableAtLeast("var0", 5)).when(new QuestCondition.VariableBelow("var0", 56)).goTo("s4");
		builder.afterCommit(new AfterCommitAction.SendSystemMessage(QuestSystemMessage.QUEST_FAILED));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(700552, -1, 0)).from("s56").goTo("reward");
		builder.afterCommit(new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.CurrentOrDefault.INSTANCE, 110010000, 1322.1934f, 1511.148f, 567.909f, (byte) 0));
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203700, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203700, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203700, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203700, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 8755085L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 49, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 120000888, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000005, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188052781, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(203700, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 8755085L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("TITLE", 49, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 120000889, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 186000005, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.GrantReward("ITEM", 188052781, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
