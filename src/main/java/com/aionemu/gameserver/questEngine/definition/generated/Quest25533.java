package com.aionemu.gameserver.questEngine.definition.generated;

import com.aionemu.gameserver.questEngine.definition.*;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.questEngine.model.*;
import java.util.*;

/** Generated from quest definition XML; edit the XML and regenerate. */
public final class Quest25533 {
	private Quest25533() {}

	public static CompiledQuestDefinition definition() {
		QuestDsl.QuestBuilder builder = QuestDsl.quest(25533)
			.version(1)
			.metadata(metadata());
		configureNodes(builder);
		addTransitions(builder);

		return builder.compile();
	}

	private static QuestMetadata metadata() {
		return new QuestMetadata("[Daily] Protect Saphora Forest", 1802185, 68, 998, Set.of("ASMODIANS"), "SEEN_MARKER", new RepeatPolicy(255, 0L, true, false), Set.of(), List.of(), List.of(new QuestReward("EXP", 0, 77328000L), new QuestReward("ITEM", 188054912, 1L)), List.of(), Set.of(), "", 0, 1, 1, false, false, false, 0, null, null, false, Set.of("ALL"), 0, "NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new QuestStartCondition("finished", 25550, 0)), Map.of());
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
		builder.node("h1", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 1))));
		builder.node("h2", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 2))));
		builder.node("h3", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 3))));
		builder.node("h4", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 4))));
		builder.node("h5", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 5))));
		builder.node("h6", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 6))));
		builder.node("h7", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 7))));
		builder.node("h8", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 8))));
		builder.node("h9", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 9))));
		builder.node("h10", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 10))));
		builder.node("h11", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 11))));
		builder.node("h12", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 12))));
		builder.node("h13", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 13))));
		builder.node("h14", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 14))));
		builder.node("h15", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 15))));
		builder.node("h16", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 16))));
		builder.node("h17", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 17))));
		builder.node("h18", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 18))));
		builder.node("h19", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 19))));
		builder.node("h20", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 20))));
		builder.node("h21", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 21))));
		builder.node("h22", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 22))));
		builder.node("h23", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 23))));
		builder.node("h24", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 24))));
		builder.node("h25", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 25))));
		builder.node("h26", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 26))));
		builder.node("h27", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 27))));
		builder.node("h28", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 28))));
		builder.node("h29", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 29))));
		builder.node("h30", new NodeProjection(QuestStatus.START, Map.ofEntries(Map.entry("var0", 30))));
		builder.node("reward", new NodeProjection(QuestStatus.REWARD, Map.ofEntries(Map.entry("var0", 30))));
		builder.node("complete", new NodeProjection(QuestStatus.COMPLETE, Map.of()));
	}

	private static void addTransitions(QuestDsl.QuestBuilder builder) {
		addTransitionBatch0(builder);
		addTransitionBatch1(builder);
		addTransitionBatch2(builder);
		addTransitionBatch3(builder);
		addTransitionBatch4(builder);
		addTransitionBatch5(builder);
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
	}

	private static void addTransition0(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 31, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1011));
	}

	private static void addTransition1(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1007, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(4));
	}

	private static void addTransition2(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1002, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1003));
	}

	private static void addTransition3(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 20000, 0)).from("unaccepted").when(new QuestCondition.StartEligible()).goTo("started");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH));
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition4(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1003, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition5(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1004, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition6(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 20001, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.CloseDialog());
	}

	private static void addTransition7(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1008, 0)).from("unaccepted").goTo("unaccepted");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition8(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1008, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition9(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 31, 0)).from("started").goTo("started");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(1352));
	}

	private static void addTransition10(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition11(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition12(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition13(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition14(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition15(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition16(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition17(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition18(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition19(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition20(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition21(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition22(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition23(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition24(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition25(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition26(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition27(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition28(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition29(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition30(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition31(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition32(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("started").when(new QuestCondition.QuestVariableIs("var0", 0)).then(new QuestAction.SetVariable("var0", 1)).goTo("h1");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition33(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition34(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition35(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition36(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition37(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition38(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition39(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition40(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition41(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition42(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition43(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition44(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition45(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition46(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition47(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition48(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition49(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition50(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition51(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition52(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition53(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition54(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition55(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h1").when(new QuestCondition.QuestVariableIs("var0", 1)).then(new QuestAction.SetVariable("var0", 2)).goTo("h2");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition56(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition57(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition58(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition59(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition60(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition61(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition62(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition63(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition64(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition65(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition66(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition67(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition68(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition69(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition70(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition71(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition72(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition73(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition74(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition75(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition76(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition77(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition78(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h2").when(new QuestCondition.QuestVariableIs("var0", 2)).then(new QuestAction.SetVariable("var0", 3)).goTo("h3");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition79(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition80(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition81(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition82(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition83(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition84(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition85(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition86(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition87(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition88(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition89(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition90(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition91(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition92(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition93(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition94(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition95(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition96(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition97(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition98(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition99(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition100(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition101(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h3").when(new QuestCondition.QuestVariableIs("var0", 3)).then(new QuestAction.SetVariable("var0", 4)).goTo("h4");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition102(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition103(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition104(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition105(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition106(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition107(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition108(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition109(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition110(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition111(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition112(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition113(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition114(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition115(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition116(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition117(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition118(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition119(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition120(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition121(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition122(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition123(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition124(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h4").when(new QuestCondition.QuestVariableIs("var0", 4)).then(new QuestAction.SetVariable("var0", 5)).goTo("h5");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition125(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition126(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition127(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition128(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition129(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition130(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition131(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition132(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition133(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition134(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition135(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition136(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition137(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition138(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition139(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition140(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition141(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition142(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition143(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition144(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition145(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition146(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition147(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h5").when(new QuestCondition.QuestVariableIs("var0", 5)).then(new QuestAction.SetVariable("var0", 6)).goTo("h6");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition148(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition149(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition150(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition151(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition152(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition153(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition154(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition155(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition156(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition157(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition158(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition159(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition160(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition161(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition162(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition163(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition164(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition165(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition166(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition167(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition168(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition169(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition170(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h6").when(new QuestCondition.QuestVariableIs("var0", 6)).then(new QuestAction.SetVariable("var0", 7)).goTo("h7");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition171(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition172(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition173(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition174(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition175(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition176(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition177(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition178(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition179(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition180(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition181(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition182(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition183(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition184(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition185(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition186(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition187(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition188(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition189(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition190(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition191(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition192(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition193(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h7").when(new QuestCondition.QuestVariableIs("var0", 7)).then(new QuestAction.SetVariable("var0", 8)).goTo("h8");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition194(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition195(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition196(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition197(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition198(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition199(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition200(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition201(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition202(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition203(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition204(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition205(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition206(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition207(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition208(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition209(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition210(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition211(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition212(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition213(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition214(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition215(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition216(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h8").when(new QuestCondition.QuestVariableIs("var0", 8)).then(new QuestAction.SetVariable("var0", 9)).goTo("h9");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition217(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition218(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition219(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition220(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition221(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition222(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition223(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition224(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition225(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition226(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition227(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition228(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition229(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition230(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition231(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition232(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition233(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition234(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition235(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition236(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition237(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition238(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition239(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h9").when(new QuestCondition.QuestVariableIs("var0", 9)).then(new QuestAction.SetVariable("var0", 10)).goTo("h10");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition240(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition241(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition242(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition243(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition244(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition245(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition246(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition247(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition248(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition249(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition250(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition251(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition252(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition253(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition254(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition255(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition256(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition257(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition258(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition259(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition260(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition261(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition262(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h10").when(new QuestCondition.QuestVariableIs("var0", 10)).then(new QuestAction.SetVariable("var0", 11)).goTo("h11");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition263(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition264(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition265(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition266(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition267(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition268(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition269(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition270(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition271(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition272(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition273(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition274(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition275(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition276(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition277(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition278(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition279(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition280(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition281(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition282(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition283(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition284(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition285(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h11").when(new QuestCondition.QuestVariableIs("var0", 11)).then(new QuestAction.SetVariable("var0", 12)).goTo("h12");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition286(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition287(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition288(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition289(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition290(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition291(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition292(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition293(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition294(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition295(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition296(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition297(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition298(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition299(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition300(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition301(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition302(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition303(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition304(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition305(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition306(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition307(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition308(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h12").when(new QuestCondition.QuestVariableIs("var0", 12)).then(new QuestAction.SetVariable("var0", 13)).goTo("h13");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition309(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition310(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition311(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition312(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition313(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition314(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition315(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition316(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition317(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition318(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition319(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition320(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition321(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition322(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition323(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition324(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition325(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition326(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition327(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition328(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition329(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition330(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition331(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h13").when(new QuestCondition.QuestVariableIs("var0", 13)).then(new QuestAction.SetVariable("var0", 14)).goTo("h14");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition332(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition333(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition334(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition335(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition336(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition337(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition338(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition339(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition340(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition341(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition342(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition343(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition344(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition345(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition346(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition347(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition348(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition349(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition350(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition351(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition352(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition353(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition354(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h14").when(new QuestCondition.QuestVariableIs("var0", 14)).then(new QuestAction.SetVariable("var0", 15)).goTo("h15");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition355(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition356(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition357(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition358(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition359(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition360(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition361(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition362(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition363(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition364(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition365(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition366(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition367(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition368(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition369(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition370(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition371(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition372(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition373(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition374(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition375(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition376(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition377(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h15").when(new QuestCondition.QuestVariableIs("var0", 15)).then(new QuestAction.SetVariable("var0", 16)).goTo("h16");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition378(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition379(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition380(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition381(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition382(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition383(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition384(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition385(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition386(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition387(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition388(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition389(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition390(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition391(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition392(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition393(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition394(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition395(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition396(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition397(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition398(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition399(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition400(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h16").when(new QuestCondition.QuestVariableIs("var0", 16)).then(new QuestAction.SetVariable("var0", 17)).goTo("h17");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition401(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition402(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition403(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition404(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition405(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition406(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition407(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition408(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition409(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition410(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition411(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition412(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition413(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition414(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition415(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition416(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition417(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition418(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition419(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition420(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition421(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition422(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition423(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h17").when(new QuestCondition.QuestVariableIs("var0", 17)).then(new QuestAction.SetVariable("var0", 18)).goTo("h18");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition424(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition425(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition426(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition427(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition428(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition429(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition430(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition431(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition432(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition433(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition434(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition435(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition436(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition437(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition438(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition439(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition440(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition441(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition442(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition443(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition444(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition445(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition446(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h18").when(new QuestCondition.QuestVariableIs("var0", 18)).then(new QuestAction.SetVariable("var0", 19)).goTo("h19");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition447(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition448(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition449(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition450(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition451(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition452(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition453(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition454(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition455(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition456(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition457(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition458(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition459(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition460(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition461(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition462(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition463(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition464(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition465(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition466(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition467(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition468(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition469(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h19").when(new QuestCondition.QuestVariableIs("var0", 19)).then(new QuestAction.SetVariable("var0", 20)).goTo("h20");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition470(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition471(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition472(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition473(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition474(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition475(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition476(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition477(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition478(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition479(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition480(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition481(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition482(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition483(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition484(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition485(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition486(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition487(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition488(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition489(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition490(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition491(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition492(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h20").when(new QuestCondition.QuestVariableIs("var0", 20)).then(new QuestAction.SetVariable("var0", 21)).goTo("h21");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition493(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition494(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition495(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition496(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition497(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition498(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition499(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition500(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition501(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition502(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition503(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition504(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition505(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition506(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition507(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition508(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition509(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition510(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition511(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition512(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition513(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition514(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition515(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h21").when(new QuestCondition.QuestVariableIs("var0", 21)).then(new QuestAction.SetVariable("var0", 22)).goTo("h22");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition516(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition517(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition518(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition519(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition520(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition521(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition522(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition523(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition524(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition525(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition526(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition527(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition528(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition529(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition530(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition531(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition532(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition533(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition534(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition535(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition536(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition537(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition538(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h22").when(new QuestCondition.QuestVariableIs("var0", 22)).then(new QuestAction.SetVariable("var0", 23)).goTo("h23");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition539(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition540(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition541(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition542(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition543(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition544(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition545(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition546(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition547(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition548(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition549(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition550(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition551(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition552(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition553(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition554(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition555(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition556(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition557(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition558(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition559(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition560(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition561(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h23").when(new QuestCondition.QuestVariableIs("var0", 23)).then(new QuestAction.SetVariable("var0", 24)).goTo("h24");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition562(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition563(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition564(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition565(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition566(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition567(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition568(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition569(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition570(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition571(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition572(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition573(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition574(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition575(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition576(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition577(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition578(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition579(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition580(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition581(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition582(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition583(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition584(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h24").when(new QuestCondition.QuestVariableIs("var0", 24)).then(new QuestAction.SetVariable("var0", 25)).goTo("h25");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition585(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition586(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition587(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition588(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition589(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition590(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition591(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition592(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition593(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition594(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition595(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition596(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition597(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition598(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition599(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition600(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition601(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition602(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition603(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition604(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition605(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition606(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition607(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h25").when(new QuestCondition.QuestVariableIs("var0", 25)).then(new QuestAction.SetVariable("var0", 26)).goTo("h26");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition608(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition609(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition610(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition611(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition612(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition613(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition614(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition615(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition616(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition617(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition618(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition619(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition620(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition621(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition622(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition623(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition624(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition625(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition626(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition627(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition628(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition629(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition630(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h26").when(new QuestCondition.QuestVariableIs("var0", 26)).then(new QuestAction.SetVariable("var0", 27)).goTo("h27");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition631(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition632(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition633(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition634(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition635(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition636(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition637(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition638(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition639(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition640(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition641(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition642(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition643(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition644(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition645(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition646(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition647(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition648(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition649(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition650(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition651(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition652(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition653(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h27").when(new QuestCondition.QuestVariableIs("var0", 27)).then(new QuestAction.SetVariable("var0", 28)).goTo("h28");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition654(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition655(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition656(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition657(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition658(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition659(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition660(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition661(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition662(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition663(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition664(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition665(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition666(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition667(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition668(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition669(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition670(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition671(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition672(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition673(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition674(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition675(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition676(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h28").when(new QuestCondition.QuestVariableIs("var0", 28)).then(new QuestAction.SetVariable("var0", 29)).goTo("h29");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition677(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240467)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition678(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(240469)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition679(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237613)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition680(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237618)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition681(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(237623)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition682(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238840)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition683(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238845)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition684(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238850)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition685(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238855)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition686(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238860)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition687(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238865)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition688(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238870)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition689(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238875)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition690(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238880)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition691(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238885)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition692(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238890)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition693(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238895)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition694(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238900)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition695(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238905)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition696(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238910)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition697(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238915)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition698(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238920)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition699(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.KillNpc(238925)).from("h29").when(new QuestCondition.QuestVariableIs("var0", 29)).then(new QuestAction.SetVariable("var0", 30)).goTo("h30");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
	}

	private static void addTransition700(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1009, 0)).from("h30").goTo("reward");
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition701(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, -1, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition702(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 1009, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(5));
	}

	private static void addTransition703(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 31, 0)).from("reward").goTo("reward");
		builder.afterCommit(new AfterCommitAction.ShowQuestDialog(10002));
	}

	private static void addTransition704(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 8, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition705(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 9, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition706(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 10, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition707(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 11, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition708(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 12, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition709(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 13, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition710(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 14, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition711(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 15, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition712(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 16, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition713(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 17, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition714(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 18, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition715(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 19, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition716(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 20, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition717(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 21, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition718(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 22, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}

	private static void addTransition719(QuestDsl.QuestBuilder builder) {
		builder.on(new QuestEvent.TalkToNpc(806112, 23, 0)).from("reward").then(new QuestAction.GrantReward("EXP", 0, 77328000L, QuestRewardAmountMode.QUEST_BASE)).then(new QuestAction.GrantReward("ITEM", 188054912, 1L, QuestRewardAmountMode.EXACT)).then(new QuestAction.CompleteQuest(0)).goTo("complete");
		builder.afterCommit(new AfterCommitAction.RefreshPlayerStats());
		builder.afterCommit(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
		builder.afterCommit(new AfterCommitAction.ShowQuestSelectionDialog(10));
	}
}
