package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest80605 {
	private Quest80605() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(80605)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Service/Group] Unending Assault", 1136977, 61, 82, Set.of("ELYOS"), "QUEST", new RepeatPolicy(255, 0L, false, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 4742870L), new QuestReward("ITEM", 186000469, 150L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
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
		builder.node("a0b4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 4), Map.entry("var0", 0))));
		builder.node("a0b5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 5), Map.entry("var0", 0))));
		builder.node("a0b6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 6), Map.entry("var0", 0))));
		builder.node("a0b7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 7), Map.entry("var0", 0))));
		builder.node("a0b8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 8), Map.entry("var0", 0))));
		builder.node("a0b9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 9), Map.entry("var0", 0))));
		builder.node("a0b10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 10), Map.entry("var0", 0))));
		builder.node("a0b11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 11), Map.entry("var0", 0))));
		builder.node("a0b12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 12), Map.entry("var0", 0))));
		builder.node("a0b13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 13), Map.entry("var0", 0))));
		builder.node("a0b14", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 14), Map.entry("var0", 0))));
		builder.node("a0b15", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 15), Map.entry("var0", 0))));
		builder.node("a1b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 1))));
		builder.node("a1b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 1))));
		builder.node("a1b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 1))));
		builder.node("a1b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 1))));
		builder.node("a1b4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 4), Map.entry("var0", 1))));
		builder.node("a1b5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 5), Map.entry("var0", 1))));
		builder.node("a1b6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 6), Map.entry("var0", 1))));
		builder.node("a1b7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 7), Map.entry("var0", 1))));
		builder.node("a1b8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 8), Map.entry("var0", 1))));
		builder.node("a1b9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 9), Map.entry("var0", 1))));
		builder.node("a1b10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 10), Map.entry("var0", 1))));
		builder.node("a1b11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 11), Map.entry("var0", 1))));
		builder.node("a1b12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 12), Map.entry("var0", 1))));
		builder.node("a1b13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 13), Map.entry("var0", 1))));
		builder.node("a1b14", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 14), Map.entry("var0", 1))));
		builder.node("a1b15", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 15), Map.entry("var0", 1))));
		builder.node("a2b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 2))));
		builder.node("a2b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 2))));
		builder.node("a2b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 2))));
		builder.node("a2b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 2))));
		builder.node("a2b4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 4), Map.entry("var0", 2))));
		builder.node("a2b5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 5), Map.entry("var0", 2))));
		builder.node("a2b6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 6), Map.entry("var0", 2))));
		builder.node("a2b7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 7), Map.entry("var0", 2))));
		builder.node("a2b8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 8), Map.entry("var0", 2))));
		builder.node("a2b9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 9), Map.entry("var0", 2))));
		builder.node("a2b10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 10), Map.entry("var0", 2))));
		builder.node("a2b11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 11), Map.entry("var0", 2))));
		builder.node("a2b12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 12), Map.entry("var0", 2))));
		builder.node("a2b13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 13), Map.entry("var0", 2))));
		builder.node("a2b14", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 14), Map.entry("var0", 2))));
		builder.node("a2b15", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 15), Map.entry("var0", 2))));
		builder.node("a3b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 3))));
		builder.node("a3b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 3))));
		builder.node("a3b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 3))));
		builder.node("a3b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 3))));
		builder.node("a3b4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 4), Map.entry("var0", 3))));
		builder.node("a3b5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 5), Map.entry("var0", 3))));
		builder.node("a3b6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 6), Map.entry("var0", 3))));
		builder.node("a3b7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 7), Map.entry("var0", 3))));
		builder.node("a3b8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 8), Map.entry("var0", 3))));
		builder.node("a3b9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 9), Map.entry("var0", 3))));
		builder.node("a3b10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 10), Map.entry("var0", 3))));
		builder.node("a3b11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 11), Map.entry("var0", 3))));
		builder.node("a3b12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 12), Map.entry("var0", 3))));
		builder.node("a3b13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 13), Map.entry("var0", 3))));
		builder.node("a3b14", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 14), Map.entry("var0", 3))));
		builder.node("a3b15", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 15), Map.entry("var0", 3))));
		builder.node("a4b0", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 4))));
		builder.node("a4b1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 1), Map.entry("var0", 4))));
		builder.node("a4b2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 2), Map.entry("var0", 4))));
		builder.node("a4b3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 3), Map.entry("var0", 4))));
		builder.node("a4b4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 4), Map.entry("var0", 4))));
		builder.node("a4b5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 5), Map.entry("var0", 4))));
		builder.node("a4b6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 6), Map.entry("var0", 4))));
		builder.node("a4b7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 7), Map.entry("var0", 4))));
		builder.node("a4b8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 8), Map.entry("var0", 4))));
		builder.node("a4b9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 9), Map.entry("var0", 4))));
		builder.node("a4b10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 10), Map.entry("var0", 4))));
		builder.node("a4b11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 11), Map.entry("var0", 4))));
		builder.node("a4b12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 12), Map.entry("var0", 4))));
		builder.node("a4b13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 13), Map.entry("var0", 4))));
		builder.node("a4b14", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 14), Map.entry("var0", 4))));
		builder.node("a4b15", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var1", 15), Map.entry("var0", 4))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var1", 15), Map.entry("var0", 4))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.ofEntries(Map.entry("var1", 0), Map.entry("var0", 0))));
	}

	private static void addTransitions(QuestDsl.QuestBuilder builder) {
		addTransitionBatch0(builder);
		addTransitionBatch1(builder);
		addTransitionBatch2(builder);
		addTransitionBatch3(builder);
		addTransitionBatch4(builder);
		addTransitionBatch5(builder);
		addTransitionBatch6(builder);
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
		addTransition305(builder);
		addTransition306(builder);
		addTransition307(builder);
		addTransition308(builder);
		addTransition309(builder);
		addTransition310(builder);
		addTransition311(builder);
		addTransition312(builder);
		addTransition313(builder);
		addTransition314(builder);
		addTransition315(builder);
		addTransition316(builder);
		addTransition317(builder);
		addTransition318(builder);
		addTransition319(builder);
		addTransition320(builder);
		addTransition321(builder);
		addTransition322(builder);
		addTransition323(builder);
		addTransition324(builder);
		addTransition325(builder);
		addTransition326(builder);
		addTransition327(builder);
		addTransition328(builder);
		addTransition329(builder);
		addTransition330(builder);
		addTransition331(builder);
		addTransition332(builder);
		addTransition333(builder);
		addTransition334(builder);
		addTransition335(builder);
		addTransition336(builder);
		addTransition337(builder);
		addTransition338(builder);
		addTransition339(builder);
		addTransition340(builder);
		addTransition341(builder);
		addTransition342(builder);
		addTransition343(builder);
		addTransition344(builder);
		addTransition345(builder);
		addTransition346(builder);
		addTransition347(builder);
		addTransition348(builder);
		addTransition349(builder);
		addTransition350(builder);
		addTransition351(builder);
		addTransition352(builder);
		addTransition353(builder);
		addTransition354(builder);
		addTransition355(builder);
		addTransition356(builder);
		addTransition357(builder);
		addTransition358(builder);
		addTransition359(builder);
		addTransition360(builder);
		addTransition361(builder);
		addTransition362(builder);
		addTransition363(builder);
		addTransition364(builder);
		addTransition365(builder);
		addTransition366(builder);
		addTransition367(builder);
		addTransition368(builder);
		addTransition369(builder);
		addTransition370(builder);
		addTransition371(builder);
		addTransition372(builder);
		addTransition373(builder);
		addTransition374(builder);
		addTransition375(builder);
		addTransition376(builder);
		addTransition377(builder);
		addTransition378(builder);
		addTransition379(builder);
		addTransition380(builder);
		addTransition381(builder);
		addTransition382(builder);
		addTransition383(builder);
	}

	private static void addTransitionBatch3(QuestDsl.QuestBuilder builder) {
		addTransition384(builder);
		addTransition385(builder);
		addTransition386(builder);
		addTransition387(builder);
		addTransition388(builder);
		addTransition389(builder);
		addTransition390(builder);
		addTransition391(builder);
		addTransition392(builder);
		addTransition393(builder);
		addTransition394(builder);
		addTransition395(builder);
		addTransition396(builder);
		addTransition397(builder);
		addTransition398(builder);
		addTransition399(builder);
		addTransition400(builder);
		addTransition401(builder);
		addTransition402(builder);
		addTransition403(builder);
		addTransition404(builder);
		addTransition405(builder);
		addTransition406(builder);
		addTransition407(builder);
		addTransition408(builder);
		addTransition409(builder);
		addTransition410(builder);
		addTransition411(builder);
		addTransition412(builder);
		addTransition413(builder);
		addTransition414(builder);
		addTransition415(builder);
		addTransition416(builder);
		addTransition417(builder);
		addTransition418(builder);
		addTransition419(builder);
		addTransition420(builder);
		addTransition421(builder);
		addTransition422(builder);
		addTransition423(builder);
		addTransition424(builder);
		addTransition425(builder);
		addTransition426(builder);
		addTransition427(builder);
		addTransition428(builder);
		addTransition429(builder);
		addTransition430(builder);
		addTransition431(builder);
		addTransition432(builder);
		addTransition433(builder);
		addTransition434(builder);
		addTransition435(builder);
		addTransition436(builder);
		addTransition437(builder);
		addTransition438(builder);
		addTransition439(builder);
		addTransition440(builder);
		addTransition441(builder);
		addTransition442(builder);
		addTransition443(builder);
		addTransition444(builder);
		addTransition445(builder);
		addTransition446(builder);
		addTransition447(builder);
		addTransition448(builder);
		addTransition449(builder);
		addTransition450(builder);
		addTransition451(builder);
		addTransition452(builder);
		addTransition453(builder);
		addTransition454(builder);
		addTransition455(builder);
		addTransition456(builder);
		addTransition457(builder);
		addTransition458(builder);
		addTransition459(builder);
		addTransition460(builder);
		addTransition461(builder);
		addTransition462(builder);
		addTransition463(builder);
		addTransition464(builder);
		addTransition465(builder);
		addTransition466(builder);
		addTransition467(builder);
		addTransition468(builder);
		addTransition469(builder);
		addTransition470(builder);
		addTransition471(builder);
		addTransition472(builder);
		addTransition473(builder);
		addTransition474(builder);
		addTransition475(builder);
		addTransition476(builder);
		addTransition477(builder);
		addTransition478(builder);
		addTransition479(builder);
		addTransition480(builder);
		addTransition481(builder);
		addTransition482(builder);
		addTransition483(builder);
		addTransition484(builder);
		addTransition485(builder);
		addTransition486(builder);
		addTransition487(builder);
		addTransition488(builder);
		addTransition489(builder);
		addTransition490(builder);
		addTransition491(builder);
		addTransition492(builder);
		addTransition493(builder);
		addTransition494(builder);
		addTransition495(builder);
		addTransition496(builder);
		addTransition497(builder);
		addTransition498(builder);
		addTransition499(builder);
		addTransition500(builder);
		addTransition501(builder);
		addTransition502(builder);
		addTransition503(builder);
		addTransition504(builder);
		addTransition505(builder);
		addTransition506(builder);
		addTransition507(builder);
		addTransition508(builder);
		addTransition509(builder);
		addTransition510(builder);
		addTransition511(builder);
	}

	private static void addTransitionBatch4(QuestDsl.QuestBuilder builder) {
		addTransition512(builder);
		addTransition513(builder);
		addTransition514(builder);
		addTransition515(builder);
		addTransition516(builder);
		addTransition517(builder);
		addTransition518(builder);
		addTransition519(builder);
		addTransition520(builder);
		addTransition521(builder);
		addTransition522(builder);
		addTransition523(builder);
		addTransition524(builder);
		addTransition525(builder);
		addTransition526(builder);
		addTransition527(builder);
		addTransition528(builder);
		addTransition529(builder);
		addTransition530(builder);
		addTransition531(builder);
		addTransition532(builder);
		addTransition533(builder);
		addTransition534(builder);
		addTransition535(builder);
		addTransition536(builder);
		addTransition537(builder);
		addTransition538(builder);
		addTransition539(builder);
		addTransition540(builder);
		addTransition541(builder);
		addTransition542(builder);
		addTransition543(builder);
		addTransition544(builder);
		addTransition545(builder);
		addTransition546(builder);
		addTransition547(builder);
		addTransition548(builder);
		addTransition549(builder);
		addTransition550(builder);
		addTransition551(builder);
		addTransition552(builder);
		addTransition553(builder);
		addTransition554(builder);
		addTransition555(builder);
		addTransition556(builder);
		addTransition557(builder);
		addTransition558(builder);
		addTransition559(builder);
		addTransition560(builder);
		addTransition561(builder);
		addTransition562(builder);
		addTransition563(builder);
		addTransition564(builder);
		addTransition565(builder);
		addTransition566(builder);
		addTransition567(builder);
		addTransition568(builder);
		addTransition569(builder);
		addTransition570(builder);
		addTransition571(builder);
		addTransition572(builder);
		addTransition573(builder);
		addTransition574(builder);
		addTransition575(builder);
		addTransition576(builder);
		addTransition577(builder);
		addTransition578(builder);
		addTransition579(builder);
		addTransition580(builder);
		addTransition581(builder);
		addTransition582(builder);
		addTransition583(builder);
		addTransition584(builder);
		addTransition585(builder);
		addTransition586(builder);
		addTransition587(builder);
		addTransition588(builder);
		addTransition589(builder);
		addTransition590(builder);
		addTransition591(builder);
		addTransition592(builder);
		addTransition593(builder);
		addTransition594(builder);
		addTransition595(builder);
		addTransition596(builder);
		addTransition597(builder);
		addTransition598(builder);
		addTransition599(builder);
		addTransition600(builder);
		addTransition601(builder);
		addTransition602(builder);
		addTransition603(builder);
		addTransition604(builder);
		addTransition605(builder);
		addTransition606(builder);
		addTransition607(builder);
		addTransition608(builder);
		addTransition609(builder);
		addTransition610(builder);
		addTransition611(builder);
		addTransition612(builder);
		addTransition613(builder);
		addTransition614(builder);
		addTransition615(builder);
		addTransition616(builder);
		addTransition617(builder);
		addTransition618(builder);
		addTransition619(builder);
		addTransition620(builder);
		addTransition621(builder);
		addTransition622(builder);
		addTransition623(builder);
		addTransition624(builder);
		addTransition625(builder);
		addTransition626(builder);
		addTransition627(builder);
		addTransition628(builder);
		addTransition629(builder);
		addTransition630(builder);
		addTransition631(builder);
		addTransition632(builder);
		addTransition633(builder);
		addTransition634(builder);
		addTransition635(builder);
		addTransition636(builder);
		addTransition637(builder);
		addTransition638(builder);
		addTransition639(builder);
	}

	private static void addTransitionBatch5(QuestDsl.QuestBuilder builder) {
		addTransition640(builder);
		addTransition641(builder);
		addTransition642(builder);
		addTransition643(builder);
		addTransition644(builder);
		addTransition645(builder);
		addTransition646(builder);
		addTransition647(builder);
		addTransition648(builder);
		addTransition649(builder);
		addTransition650(builder);
		addTransition651(builder);
		addTransition652(builder);
		addTransition653(builder);
		addTransition654(builder);
		addTransition655(builder);
		addTransition656(builder);
		addTransition657(builder);
		addTransition658(builder);
		addTransition659(builder);
		addTransition660(builder);
		addTransition661(builder);
		addTransition662(builder);
		addTransition663(builder);
		addTransition664(builder);
		addTransition665(builder);
		addTransition666(builder);
		addTransition667(builder);
		addTransition668(builder);
		addTransition669(builder);
		addTransition670(builder);
		addTransition671(builder);
		addTransition672(builder);
		addTransition673(builder);
		addTransition674(builder);
		addTransition675(builder);
		addTransition676(builder);
		addTransition677(builder);
		addTransition678(builder);
		addTransition679(builder);
		addTransition680(builder);
		addTransition681(builder);
		addTransition682(builder);
		addTransition683(builder);
		addTransition684(builder);
		addTransition685(builder);
		addTransition686(builder);
		addTransition687(builder);
		addTransition688(builder);
		addTransition689(builder);
		addTransition690(builder);
		addTransition691(builder);
		addTransition692(builder);
		addTransition693(builder);
		addTransition694(builder);
		addTransition695(builder);
		addTransition696(builder);
		addTransition697(builder);
		addTransition698(builder);
		addTransition699(builder);
		addTransition700(builder);
		addTransition701(builder);
		addTransition702(builder);
		addTransition703(builder);
		addTransition704(builder);
		addTransition705(builder);
		addTransition706(builder);
		addTransition707(builder);
		addTransition708(builder);
		addTransition709(builder);
		addTransition710(builder);
		addTransition711(builder);
		addTransition712(builder);
		addTransition713(builder);
		addTransition714(builder);
		addTransition715(builder);
		addTransition716(builder);
		addTransition717(builder);
		addTransition718(builder);
		addTransition719(builder);
		addTransition720(builder);
		addTransition721(builder);
		addTransition722(builder);
		addTransition723(builder);
		addTransition724(builder);
		addTransition725(builder);
		addTransition726(builder);
		addTransition727(builder);
		addTransition728(builder);
		addTransition729(builder);
		addTransition730(builder);
		addTransition731(builder);
		addTransition732(builder);
		addTransition733(builder);
		addTransition734(builder);
		addTransition735(builder);
		addTransition736(builder);
		addTransition737(builder);
		addTransition738(builder);
		addTransition739(builder);
		addTransition740(builder);
		addTransition741(builder);
		addTransition742(builder);
		addTransition743(builder);
		addTransition744(builder);
		addTransition745(builder);
		addTransition746(builder);
		addTransition747(builder);
		addTransition748(builder);
		addTransition749(builder);
		addTransition750(builder);
		addTransition751(builder);
		addTransition752(builder);
		addTransition753(builder);
		addTransition754(builder);
		addTransition755(builder);
		addTransition756(builder);
		addTransition757(builder);
		addTransition758(builder);
		addTransition759(builder);
		addTransition760(builder);
		addTransition761(builder);
		addTransition762(builder);
		addTransition763(builder);
		addTransition764(builder);
		addTransition765(builder);
		addTransition766(builder);
		addTransition767(builder);
	}

	private static void addTransitionBatch6(QuestDsl.QuestBuilder builder) {
		addTransition768(builder);
		addTransition769(builder);
		addTransition770(builder);
		addTransition771(builder);
		addTransition772(builder);
		addTransition773(builder);
		addTransition774(builder);
		addTransition775(builder);
		addTransition776(builder);
		addTransition777(builder);
		addTransition778(builder);
		addTransition779(builder);
		addTransition780(builder);
		addTransition781(builder);
		addTransition782(builder);
		addTransition783(builder);
		addTransition784(builder);
		addTransition785(builder);
		addTransition786(builder);
		addTransition787(builder);
		addTransition788(builder);
		addTransition789(builder);
		addTransition790(builder);
		addTransition791(builder);
		addTransition792(builder);
		addTransition793(builder);
		addTransition794(builder);
		addTransition795(builder);
		addTransition796(builder);
		addTransition797(builder);
		addTransition798(builder);
		addTransition799(builder);
		addTransition800(builder);
		addTransition801(builder);
		addTransition802(builder);
		addTransition803(builder);
		addTransition804(builder);
		addTransition805(builder);
		addTransition806(builder);
		addTransition807(builder);
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("a0b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1008, 0)).from("a0b0").goTo("a0b0");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219240)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219241)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition171(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition172(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition173(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition174(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition175(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition176(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition177(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition178(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition179(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition180(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition181(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition182(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition183(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition184(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition185(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition186(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition187(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition188(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition189(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition190(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition191(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition192(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition193(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition194(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition195(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition196(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition197(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition198(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition199(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition200(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219242)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition201(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition202(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition203(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition204(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition205(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition206(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition207(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition208(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition209(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition210(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition211(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition212(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition213(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition214(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition215(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition216(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition217(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition218(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition219(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition220(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition221(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition222(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition223(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition224(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition225(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition226(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition227(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition228(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition229(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition230(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition231(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition232(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition233(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition234(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition235(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition236(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition237(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition238(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition239(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition240(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition241(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition242(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition243(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition244(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition245(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition246(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition247(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition248(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition249(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition250(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition251(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition252(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition253(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition254(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition255(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition256(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition257(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition258(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition259(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition260(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition261(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition262(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition263(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition264(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219244)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition265(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition266(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition267(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition268(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition269(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition270(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition271(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition272(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition273(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition274(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition275(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition276(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition277(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition278(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition279(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition280(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition281(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition282(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition283(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition284(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition285(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition286(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition287(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition288(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition289(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition290(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition291(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition292(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition293(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition294(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition295(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition296(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition297(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition298(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition299(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition300(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition301(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition302(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition303(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition304(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition305(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition306(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition307(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition308(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition309(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition310(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition311(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition312(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition313(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition314(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition315(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition316(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition317(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition318(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition319(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition320(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition321(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition322(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition323(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition324(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition325(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition326(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition327(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition328(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219246)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition329(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition330(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition331(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition332(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition333(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition334(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition335(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition336(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition337(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition338(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition339(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition340(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition341(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition342(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition343(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition344(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition345(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition346(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition347(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition348(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition349(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition350(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition351(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition352(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition353(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition354(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition355(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition356(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition357(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition358(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition359(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition360(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition361(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition362(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition363(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition364(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition365(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition366(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition367(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition368(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition369(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition370(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition371(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition372(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition373(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition374(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition375(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition376(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition377(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition378(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition379(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition380(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition381(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition382(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition383(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition384(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition385(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition386(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition387(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition388(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition389(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition390(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition391(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition392(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219247)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition393(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition394(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition395(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition396(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition397(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition398(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition399(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition400(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition401(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition402(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition403(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition404(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition405(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition406(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition407(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition408(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition409(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition410(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition411(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition412(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition413(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition414(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition415(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition416(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition417(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition418(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition419(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition420(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition421(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition422(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition423(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition424(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition425(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition426(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition427(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition428(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition429(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition430(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition431(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition432(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition433(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition434(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition435(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition436(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition437(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition438(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition439(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition440(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition441(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition442(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition443(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition444(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition445(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition446(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition447(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition448(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition449(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition450(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition451(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition452(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition453(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition454(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition455(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition456(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219248)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition457(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition458(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition459(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition460(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition461(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition462(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition463(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition464(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition465(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition466(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition467(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition468(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition469(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition470(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition471(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition472(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition473(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition474(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition475(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition476(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition477(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition478(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition479(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition480(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition481(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition482(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition483(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition484(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition485(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition486(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition487(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition488(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition489(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition490(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition491(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition492(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition493(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition494(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition495(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition496(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition497(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition498(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition499(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition500(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition501(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition502(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition503(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition504(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition505(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition506(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition507(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition508(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition509(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition510(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition511(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition512(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition513(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition514(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition515(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition516(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition517(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition518(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition519(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition520(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219249)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition521(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition522(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition523(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition524(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition525(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition526(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition527(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition528(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition529(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition530(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition531(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition532(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition533(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition534(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition535(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition536(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition537(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition538(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition539(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition540(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition541(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition542(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition543(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition544(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition545(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition546(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition547(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition548(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition549(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition550(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition551(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition552(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition553(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition554(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition555(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition556(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition557(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition558(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition559(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition560(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition561(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition562(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition563(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition564(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition565(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition566(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition567(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition568(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition569(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition570(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition571(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition572(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition573(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition574(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition575(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition576(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition577(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition578(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition579(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition580(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition581(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition582(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition583(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition584(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219251)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition585(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition586(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition587(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition588(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition589(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition590(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition591(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition592(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition593(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition594(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition595(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition596(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition597(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition598(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition599(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition600(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition601(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition602(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition603(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition604(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition605(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition606(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition607(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition608(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition609(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition610(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition611(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition612(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition613(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition614(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition615(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition616(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition617(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition618(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition619(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition620(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition621(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition622(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition623(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition624(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition625(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition626(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition627(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition628(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition629(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition630(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition631(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition632(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition633(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition634(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition635(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition636(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition637(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition638(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition639(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition640(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition641(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition642(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition643(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition644(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition645(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition646(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition647(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition648(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219252)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition649(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b0").goTo("a1b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition650(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b1").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition651(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b2").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition652(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b3").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition653(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b4").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition654(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b5").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition655(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b6").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition656(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b7").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition657(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b8").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition658(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b9").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition659(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b10").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition660(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b11").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition661(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b12").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition662(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b13").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition663(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b14").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition664(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a0b15").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition665(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b0").goTo("a2b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition666(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b1").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition667(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b2").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition668(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b3").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition669(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b4").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition670(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b5").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition671(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b6").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition672(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b7").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition673(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b8").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition674(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b9").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition675(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b10").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition676(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b11").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition677(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b12").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition678(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b13").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition679(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b14").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition680(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a1b15").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition681(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b0").goTo("a3b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition682(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b1").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition683(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b2").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition684(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b3").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition685(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b4").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition686(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b5").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition687(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b6").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition688(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b7").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition689(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b8").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition690(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b9").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition691(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b10").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition692(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b11").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition693(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b12").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition694(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b13").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition695(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b14").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition696(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a2b15").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition697(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b0").goTo("a4b0");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition698(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b1").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition699(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b2").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition700(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b3").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition701(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b4").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition702(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b5").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition703(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b6").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition704(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b7").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition705(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b8").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition706(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b9").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition707(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b10").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition708(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b11").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition709(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b12").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition710(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b13").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition711(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b14").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition712(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219253)).from("a3b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition713(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b0").goTo("a0b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition714(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b1").goTo("a0b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition715(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b2").goTo("a0b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition716(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b3").goTo("a0b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition717(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b4").goTo("a0b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition718(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b5").goTo("a0b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition719(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b6").goTo("a0b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition720(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b7").goTo("a0b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition721(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b8").goTo("a0b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition722(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b9").goTo("a0b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition723(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b10").goTo("a0b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition724(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b11").goTo("a0b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition725(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b12").goTo("a0b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition726(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b13").goTo("a0b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition727(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a0b14").goTo("a0b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition728(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b0").goTo("a1b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition729(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b1").goTo("a1b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition730(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b2").goTo("a1b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition731(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b3").goTo("a1b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition732(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b4").goTo("a1b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition733(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b5").goTo("a1b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition734(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b6").goTo("a1b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition735(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b7").goTo("a1b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition736(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b8").goTo("a1b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition737(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b9").goTo("a1b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition738(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b10").goTo("a1b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition739(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b11").goTo("a1b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition740(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b12").goTo("a1b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition741(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b13").goTo("a1b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition742(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a1b14").goTo("a1b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition743(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b0").goTo("a2b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition744(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b1").goTo("a2b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition745(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b2").goTo("a2b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition746(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b3").goTo("a2b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition747(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b4").goTo("a2b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition748(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b5").goTo("a2b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition749(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b6").goTo("a2b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition750(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b7").goTo("a2b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition751(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b8").goTo("a2b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition752(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b9").goTo("a2b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition753(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b10").goTo("a2b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition754(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b11").goTo("a2b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition755(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b12").goTo("a2b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition756(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b13").goTo("a2b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition757(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a2b14").goTo("a2b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition758(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b0").goTo("a3b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition759(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b1").goTo("a3b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition760(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b2").goTo("a3b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition761(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b3").goTo("a3b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition762(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b4").goTo("a3b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition763(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b5").goTo("a3b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition764(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b6").goTo("a3b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition765(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b7").goTo("a3b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition766(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b8").goTo("a3b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition767(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b9").goTo("a3b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition768(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b10").goTo("a3b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition769(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b11").goTo("a3b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition770(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b12").goTo("a3b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition771(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b13").goTo("a3b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition772(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a3b14").goTo("a3b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition773(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b0").goTo("a4b1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition774(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b1").goTo("a4b2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition775(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b2").goTo("a4b3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition776(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b3").goTo("a4b4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition777(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b4").goTo("a4b5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition778(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b5").goTo("a4b6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition779(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b6").goTo("a4b7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition780(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b7").goTo("a4b8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition781(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b8").goTo("a4b9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition782(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b9").goTo("a4b10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition783(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b10").goTo("a4b11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition784(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b11").goTo("a4b12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition785(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b12").goTo("a4b13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition786(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b13").goTo("a4b14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition787(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(219245)).from("a4b14").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition788(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 31, 0)).from("a4b15").goTo("a4b15");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition789(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1009, 0)).from("a4b15").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition790(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition791(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition792(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition793(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition794(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition795(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition796(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition797(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition798(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition799(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition800(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition801(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition802(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition803(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition804(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition805(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition806(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition807(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(831831, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 4742870L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 186000469, 150L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
