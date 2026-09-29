package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DataDriven 真端驱动门禁（P5-1 起，按批推进）。判据 = 真端表 + 客户端契约：P5-1 覆盖
 * "Talk 接取 + 单块 Hunt 进度"（复用 hunt 计数网格），其余步骤类型按批补齐并保持
 * {@code FAMILY_PENDING}。
 * <p>
 * 断言四件事：家族规模冻结 / 采纳数量下限 / 漂移登记同步 / 冻结 IR 指纹 + 保留清单一致。
 * Retail-semantics gate for the DataDriven family (batch-wise rollout).
 */
class RetailDataDrivenGateTest {

	private static final String CATALOG = "/aion/data/static_data/quest_definition/quest_definition_catalog.xml";
	private static final String RETENTION = "/aion/data/static_data/quest_retail/retail-xml-retention.tsv";
	/** 漂移登记（P5-1 覆盖行 + 未覆盖行均登记）。 / Drift registry. */
	private static final String DRIFT_REGISTRY = "/quest/retail-data-driven-drift.tsv";
	/** 冻结 IR 指纹。 / Frozen IR fingerprints. */
	private static final String FINGERPRINTS = "/quest/retail-data-driven-ir-fingerprints.tsv";
	/** 冻结的家族规模。 / Frozen family size. */
	private static final int FROZEN_FAMILY_SIZE = 1508; // 家族=（catalog∪retired)∩表，翻转只换 owner 不减员；并行会话家族表并入行当前解析器不可见（其解析器改动未落地），钉解析实况
	/** 可驱动数量下限（三族混合链切片 + 25050/25082 curated 退回）。 / Floor for retail-drivable quests. */
	private static final int ACCEPTED_FLOOR = 1091;
	/** 已退役子集冻结规模（P5-1..P5-4 + 混合链切片采纳批）。 / Retired subset frozen size (batches). */
	private static final int FROZEN_RETIRED_SIZE = 1091;

	/**
	 * 暂缓采纳（编译可过但遗留合同测试仍锁定旧行为；逐条给出裁定理由，落定前保留 XML）。
	 * 16803/16804/26803/26804 曾因对齐测试锁定迁移合同暂缓；测试已重塑为真端网格合同（P5-4 收口），
	 * 当前为空。后续如需暂缓，按同一模式登记 id → 理由。
	 * Curated deferrals: rows that compile but whose legacy contract tests still pin the migrated
	 * behavior. 16803/16804/26803/26804 were deferred until their alignment tests were reshaped to
	 * the retail grid contract (done in the P5-4 closeout); the registry is currently empty.
	 */
/**
	 * curated 暂缓登记（编译可过但真端词汇缺机制）：25050 的仪式召唤/消散与祭坛供奉推进
	 * 超出混合采集链词汇（m3d 退回）。后续如需按行暂缓，登记 (quest_id → 理由)。
	 * Curated deferrals (compilable but the retail vocabulary lacks the mechanics): 25050's
	 * ritual spawn/despawn and altar offering advances exceed the mixed collect-chain vocabulary
	 * (m3d downgrade). To defer a row, register (quest_id → reason) here.
	 */
	private static final Map<Integer, String> CURATED_DEFERRED = Map.of(
		25050, "collect-chain vocabulary lacks the ritual spawn/despawn + altar offering advances "
			+ "(the legacy contract test pins them; m3d downgrade)",
		25082, "collect-chain vocabulary lacks the ritual spawn/despawn advances "
			+ "(the legacy contract pins them; m3d downgrade)",
		19900, "minion tutorial prerequisite axis (1007) is unexpressed in the retail metadata and "
			+ "the legacy contract pins the LevelUp+EnterWorld dual accept at 1500ms",
		29900, "minion tutorial prerequisite axis (2009) is unexpressed in the retail metadata and "
			+ "the legacy contract pins the LevelUp+EnterWorld dual accept at 1500ms",
		// P0c-53：活动商人采集行经组表（event_npc_idsolo_s4，组员 LC1_/DC1_）解析后编译可过，但客户端
		// 任务书未声明采集模板会下发的接取页（1002→1003 / 1003→1004）⇒ 客户端契约门判
		// PAGE_NOT_IN_TASK_HTML（12 条）⇒ 如实保留 XML；同一组名的 hunt 行（50091/50092）页形不同、已采纳。
		// P0c-53: the event-vendor collect rows compile once the group table resolves their members, but
		// the client task HTML does not declare the accept pages (1002->1003 / 1003->1004) the collect
		// template emits, so the client contract gate reports PAGE_NOT_IN_TASK_HTML (12 rows) and the
		// rows stay on XML; the same group name's hunt rows (50091/50092) have a different page shape
		// and were adopted.
		50089, "event-vendor collect row: the client task HTML does not declare the accept pages "
			+ "(1002->1003 / 1003->1004) emitted for the declared group members "
			+ "(QuestClientContractGateTest PAGE_NOT_IN_TASK_HTML)",
		50090, "event-vendor collect row: the client task HTML does not declare the accept pages "
			+ "(1002->1003 / 1003->1004) emitted for the declared group members "
			+ "(QuestClientContractGateTest PAGE_NOT_IN_TASK_HTML)",
		25051, "TalkFOBJ relative monster spawn axis exceeds the standard talk+hunt vocabulary "
			+ "(the legacy contract pins it; curated deferral)");
	private static final String CURATED_CODE = "CURATED_LEGACY_CONTRACT_LOCK";

	private static RetailDataDrivenTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static RetailClientSummaryRows clientSummaryRows;
	private static RetailClientRewardNpcs clientRewardNpcs;
	/** 客户端交付型对话页（决定合成形状，必须与生产一致）。 / Client hand-in pages; must match production. */
	private static RetailClientHandinPages clientHandinPages;
	private static RetailQuestUseItemNpcs interactionObjects;
	private static RetailClientTalkChainPages clientTalkChainPages;
	private static RetailClientTalkCollectChainPages clientTalkCollectChainPages;
	private static RetailClientKillTargets clientKillTargets;
	private static RetailClientHuntProgressRows clientHuntProgressRows;
	private static RetailEnterAreaZoneResolution enterAreaZoneResolution;
	private static RetailClientDialogExits clientDialogExits;
	private static RetailQuestAreaIndex clientQuestAreas;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Set<Integer> familyIds;
	private static Map<Integer, String> driftRegistry;
	private static Map<Integer, String> fingerprints;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest_retail/data_driven_quest.xml")) {
			table = RetailDataDrivenTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		clientSummaryRows = RetailClientSummaryRows.defaultSummaryRows();
		clientHandinPages = RetailClientHandinPages.defaultHandinPages();
		clientTalkChainPages = RetailClientTalkChainPages.defaultTalkChainPages();
		clientTalkCollectChainPages = RetailClientTalkCollectChainPages.defaultTalkCollectChainPages();
		clientKillTargets = RetailClientKillTargets.defaultKillTargets();
		clientHuntProgressRows = RetailClientHuntProgressRows.defaultHuntProgressRows();
		enterAreaZoneResolution = RetailEnterAreaZoneResolution.defaultZoneResolution();
		clientDialogExits = RetailClientDialogExits.defaultExits();
		clientRewardNpcs = RetailClientRewardNpcs.defaultRewardNpcs();
		try (InputStream input = open("/aion/definitions/compact/ai/ai-areas.xml")) {
			clientQuestAreas = RetailQuestAreaIndex.load(input);
		}
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR(), NPC_TEMPLATES()), RetailQuestAiNameGroupsFixture.streams());
		interactionObjects = RetailQuestUseItemNpcs.fromIds(npcIndex.questUseItemNpcIds());
		itemIndex = RetailItemNameIndex.build(openAll(ITEM_DIR(), listXmlNames(ITEM_DIR())));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		familyIds = familyIds();
		driftRegistry = driftRegistry();
		fingerprints = fingerprints();
	}

	private static String NPC_DIR() {
		return "/aion/data/static_data/npcs/";
	}

	private static String ITEM_DIR() {
		return "/aion/data/static_data/items/item/";
	}

	private static List<String> NPC_TEMPLATES() {
		return List.of("npc_template_200000_216188.xml", "npc_template_216189_235748.xml",
			"npc_template_235749_247606.xml", "npc_template_247607_270057.xml", "npc_template_270058_286320.xml",
			"npc_template_286321_800030.xml", "npc_template_800031_834289.xml", "npc_template_834290_885645.xml");
	}

	@Test
	void familyScopeIsFrozen() {
		assertTrue(familyIds.size() == FROZEN_FAMILY_SIZE,
			() -> "DataDriven 家族规模漂移：期望 " + FROZEN_FAMILY_SIZE + " 实际 " + familyIds.size());
	}

	/** 采纳行必须落在 P5 已覆盖形状内（Talk 接取 + 单块 Hunt 或纯 CollectItem）。 / Accepted rows stay in a covered P5 shape. */
	@Test
	void acceptedDefinitionsStayInCoveredScope() {
		int accepted = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			var outcome = compile(questId);
			if (!outcome.accepted()) {
				continue;
			}
			accepted++;
			var entry = table.find(questId).orElseThrow();
			boolean talkAcquire = "talk".equalsIgnoreCase(entry.acquireCategory());
			// P5-4：系统发放（EnterArea/none，世界 quest_area 进区域发放，isBound 判定在 requireAcquire）。
			// P5-4: system grant (EnterArea/none) via world quest-area entry; isBound enforced by requireAcquire.
			boolean systemAcquire = "enterarea".equalsIgnoreCase(entry.acquireCategory())
				|| "none".equalsIgnoreCase(entry.acquireCategory());
			// 顺序 SECTION 链切片：多段 hunt 行（2..5 段）由 compileSequentialStages 合成——客户端
			// quest_monster.csv 只在当前段列目标怪，链式前缀状态、乱序不计数；5 段是布局硬上限
			// （SECTION_5 是简报标志位，6 段越 32 位），超限行在编译器被拒、不会出现在采纳集。
			// Sequential SECTION-chain slice: multi-stage hunt rows (2..5 stages) synthesize via
			// compileSequentialStages — the client journal lists only the current stage's targets
			// (chained prefix states, out-of-order kills never count); 5 stages is the layout cap
			// and over-cap rows are rejected before reaching the accepted set.
			boolean huntScope = talkAcquire && entry.allHunt() && entry.huntStages().size() >= 1
				&& entry.huntStages().size() <= 5;
			boolean areaHuntScope = systemAcquire && entry.allHunt() && entry.huntStages().size() >= 1
				&& entry.huntStages().size() <= 5;
			boolean collectScope = talkAcquire && entry.allCollect();
			boolean talkScope = talkAcquire && entry.noProgress();
			// 物品接取的无进度行（P0c-55，13952 形）：真端接取类别 = ItemPlay（参数是任务起始道具），
			// 无进度步 ⇒ 接取段 = 使用道具开窗 + 无主接受/拒绝，报告/完成流与 talkScope 同一装配。
			// Item-acquired no-progress rows (P0c-55, the 13952 shape): acquire category ItemPlay whose
			// parameter is a quest-start item; the accept segment is the item use plus targetless
			// accept/refuse, and the report/completion flows share the talkScope assembly.
			boolean itemAcquireNoProgressScope = "itemplay".equalsIgnoreCase(entry.acquireCategory())
				&& entry.noProgress();
			boolean chainScope = (talkAcquire || systemAcquire) && entry.allTalk();
			// 混合长尾切片 1：talk/hunt 交错行（块序混合链合成器）。
			// Mixed-tail slice 1: talk/hunt interleave rows (the block-order mixed-chain compiler).
			boolean talkHuntMixScope = talkAcquire && entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("hunt"))
				&& entry.stepCategories().contains("talk") && entry.stepCategories().contains("hunt");
			// 混合长尾切片 2：talk/collectitem 交错行（单 var0 阶梯混合采集链合成器）。
			// Mixed-tail slice 2: talk/collectitem interleave rows (the single-ladder mixed
			// collect-chain compiler).
			boolean talkCollectMixScope = talkAcquire && entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("collectitem"))
				&& entry.stepCategories().contains("talk") && entry.stepCategories().contains("collectitem");
			// 链式接取（none）混合链（2026-09-26 续片 19）：前序任务完成 / 区域任务结束事件自动发放，
			// 无接取 NPC（narrow 编译器 -1 哨兵发 level-up + zone-mission-end 边，entry_page 不参与）；
			// 步词汇与 talk 接取的混合链同域（10011/10501..10507/20011/20502..20507 十二行）。
			// Chain-acquired (none) mixed chains (the 2026-09-26 slice 19): granted by the prerequisite
			// completion or zone-mission-end event with no acquire npc (the narrow compiler's -1
			// sentinel emits the level-up and zone-mission-end edges and never reads entry_page); the
			// step vocabulary is the talk-acquire mixed-chain one (the twelve 10011/10501/20502 rows).
			boolean noneAcquireMixScope = "none".equalsIgnoreCase(entry.acquireCategory())
				&& !entry.stepCategories().isEmpty()
				&& entry.stepCategories().stream().allMatch(
					category -> category.equals("talk") || category.equals("collectitem")
						|| category.equals("hunt") || category.equals("enterarea")
						|| category.equals("enterworld") || category.equals("itemplay")
						|| category.equals("talkfobj"));
			// 混合长尾切片 3：talk/collectitem/hunt 三族交错行（同一顺序链：信件梯 + 检查对 + 计数段）。
			// Mixed-tail slice 3: talk/collectitem/hunt interleave rows (one sequential chain:
			// letter ladders, the check pair and counter stages).
			boolean talkCollectHuntMixScope = talkAcquire && entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("collectitem")
					|| category.equals("hunt"))
				&& entry.stepCategories().contains("talk") && entry.stepCategories().contains("hunt")
				&& entry.stepCategories().contains("collectitem");
			// 混合长尾切片 4：enterarea 交织行（EA 步占行不占对话段；块级混合链消费）。
			// Mixed-tail slice 4: enterarea interleave rows (EA steps occupy a journal row but no
			// dialog segment; consumed by the block-order chain compiler).
			boolean eaMixScope = talkAcquire && entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("collectitem")
					|| category.equals("hunt") || category.equals("enterarea"))
				&& entry.stepCategories().contains("enterarea");
			// EnterArea 接取（LevelUp/ZoneMissionEnd 自动发放）与 enterarea 步交织的混合链（16823 形）。
			// EnterArea-acquired (LevelUp/ZoneMissionEnd auto grant) mixed chains interleaving EA
			// steps (the 16823 shape).
			boolean eaAcquireMixScope = systemAcquire && entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("collectitem")
					|| category.equals("hunt") || category.equals("enterarea"))
				&& !entry.stepCategories().isEmpty();
			// hunt/enterarea 交错（无对话段；13956 形：EA → 首领 → EA 链，接取形不限）。
			// A hunt/enterarea interleave with no dialog stage (the 13956 shape: EA, boss, EA chain;
			// any acquire form).
			boolean huntEaMixScope = entry.stepCategories().stream().allMatch(
				category -> category.equals("hunt") || category.equals("enterarea"))
				&& entry.stepCategories().contains("hunt")
				&& entry.stepCategories().contains("enterarea");
			// hunt/collectitem 交错（无 talk；15608 形：EA → 采集 → EA → 首领；80848 形：首领 → 交付；
			// 2026-09-26 续片 20 的混合链词汇扩展）。
			// A hunt/collectitem interleave without talk (the 15608 shape: EA → collect → EA → hunt;
			// the 80848 shape: hunt → hand-in) — the mixed-chain vocabulary extension of slice 20.
			boolean huntCollectMixScope = entry.stepCategories().contains("hunt")
				&& entry.stepCategories().contains("collectitem")
				&& entry.stepCategories().stream().allMatch(
					category -> category.equals("hunt") || category.equals("collectitem")
						|| category.equals("enterarea") || category.equals("enterworld")
						|| category.equals("itemplay") || category.equals("talkfobj"));
			// 纯 enterarea 链（无计数无对话；16831 形：连续区域推进）。
			// A pure enterarea chain with no counters or dialogs (the 16831 shape).
			boolean eaOnlyScope = entry.stepCategories().stream().allMatch(
				category -> category.equals("enterarea"))
				&& !entry.stepCategories().isEmpty();
			// 混合长尾切片 5：itemplay 交织行（ItemPlay 步占行不占对话段；Talk 接取 + 单步切片 1）。
			// Mixed-tail slice 5: itemplay interleave rows (itemplay steps occupy a journal row but
			// no dialog segment; slice 1 = Talk acquire plus a single step).
			boolean itemPlayMixScope = talkAcquire && entry.stepCategories().stream().allMatch(
				category -> category.equals("talk") || category.equals("collectitem")
					|| category.equals("hunt") || category.equals("enterarea")
					|| category.equals("itemplay"))
				&& entry.stepCategories().contains("itemplay");
			// LevelUpLogIn 接取（P5-5）：升级事件自动接取 + ItemPlay 步链（遗留 13830 形）。
			// LevelUpLogIn acquire (P5-5): the level-up event auto-grants plus the itemplay step
			// chain (the legacy 13830 shape).
			boolean levelUpAcquireScope = "leveluplogin".equalsIgnoreCase(entry.acquireCategory())
				&& entry.stepCategories().stream().allMatch(
					category -> category.equals("talk") || category.equals("collectitem")
						|| category.equals("hunt") || category.equals("enterarea")
						|| category.equals("itemplay"));
			// P5-4：纯 PVP 计数网格（KillInWorld(0) 通配；NPC 接取与世界 quest_area 发放皆可）。
			boolean pvpScope = entry.allPvp();
			// 链内 PVP 计数（2026-09-26 续片 22）：PVP 步与其他类别交错（80846 形 pvp+collectitem、
			// 15673 形 enterarea+pvp、9639 形 hunt+hunt+pvp+talk+hunt），接取形不限；纯 PVP 行由上方
			// pvpScope 覆盖（本判据的 allPvp 取反把它排除在外）。
			// In-chain PVP counters (the 2026-09-26 slice 22): a PVP step interleaved with other
			// categories (the 80846 pvp+collectitem, 15673 enterarea+pvp and 9639
			// hunt+hunt+pvp+talk+hunt shapes), any acquire form; pure-PVP rows are covered by pvpScope
			// above (the !allPvp guard keeps them out of this predicate).
			boolean pvpMixScope = entry.stepCategories().contains("pvp") && !entry.allPvp()
				&& entry.stepCategories().stream().allMatch(
					category -> category.equals("talk") || category.equals("collectitem")
						|| category.equals("hunt") || category.equals("enterarea")
						|| category.equals("enterworld") || category.equals("itemplay")
						|| category.equals("talkfobj") || category.equals("pvp"));
			// 区域接取 + 交付即完成（无进度步；13842 形：真端世界文件绑定区域 → 进区域发放，
			// SystemGrant 边 + 交付 NPC 报告流；未绑定的同形行仍拒绝）。
			// Area acquire with a no-progress row (the 13842 shape: bound in the retail world files,
			// granted on area entry; SystemGrant edge plus the hand-in report flow).
			boolean areaNoProgressScope = "enterarea".equalsIgnoreCase(entry.acquireCategory())
				&& entry.stepCategories().isEmpty();
			// EnterWorld 接取混合链（副本任务，进世界 + WorldIs 发放；10010 形；EA 步同词汇）。
			// EnterWorld-acquired mixed chains (instance quests granted on world entry + WorldIs;
			// the 10010 shape; EA steps share the vocabulary).
			boolean worldMixScope = "enterworld".equalsIgnoreCase(entry.acquireCategory())
				&& entry.stepCategories().stream().allMatch(
					category -> category.equals("talk") || category.equals("collectitem")
						|| category.equals("hunt") || category.equals("enterarea"));
			// EnterWorld 步骑行者（2026-09-26 续片 11）：进入指定世界推进（遗留 `<enter-world/>` +
			// `<world-is world-id>` 同形），无计数无对话段；本批采纳四形——纯 EW（50128 形）、
			// talk+EW（16835 形）、talk+talk+EW（13950 形）、hunt+EW（17552 形）。
			// EnterWorld rider steps (the 2026-09-26 slice 11): entering the named world advances the
			// row (the legacy enter-world plus world-is shape) with no counter and no dialog stage;
			// the adopted shapes are pure EW, talk+EW, talk+talk+EW and hunt+EW.
			boolean enterWorldRiderScope = entry.stepCategories().contains("enterworld")
				&& entry.stepCategories().stream().allMatch(
					category -> category.equals("talk") || category.equals("collectitem")
						|| category.equals("hunt") || category.equals("enterarea")
						|| category.equals("enterworld") || category.equals("itemplay")
						|| category.equals("talkfobj"));
			// TalkFOBJ/hunt 交错行（18931 形：FOBJ 物体交互步 → 首领击杀块；无 talk/collect）。
			// TalkFOBJ/hunt interleave rows (the 18931 shape: an fobj interaction step then a boss
			// kill block; no talk/collect steps).
			boolean talkFobjHuntScope = entry.stepCategories().contains("talkfobj")
				&& entry.stepCategories().contains("hunt")
				&& entry.stepCategories().stream().allMatch(
					category -> category.equals("talkfobj") || category.equals("hunt")
						|| category.equals("talk") || category.equals("collectitem")
						|| category.equals("enterarea") || category.equals("enterworld")
						|| category.equals("itemplay"));
			// 纯 TalkFOBJ 单步行（25070 形：与 FOBJ 交互即入领奖；采集要求/掉落源由门在路由侧拦下）。
			// Pure single-step TalkFOBJ rows (the 25070 shape: the fobj interaction alone lands on
			// reward; rows carrying collect requirements or drop sources are rejected by the router).
			boolean talkFobjOnlyScope = entry.stepCategories().equals(java.util.List.of("talkfobj"));
			assertTrue(huntScope || areaHuntScope || collectScope || talkScope || chainScope
					|| pvpScope || pvpMixScope || areaNoProgressScope || talkHuntMixScope
					|| talkCollectMixScope
					|| talkCollectHuntMixScope || eaMixScope || eaAcquireMixScope || huntEaMixScope
					|| eaOnlyScope || worldMixScope || itemPlayMixScope || levelUpAcquireScope
					|| enterWorldRiderScope || noneAcquireMixScope || huntCollectMixScope
					|| talkFobjHuntScope || talkFobjOnlyScope || itemAcquireNoProgressScope,
				questId + ": 采纳行越出 P5 已覆盖形状");
		}
		int drivable = accepted;
		assertTrue(drivable >= ACCEPTED_FLOOR, () -> "DataDriven 可驱动数量回退："
			+ drivable + " < " + ACCEPTED_FLOOR);
	}

	/** 漂移登记与逐任务分类全量一致；retired 只豁免壳 XML 存在，不豁免登记同步。 /
	 * Drift registry matches classification for every family row; retirement exempts the XML shell,
	 * not registry synchronization. */
	@Test
	void driftVersusShellsIsRegistered() throws Exception {
		Map<Integer, String> classification = classify();
		List<String> problems = new ArrayList<>();
		for (int questId : new TreeSet<>(driftRegistry.keySet())) {
			String expected = driftRegistry.get(questId);
			String actual = classification.get(questId);
			if (!expected.equals(actual)) {
				problems.add(questId + ": 登记=" + expected + " 实际=" + actual);
			}
		}
		for (int questId : new TreeSet<>(classification.keySet())) {
			if (!driftRegistry.containsKey(questId)) {
				problems.add(questId + ": 未登记（" + classification.get(questId) + "）");
			}
		}
		String dump = System.getProperty("retail.dataDriven.equivOut");
		if (dump != null) {
			Files.writeString(Path.of(dump), dumpText(classification));
		}
		assertTrue(driftRegistry.size() == FROZEN_FAMILY_SIZE,
			() -> "漂移登记行数漂移：" + driftRegistry.size());
		assertTrue(problems.isEmpty(), () -> "DataDriven 漂移登记失同步："
			+ problems.stream().limit(20).toList());
	}

	/** 冻结 IR 指纹：壳 XML 删除后仍能发现静默漂移。 / Frozen fingerprints, the post-deletion drift guard. */
	@Test
	void frozenFingerprintsCoverExactlyTheRetiredQuests() throws Exception {
		String freezeOut = System.getProperty("retail.dataDriven.fpOut");
		Map<Integer, String> actual = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			var outcome = compile(questId);
			if (!outcome.accepted()) {
				continue;
			}
			actual.put(questId, RetailIrFingerprint.fingerprint(outcome.definition().definition()));
		}
		if (freezeOut != null) {
			StringBuilder text = new StringBuilder("# DataDriven 冻结 IR 指纹（壳 XML 删除后继续守语义证据）\n"
				+ "# quest_id\tretail_fingerprint\tnodes\ttransitions\n");
			for (Map.Entry<Integer, String> entry : actual.entrySet()) {
				if (!RetiredQuestIds.contains(entry.getKey())) {
					continue;
				}
				QuestDefinition definition = compile(entry.getKey()).definition().definition();
				text.append(entry.getKey()).append('\t').append(entry.getValue()).append('\t')
					.append(definition.nodes().size()).append('\t').append(definition.transitions().size())
					.append('\n');
			}
			Files.writeString(Path.of(freezeOut), text.toString());
			fingerprints = fingerprints();
		}
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, String> entry : fingerprints.entrySet()) {
			String observed = actual.get(entry.getKey());
			if (observed == null) {
				problems.add(entry.getKey() + ": 冻结指纹存在但当前不可驱动");
			} else if (!observed.equals(entry.getValue())) {
				problems.add(entry.getKey() + ": 指纹漂移 " + entry.getValue() + " -> " + observed);
			}
		}
		Set<Integer> retiredInFamily = new TreeSet<>(RetiredQuestIds.all());
		retiredInFamily.retainAll(familyIds);
		assertTrue(fingerprints.size() == retiredInFamily.size(),
			() -> "冻结集合与退役集合不一致：frozen=" + fingerprints.size() + " retired=" + retiredInFamily.size());
		assertTrue(problems.isEmpty(), () -> "DataDriven 冻结指纹失同步："
			+ problems.stream().limit(20).toList());
	}

	/** 保留清单：采纳行必须 RETAIL_TABLE。 / Manifest agreement with the driver. */
	@Test
	void retentionManifestMatchesDriverOwnership() throws Exception {
		Map<Integer, String> owners = retentionOwners();
		List<String> problems = new ArrayList<>();
		int owned = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			var outcome = compile(questId);
			String owner = owners.get(questId);
			if (outcome.accepted() && "RETAIL_TABLE".equals(owner)) {
				owned++;
				continue;
			}
			if (outcome.accepted() && "XML_RETENTION".equals(owner) && !RetiredQuestIds.contains(questId)) {
				problems.add(questId + ": 可驱动却被留在 XML");
			}
		}
		int ownedSnapshot = owned;
		assertTrue(ownedSnapshot >= FROZEN_RETIRED_SIZE,
			() -> "真端驱动拥有的任务数量回退：" + ownedSnapshot + " < " + FROZEN_RETIRED_SIZE);
		assertTrue(problems.isEmpty(), () -> "保留清单与真端驱动不一致："
			+ problems.stream().limit(20).toList());
	}

	/** 暂缓采纳警卫：curated 行必须仍留 XML，且登记为 curated 拒绝码（裁定落定前不得悄悄采纳）。
	 * Curated-deferral guard: deferred rows stay on XML with the curated rejection code. */
	@Test
	void curatedDeferralsStayOnXmlUntilReshaped() throws Exception {
		Map<Integer, String> owners = retentionOwners();
		Map<Integer, String> classification = classify();
		for (Map.Entry<Integer, String> deferral : CURATED_DEFERRED.entrySet()) {
			int questId = deferral.getKey();
			assertEquals("XML_RETENTION", owners.get(questId),
				questId + " 暂缓期间必须保留 XML");
			assertEquals("REJECTED:" + CURATED_CODE, classification.get(questId),
				questId + " 暂缓码必须一致：" + deferral.getValue());
			assertTrue(!RetiredQuestIds.contains(questId),
				() -> questId + " 暂缓行不得退役");
		}
	}

	// ------------------------------------------------------------------ 分类与装载

	/** 逐任务编译结果缓存（5 个用例各自遍历 1508 行，缓存把总耗时降到一趟）。 / Per-quest compile cache. */
	private static final Map<Integer, RetailDataDrivenDefinitionCompiler.Outcome> COMPILED = new java.util.HashMap<>();

	private static RetailDataDrivenDefinitionCompiler.Outcome compile(int questId) {
		var cached = COMPILED.get(questId);
		if (cached != null) {
			return cached;
		}
		var entry = table.find(questId).orElse(null);
		var outcome = CURATED_DEFERRED.containsKey(questId)
			? new RetailDataDrivenDefinitionCompiler.Outcome(null, CURATED_CODE,
				CURATED_DEFERRED.get(questId))
			: entry == null
				? new RetailDataDrivenDefinitionCompiler.Outcome(null, "RETAIL_ROW_MISSING", null)
				: RetailDataDrivenDefinitionCompiler.compile(entry, itemIndex, npcIndex,
					RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(), npcIndex,
						itemIndex, randomRewards, nameIds),
					clientRewardNpcs, clientQuestAreas,
					clientDialogExits, clientSummaryRows, clientHandinPages,
					interactionObjects, clientTalkChainPages, clientKillTargets,
					clientTalkCollectChainPages, clientHuntProgressRows, enterAreaZoneResolution);
			COMPILED.put(questId, outcome);
			return outcome;
	}

	private static Map<Integer, String> classify() {
		Map<Integer, String> classification = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			var outcome = compile(questId);
			if (outcome.accepted()) {
				classification.put(questId, "ADOPTED");
			} else {
				classification.put(questId, "REJECTED:" + outcome.rejectionCode());
			}
		}
		return classification;
	}

	private static String dumpText(Map<Integer, String> classification) {
		Map<String, Integer> histogram = new TreeMap<>();
		classification.values().forEach(kind -> histogram.merge(kind, 1, Integer::sum));
		StringBuilder text = new StringBuilder("# DataDriven 漂移登记（禁止手改；重算：-Dretail.dataDriven.equivOut=<path>）\n");
		histogram.forEach((kind, count) -> text.append("# ").append(kind).append('\t').append(count).append('\n'));
		classification.forEach((questId, kind) -> {
			text.append(questId).append('\t').append(kind);
			// 拒绝行附第三列 detail（装载端只取 parts[1]，向后兼容），排障不必重跑编译器。
			// Rejected rows carry the detail in a third column (the loader reads parts[1] only),
			// so diagnosis no longer requires a rerun.
			if (kind.startsWith("REJECTED:")) {
				String detail = compile(questId).detail();
				if (detail != null && !detail.isBlank()) {
					text.append('\t').append(detail.replace('\t', ' '));
				}
			}
			text.append('\n');
		});
		return text.toString();
	}

	private static Set<Integer> familyIds() throws Exception {
		String text = new String(open(CATALOG).readAllBytes(), StandardCharsets.UTF_8);
		Set<Integer> ids = new HashSet<>();
		var matcher = java.util.regex.Pattern.compile("<definition id=\"(\\d+)\"").matcher(text);
		while (matcher.find()) {
			ids.add(Integer.parseInt(matcher.group(1)));
		}
		ids.addAll(RetiredQuestIds.all());
		ids.retainAll(table.questIds());
		return ids;
	}

	private static Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new HashMap<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			owners.put(Integer.parseInt(parts[0]), parts[1]);
		}
		return owners;
	}

	private static Map<Integer, String> driftRegistry() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		try (InputStream input = open(DRIFT_REGISTRY)) {
			for (String line : lines(input)) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				rows.put(Integer.parseInt(parts[0]), parts[1]);
			}
		}
		return rows;
	}

	private static Map<Integer, String> fingerprints() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		try (InputStream input = open(FINGERPRINTS)) {
			for (String line : lines(input)) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				rows.put(Integer.parseInt(parts[0]), parts[1]);
			}
		}
		return rows;
	}

	private static InputStream open(String resource) {
		InputStream input = RetailDataDrivenGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}

	private static List<InputStream> openAll(String dir, List<String> names) {
		List<InputStream> streams = new ArrayList<>();
		for (String name : names) {
			streams.add(open(dir + name));
		}
		return streams;
	}

	private static List<String> listXmlNames(String dir) throws Exception {
		var url = RetailDataDrivenGateTest.class.getResource(dir);
		assertNotNull(url, "missing dir " + dir);
		java.io.File[] files = new java.io.File(url.toURI()).listFiles((directory, name) -> name.endsWith(".xml"));
		assertNotNull(files);
		return java.util.Arrays.stream(files).map(java.io.File::getName).sorted().toList();
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			List<String> lines = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			return lines;
		}
	}

	private static Map<Integer, Integer> nameIds() throws Exception {
		Map<Integer, Integer> ids = new HashMap<>();
		for (String line : lines(open("/aion/data/static_data/quest_retail/quest_name_string_ids.tsv"))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			ids.put(Integer.parseInt(parts[0].substring("STR_QUEST_NAME_Q".length())), Integer.parseInt(parts[1]));
		}
		return ids;
	}

	private static Map<String, Integer> randomRewardIds() throws Exception {
		Map<String, Integer> ids = new HashMap<>();
		var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(open("/aion/data/static_data/quest_random_rewards.xml"));
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			var element = (org.w3c.dom.Element) nodes.item(index);
			ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
				Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
		}
		return ids;
	}
}
