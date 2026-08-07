package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest21213 {
	private Quest21213() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(21213)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("Researcher's Dilemma", 1127116, 54, 2147483647, Set.of("ASMODIANS"), "QUEST", new RepeatPolicy(1, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("GOLD", 0, 220720L), new QuestReward("EXP", 0, 6517414L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		builder.node("a0b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 0))));
		builder.node("a0b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 0))));
		builder.node("a0b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 0))));
		builder.node("a0b2c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 0))));
		builder.node("a1b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 1))));
		builder.node("a1b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 1))));
		builder.node("a1b2c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 1))));
		builder.node("a2b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 2))));
		builder.node("a2b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 2))));
		builder.node("a2b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 2))));
		builder.node("a2b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 2))));
		builder.node("a2b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 2))));
		builder.node("a2b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 2))));
		builder.node("a2b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 2))));
		builder.node("a2b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 2))));
		builder.node("a2b2c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 2))));
		builder.node("a3b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 3))));
		builder.node("a3b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 3))));
		builder.node("a3b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 3))));
		builder.node("a3b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 3))));
		builder.node("a3b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 3))));
		builder.node("a3b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 3))));
		builder.node("a3b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 3))));
		builder.node("a3b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 3))));
		builder.node("a3b2c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 3))));
		builder.node("a4b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 4))));
		builder.node("a4b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 4))));
		builder.node("a4b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 4))));
		builder.node("a4b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 4))));
		builder.node("a4b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 4))));
		builder.node("a4b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 4))));
		builder.node("a4b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 4))));
		builder.node("a4b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 4))));
		builder.node("a4b2c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 4))));
		builder.node("a5b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 5))));
		builder.node("a5b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 5))));
		builder.node("a5b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 5))));
		builder.node("a5b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 5))));
		builder.node("a5b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 5))));
		builder.node("a5b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 5))));
		builder.node("a5b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 5))));
		builder.node("a5b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 5))));
		builder.node("a5b2c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 5))));
		builder.node("a6b0c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 6))));
		builder.node("a6b0c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 0), Map.entry("var0", 6))));
		builder.node("a6b0c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 0), Map.entry("var0", 6))));
		builder.node("a6b1c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 1), Map.entry("var0", 6))));
		builder.node("a6b1c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 1), Map.entry("var0", 6))));
		builder.node("a6b1c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 1), Map.entry("var0", 6))));
		builder.node("a6b2c0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 2), Map.entry("var0", 6))));
		builder.node("a6b2c1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 1), Map.entry("var1", 2), Map.entry("var0", 6))));
		builder.node("a6b2c2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 6))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var2", 2), Map.entry("var1", 2), Map.entry("var0", 6))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var2", 0), Map.entry("var1", 0), Map.entry("var0", 0))));
	}

	private static void addTransitions(QuestDsl.QuestBuilder builder) {
		addTransitionBatch0(builder);
		addTransitionBatch1(builder);
		addTransitionBatch2(builder);
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
		addTransition197(builder);
		addTransition198(builder);
		addTransition199(builder);
		addTransition200(builder);
		addTransition201(builder);
		addTransition202(builder);
		addTransition203(builder);
		addTransition204(builder);
		addTransition205(builder);
		addTransition206(builder);
		addTransition207(builder);
		addTransition208(builder);
		addTransition209(builder);
		addTransition210(builder);
		addTransition211(builder);
		addTransition212(builder);
		addTransition213(builder);
		addTransition214(builder);
		addTransition215(builder);
		addTransition216(builder);
		addTransition217(builder);
		addTransition218(builder);
		addTransition219(builder);
		addTransition220(builder);
		addTransition221(builder);
		addTransition222(builder);
		addTransition223(builder);
		addTransition224(builder);
		addTransition225(builder);
		addTransition226(builder);
		addTransition227(builder);
		addTransition228(builder);
		addTransition229(builder);
		addTransition230(builder);
		addTransition231(builder);
		addTransition232(builder);
		addTransition233(builder);
		addTransition234(builder);
		addTransition235(builder);
		addTransition236(builder);
		addTransition237(builder);
		addTransition238(builder);
		addTransition239(builder);
		addTransition240(builder);
		addTransition241(builder);
		addTransition242(builder);
		addTransition243(builder);
		addTransition244(builder);
		addTransition245(builder);
		addTransition246(builder);
		addTransition247(builder);
		addTransition248(builder);
		addTransition249(builder);
		addTransition250(builder);
		addTransition251(builder);
		addTransition252(builder);
		addTransition253(builder);
		addTransition254(builder);
		addTransition255(builder);
	}

	private static void addTransitionBatch2(QuestDsl.QuestBuilder builder) {
		addTransition256(builder);
		addTransition257(builder);
		addTransition258(builder);
		addTransition259(builder);
		addTransition260(builder);
		addTransition261(builder);
		addTransition262(builder);
		addTransition263(builder);
		addTransition264(builder);
		addTransition265(builder);
		addTransition266(builder);
		addTransition267(builder);
		addTransition268(builder);
		addTransition269(builder);
		addTransition270(builder);
		addTransition271(builder);
		addTransition272(builder);
		addTransition273(builder);
		addTransition274(builder);
		addTransition275(builder);
		addTransition276(builder);
		addTransition277(builder);
		addTransition278(builder);
		addTransition279(builder);
		addTransition280(builder);
		addTransition281(builder);
		addTransition282(builder);
		addTransition283(builder);
		addTransition284(builder);
		addTransition285(builder);
		addTransition286(builder);
		addTransition287(builder);
		addTransition288(builder);
		addTransition289(builder);
		addTransition290(builder);
		addTransition291(builder);
		addTransition292(builder);
		addTransition293(builder);
		addTransition294(builder);
		addTransition295(builder);
		addTransition296(builder);
		addTransition297(builder);
		addTransition298(builder);
		addTransition299(builder);
		addTransition300(builder);
		addTransition301(builder);
		addTransition302(builder);
		addTransition303(builder);
		addTransition304(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1008, 0)).from("a0b0c0").goTo("a0b0c0");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b0c0").goTo("a1b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b0c0").goTo("a1b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b0c1").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b0c1").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b0c2").goTo("a1b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b0c2").goTo("a1b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b1c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b1c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b1c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b1c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b1c2").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b1c2").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b2c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b2c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b2c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b2c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a0b2c2").goTo("a1b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a0b2c2").goTo("a1b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b0c0").goTo("a2b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b0c0").goTo("a2b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b0c1").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b0c1").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b0c2").goTo("a2b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b0c2").goTo("a2b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b1c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b1c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b1c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b1c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b1c2").goTo("a2b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b1c2").goTo("a2b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b2c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b2c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b2c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b2c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a1b2c2").goTo("a2b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a1b2c2").goTo("a2b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b0c0").goTo("a3b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b0c0").goTo("a3b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b0c1").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b0c1").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b0c2").goTo("a3b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b0c2").goTo("a3b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b1c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b1c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b1c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b1c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b1c2").goTo("a3b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b1c2").goTo("a3b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b2c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b2c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b2c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b2c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a2b2c2").goTo("a3b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a2b2c2").goTo("a3b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b0c0").goTo("a4b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b0c0").goTo("a4b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b0c1").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b0c1").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b0c2").goTo("a4b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b0c2").goTo("a4b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b1c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b1c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b1c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b1c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b1c2").goTo("a4b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b1c2").goTo("a4b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b2c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b2c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b2c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b2c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a3b2c2").goTo("a4b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a3b2c2").goTo("a4b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b0c0").goTo("a5b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b0c0").goTo("a5b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b0c1").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b0c1").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b0c2").goTo("a5b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b0c2").goTo("a5b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b1c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b1c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b1c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b1c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b1c2").goTo("a5b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b1c2").goTo("a5b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b2c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b2c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b2c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b2c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a4b2c2").goTo("a5b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a4b2c2").goTo("a5b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b0c0").goTo("a6b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b0c0").goTo("a6b0c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b0c1").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b0c1").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b0c2").goTo("a6b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b0c2").goTo("a6b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b1c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b1c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b1c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b1c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b1c2").goTo("a6b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b1c2").goTo("a6b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b2c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b2c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b2c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b2c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215903)).from("a5b2c2").goTo("a6b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215904)).from("a5b2c2").goTo("a6b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a0b0c0").goTo("a0b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a0b0c1").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a0b0c2").goTo("a0b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a0b0c2").goTo("a0b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a1b0c0").goTo("a1b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a1b0c1").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a1b0c2").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a1b0c2").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a2b0c0").goTo("a2b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a2b0c1").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a2b0c2").goTo("a2b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a2b0c2").goTo("a2b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a3b0c0").goTo("a3b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a3b0c1").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a3b0c2").goTo("a3b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a3b0c2").goTo("a3b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a4b0c0").goTo("a4b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a4b0c1").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a4b0c2").goTo("a4b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a4b0c2").goTo("a4b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a5b0c0").goTo("a5b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a5b0c1").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a5b0c2").goTo("a5b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a5b0c2").goTo("a5b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a6b0c0").goTo("a6b1c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a6b0c1").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a6b0c2").goTo("a6b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a6b0c2").goTo("a6b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a0b1c0").goTo("a0b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a0b1c1").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a0b1c2").goTo("a0b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a0b1c2").goTo("a0b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a1b1c0").goTo("a1b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a1b1c1").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a1b1c2").goTo("a1b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a1b1c2").goTo("a1b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition171(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition172(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a2b1c0").goTo("a2b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition173(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition174(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a2b1c1").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition175(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a2b1c2").goTo("a2b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition176(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a2b1c2").goTo("a2b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition177(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition178(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a3b1c0").goTo("a3b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition179(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition180(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a3b1c1").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition181(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a3b1c2").goTo("a3b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition182(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a3b1c2").goTo("a3b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition183(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition184(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a4b1c0").goTo("a4b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition185(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition186(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a4b1c1").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition187(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a4b1c2").goTo("a4b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition188(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a4b1c2").goTo("a4b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition189(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition190(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a5b1c0").goTo("a5b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition191(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition192(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a5b1c1").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition193(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a5b1c2").goTo("a5b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition194(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a5b1c2").goTo("a5b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition195(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition196(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a6b1c0").goTo("a6b2c0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition197(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition198(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a6b1c1").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition199(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215946)).from("a6b1c2").goTo("a6b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition200(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215947)).from("a6b1c2").goTo("a6b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition201(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a0b0c0").goTo("a0b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition202(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a0b0c0").goTo("a0b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition203(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a0b1c0").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition204(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a0b1c0").goTo("a0b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition205(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a0b2c0").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition206(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a0b2c0").goTo("a0b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition207(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a1b0c0").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition208(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a1b0c0").goTo("a1b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition209(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a1b1c0").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition210(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a1b1c0").goTo("a1b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition211(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a1b2c0").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition212(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a1b2c0").goTo("a1b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition213(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a2b0c0").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition214(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a2b0c0").goTo("a2b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition215(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a2b1c0").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition216(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a2b1c0").goTo("a2b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition217(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a2b2c0").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition218(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a2b2c0").goTo("a2b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition219(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a3b0c0").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition220(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a3b0c0").goTo("a3b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition221(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a3b1c0").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition222(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a3b1c0").goTo("a3b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition223(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a3b2c0").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition224(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a3b2c0").goTo("a3b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition225(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a4b0c0").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition226(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a4b0c0").goTo("a4b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition227(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a4b1c0").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition228(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a4b1c0").goTo("a4b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition229(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a4b2c0").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition230(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a4b2c0").goTo("a4b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition231(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a5b0c0").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition232(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a5b0c0").goTo("a5b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition233(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a5b1c0").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition234(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a5b1c0").goTo("a5b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition235(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a5b2c0").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition236(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a5b2c0").goTo("a5b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition237(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a6b0c0").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition238(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a6b0c0").goTo("a6b0c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition239(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a6b1c0").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition240(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a6b1c0").goTo("a6b1c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition241(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a6b2c0").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition242(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a6b2c0").goTo("a6b2c1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition243(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a0b0c1").goTo("a0b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition244(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a0b0c1").goTo("a0b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition245(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a0b1c1").goTo("a0b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition246(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a0b1c1").goTo("a0b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition247(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a0b2c1").goTo("a0b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition248(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a0b2c1").goTo("a0b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition249(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a1b0c1").goTo("a1b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition250(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a1b0c1").goTo("a1b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition251(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a1b1c1").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition252(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a1b1c1").goTo("a1b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition253(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a1b2c1").goTo("a1b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition254(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a1b2c1").goTo("a1b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition255(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a2b0c1").goTo("a2b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition256(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a2b0c1").goTo("a2b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition257(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a2b1c1").goTo("a2b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition258(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a2b1c1").goTo("a2b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition259(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a2b2c1").goTo("a2b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition260(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a2b2c1").goTo("a2b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition261(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a3b0c1").goTo("a3b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition262(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a3b0c1").goTo("a3b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition263(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a3b1c1").goTo("a3b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition264(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a3b1c1").goTo("a3b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition265(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a3b2c1").goTo("a3b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition266(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a3b2c1").goTo("a3b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition267(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a4b0c1").goTo("a4b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition268(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a4b0c1").goTo("a4b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition269(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a4b1c1").goTo("a4b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition270(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a4b1c1").goTo("a4b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition271(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a4b2c1").goTo("a4b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition272(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a4b2c1").goTo("a4b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition273(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a5b0c1").goTo("a5b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition274(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a5b0c1").goTo("a5b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition275(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a5b1c1").goTo("a5b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition276(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a5b1c1").goTo("a5b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition277(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a5b2c1").goTo("a5b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition278(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a5b2c1").goTo("a5b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition279(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a6b0c1").goTo("a6b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition280(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a6b0c1").goTo("a6b0c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition281(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a6b1c1").goTo("a6b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition282(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a6b1c1").goTo("a6b1c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition283(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215927)).from("a6b2c1").goTo("a6b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition284(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(215928)).from("a6b2c1").goTo("a6b2c2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition285(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 31, 0)).from("a6b2c2").goTo("a6b2c2");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition286(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1009, 0)).from("a6b2c2").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition287(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition288(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition289(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 8, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition290(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 9, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition291(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 10, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition292(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 11, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition293(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 12, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition294(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 13, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition295(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 14, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition296(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 15, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition297(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 16, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition298(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 17, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition299(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 18, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition300(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 19, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition301(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 20, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition302(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 21, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition303(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 22, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition304(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(799316, 23, 0)).from("reward").then(new QuestAction.GrantReward("GOLD", 0, 220720L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("EXP", 0, 6517414L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
