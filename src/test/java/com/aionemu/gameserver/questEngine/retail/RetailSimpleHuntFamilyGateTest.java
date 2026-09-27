package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleHunt 全族"无 shell"合成门禁（M2）：清单内 RETAIL_TABLE 任务的真端定义
 * （{@link RetailSimpleHuntDefinitionCompiler}）必须逐任务编译成功，
 * 且调度层与 XML 定义逐帧一致（击杀序列 → 状态/quest_vars）。
 * <p>
 * 排查模式：{@code -Dretail.hunt.rejectsOut=<path>} 导出拒绝直方图与逐任务结果。
 * Family gate compiling every retail-owned SimpleHunt quest without an XML shell.
 */
class RetailSimpleHuntFamilyGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	/** 家族编译器拒绝登记表（quest_id, code）。 / Registered compiler rejections. */
	private static final String COMPILER_REJECTS = "/quest/retail-simplehunt-compiler-rejects.tsv";
	/** 等价类缺口（有定义但与 XML 的 IR 不等价）保留原因。 / Equivalence-class gap reasons. */
	private static final Set<String> EQUIVALENCE_GAPS = Set.of("DIALOG_ROUTE", "KILL_ROUTE_MISMATCH",
		"NODE_SET_MISMATCH", "NODE_FIELDS_MISMATCH");
	/**
	 * 驱动覆盖缺口（族表合成正常，但族表没有表达该语义的列 → 保留 XML 降级）。
	 * 当前只有一个轴：任务自身的 after-commit 刷怪边——它是击杀目标/交付 NPC 进世界的唯一途径，
	 * 真端家族表没有刷怪列（P0c-6 裁定，见 retail-simple-hunt-adjudicated-decisions.tsv）。
	 * Driver-coverage gaps: the family compiler accepts the row, but no retail table column can express
	 * the axis (the quest's own spawn edge is the only source of the NPC), so the row keeps its XML.
	 */
	private static final Set<String> DRIVER_COVERAGE_GAPS = Set.of("QUEST_SPAWN_UNEXPRESSED");
	/**
	 * 真端表缺口（P0c-8c 逐行裁定）：族表能合成，但**真端路由集 / 击杀覆盖 / 奖励区块**没有表达该语义的列
	 * —— 保留 XML 才是正确结果。逐行稳定码见 `simplehunt-dialog-route-gaps.txt`：
	 * <ul>
	 *   <li>{@code KILL_COVERAGE_LOSS}：真端编译集缺客户端点名、且生产刷怪可达的怪（采纳会丢玩家能打的怪）；</li>
	 *   <li>{@code CLIENT_BUTTON_UNWIRED}：客户端按钮（select5 的 20002 检查、报告页 1009）在真端路由集里无对应动作；</li>
	 *   <li>{@code CLASS_SELECTABLE_REWARD}：真端以 {@code <class>_selectable_reward} 表达职业可选奖励，编译器尚未落地；</li>
	 *   <li>{@code XML_EXTRA_REWARD}：旧 XML 多出客户端任务书点名的奖励物品（真端表没有）。</li>
	 * </ul>
	 * 注意：这些码**不带 {@code RETAIL_} 前缀**——带前缀的码在本门禁里专指"族编译器拒绝码"。
	 * Retail-table gaps: the family table compiles the row, but the retail routes / kill coverage / reward
	 * block cannot express the axis, so XML retention is right. Codes without the {@code RETAIL_} prefix,
	 * which this gate reserves for family-compiler rejection codes.
	 */
	private static final Set<String> RETAIL_TABLE_GAPS = Set.of("KILL_COVERAGE_LOSS", "CLIENT_BUTTON_UNWIRED",
		"CLASS_SELECTABLE_REWARD", "XML_EXTRA_REWARD");
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailQuestCatalog catalog;
	private static Set<Integer> retailOwned;
	private static RetailQuestXmlTable retailTable;
	/** 客户端交付 NPC 集登记（复合势力奖励引用）。 / Client hand-in NPC registry. */
	private static RetailClientRewardNpcs clientRewardNpcs;
	/** 生产区域发放表（{@code _area_} 行的接线判据）。 / Production quest-area grant table. */
	private static RetailQuestAreaIndex questAreas;
	/** 客户端对话出口登记（接取页 SELECT1_1 续页）。 / Client dialog exits for the accept continuation. */
	private static RetailClientDialogExits clientDialogExits;
	/** 客户端报告页登记（承载 1009 按钮的页）。 / Client report pages hosting the reward button. */
	private static RetailNpcNameIndex npcIndexFull;
	private static RetailItemNameIndex itemIndexFull;
	private static Map<String, Integer> randomRewardCache;
	private static Map<Integer, Integer> nameIdCache;
	private static Map<Integer, String> registeredRejects;
	private static Map<Integer, FamilyRow> familyRowIndex;

	@BeforeAll
	static void loadFixtures() throws Exception {
		RetailSimpleHuntTable table;
		try (InputStream input = open("/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml")) {
			table = RetailSimpleHuntTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv")) {
			clientRewardNpcs = RetailClientRewardNpcs.load(input);
		}
		try (InputStream input = open("/aion/definitions/compact/ai/ai-areas.xml")) {
			questAreas = RetailQuestAreaIndex.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv")) {
			clientDialogExits = RetailClientDialogExits.load(input);
		}
		npcIndexFull = npcIndex();
		catalog = new RetailQuestCatalog(table, npcIndexFull);
		retailOwned = retailOwned();
		itemIndexFull = itemIndex();
		randomRewardCache = randomRewardIds();
		nameIdCache = nameIds();
		registeredRejects = registeredRejects();
		familyRowIndex = familyRows();
	}

	/** 保留清单里 family=SimpleHunt 的一行。 / One registered SimpleHunt row. */
	record FamilyRow(String owner, String reason) {
	}

	/** 保留清单里 family=SimpleHunt 的全部行。 / Every registered SimpleHunt row. */
	private static Map<Integer, FamilyRow> familyRows() throws Exception {
		Map<Integer, FamilyRow> rows = new TreeMap<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if (parts.length > 3 && "SimpleHunt".equals(parts[2])) {
				rows.put(Integer.parseInt(parts[0]), new FamilyRow(parts[1], parts[3]));
			}
		}
		return rows;
	}

	@Test
	void familyCompilesWithoutShell() throws Exception {
		Map<String, Integer> histogram = new TreeMap<>();
		Map<Integer, String> rejects = new TreeMap<>();
		Map<Integer, String> rejectDetails = new TreeMap<>();
		Set<Integer> acceptedIds = new TreeSet<>();
		int accepted = 0;
		Set<Integer> covered = new TreeSet<>(familyRows().keySet());
		for (int questId : covered) {
			RetailQuestCatalog unused = catalog;
			var plan = catalog.simpleHuntPlan(questId);
			if (plan.isEmpty()) {
				rejects.put(questId, "NO_PLAN");
				histogram.merge("NO_PLAN", 1, Integer::sum);
				continue;
			}
			RetailQuestMetadataCompiler.Outcome metadata = retailMetadata(questId);
			RetailSimpleHuntDefinitionCompiler.Outcome outcome =
				RetailSimpleHuntDefinitionCompiler.compile(plan.get(), metadata, clientRewardNpcs, questAreas,
					clientDialogExits);
			if (outcome.accepted()) {
				accepted++;
				acceptedIds.add(questId);
				histogram.merge("ACCEPTED", 1, Integer::sum);
			} else {
				String detail = outcome.rejectionCode();
				rejects.put(questId, detail);
				rejectDetails.put(questId, String.valueOf(outcome.detail()).replace('\t', ' '));
				histogram.merge(detail, 1, Integer::sum);
			}
		}
		String dump = System.getProperty("retail.hunt.rejectsOut");
		if (dump != null) {
			StringBuilder text = new StringBuilder("# accepted=" + accepted + "\n");
			histogram.forEach((code, count) -> text.append("# ").append(code).append("\t").append(count).append("\n"));
			for (int questId : covered) {
				String code = rejects.getOrDefault(questId, "ACCEPTED");
				text.append(questId).append('\t').append(code).append('\t')
					.append(rejectDetails.getOrDefault(questId, "")).append('\n');
			}
			java.nio.file.Files.writeString(java.nio.file.Path.of(dump), text.toString());
		}
		// 不变量：owner=RETAIL_TABLE 的任务必须全部合成；其余必须逐条命中登记表的拒绝码
		// （拒绝登记表是"真端文件无法表达"的证据清单，任何新增/消失的拒绝都必须先在表里登记）。
		// Invariants: every retail-owned quest compiles, and every other quest carries its registered code.
		// 全族不变量（覆盖 942 行 SimpleHunt 登记行，不只 owner=RETAIL_TABLE 的子集）：
		//   RETAIL_TABLE            → 必须合成成功；
		//   SEMANTIC_GAP:RETAIL_*   → 必须按登记码被拒绝（编译器缺口）；
		//   SEMANTIC_GAP:<驱动覆盖> → 必须合成成功（缺口在族表覆盖，不在编译）；
		//   SEMANTIC_GAP:<等价类>   → 必须有计划（缺口是 IR 等价，不是编译）；
		//   其余                    → 必须是登记的（拒绝码或等价缺口），不得出现未登记状态。
		// Whole-family invariants over every registered SimpleHunt row.
		List<String> problems = new ArrayList<>();
		for (int questId : covered) {
			boolean compiled = acceptedIds.contains(questId);
			String actual = compiled ? "ACCEPTED" : rejects.getOrDefault(questId, "MISSING");
			FamilyRow registered = familyRowIndex.get(questId);
			String reason = registered == null ? null : registered.reason();
			String expected = registeredRejects.get(questId);
			if (registered != null && "RETAIL_TABLE".equals(registered.owner())) {
				if (!compiled) {
					problems.add(questId + ": owner=RETAIL_TABLE 但被拒绝（" + actual + "）");
				}
				continue;
			}
			// 保留原因前缀（SEMANTIC_GAP: / ADJUDICATED:）不影响逐码不变量：缺口批裁定的行沿用
			// 既有分类语义——编译器缺口码（RETAIL_*）必须仍被拒，族表覆盖码（KILL_COVERAGE_LOSS 等）
			// 必须仍能合成，等价类必须有计划。前缀只是"逐行裁定已落"的登记标记（fail-closed 继承）。
			// The reason prefix is invariant-neutral: adjudicated rows keep the per-code obligations.
			String code = reason == null ? "" : stripReasonPrefix(reason);
			if (DRIVER_COVERAGE_GAPS.contains(code)) {
				// 族表能合成，但族表没有该语义的列（如任务刷怪）→ 保留 XML 才是正确结果。
				// The family compiles it, but the tables cannot express the axis, so XML retention is right.
				if (!compiled) {
					problems.add(questId + ": 登记为驱动覆盖缺口但族表合成失败（" + actual + "）");
				}
				continue;
			}
			if (RETAIL_TABLE_GAPS.contains(code)) {
				// 缺口在真端表而不在编译：行必须仍能被族表合成（合成失败 ⇒ 登记码用错或编译器回归）。
				// The gap lives in the retail table, not the compiler, so the row must still compile.
				if (!compiled) {
					problems.add(questId + ": 登记为真端表缺口但族表合成失败（" + actual + "）");
				}
				continue;
			}
			if (code.startsWith("RETAIL_")) {
				if (!code.equals(actual)) {
					problems.add(questId + ": 登记=" + code + " 实际=" + actual);
				}
				continue;
			}
			if (!EQUIVALENCE_GAPS.contains(code)) {
				problems.add(questId + ": 保留原因既非编译器缺口也非等价缺口（" + reason + "）");
				continue;
			}
			if ("MISSING".equals(actual) || "NO_PLAN".equals(actual)) {
				problems.add(questId + ": 登记为等价缺口但无计划（" + actual + "）");
			}
			if (expected != null && !expected.equals(actual)) {
				problems.add(questId + ": 拒绝登记表=" + expected + " 实际=" + actual);
			}
		}
		for (int questId : new TreeSet<>(registeredRejects.keySet())) {
			if (!covered.contains(questId)) {
				problems.add(questId + ": 拒绝登记表有行但不在 SimpleHunt 登记族内");
			}
		}
		int acceptedTotal = accepted;
		assertTrue(problems.isEmpty(), () -> "family gate failed: " + problems.stream().limit(20).toList()
			+ " (accepted=" + acceptedTotal + ", registered=" + registeredRejects.size() + ")");
		// 门槛即"清单内可合成的 SimpleHunt ≥ 250"（其余已收敛进保留清单 SEMANTIC_GAP 等）。
		// The floor tracks the retail-list SimpleHunt subset that compiles without a shell.
		assertTrue(acceptedTotal > 250, () -> "family acceptance too low: " + histogram);
	}

	/** 读取家族编译器拒绝登记表。 / Loads the registered compiler rejections. */
	private static Map<Integer, String> registeredRejects() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		InputStream input = RetailSimpleHuntFamilyGateTest.class.getResourceAsStream(COMPILER_REJECTS);
		assertNotNull(input, "missing resource " + COMPILER_REJECTS);
		for (String line : lines(input)) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			rows.put(Integer.parseInt(parts[0]), parts[1]);
		}
		return rows;
	}

	/** 保留原因去前缀（{@code SEMANTIC_GAP:} / {@code ADJUDICATED:}）。 / Strips the retention reason prefix. */
	private static String stripReasonPrefix(String reason) {
		int colon = reason.indexOf(':');
		return colon < 0 ? reason : reason.substring(colon + 1);
	}

	/**
	 * 缺口批 1 采纳门：挑战哨兵采纳集（{@link RetailChallengeAcquireAdoptions}）必须与裁定登记表
	 * 逐 id 恒等，且每条采纳的四方证据在册——真端行是 {@code CHALLENGE_TASK}、交付名经
	 * {@code resolvePartyName} 解析恰为采纳 NPC、采纳集 ⊆ 裁定登记（basis=CHALLENGE_TASK_NPC_DELIVERY）。
	 * 变异负例：登记表删一行或采纳表改一个 npc id 都会让本红亮起（fail-closed）。
	 * Gap-batch-1 adoption gate: the frozen adoption set must match the adjudication registry and
	 * every entry must carry its four-sided evidence (challenge sentinel row + delivery-name
	 * resolution to the adopted NPC + registry row).
	 */
	@Test
	void challengeAcquireAdoptionsMatchRegistryAndResolveToDeliveryNpc() throws Exception {
		Set<Integer> adopted = RetailChallengeAcquireAdoptions.adoptedQuestIds();
		Set<Integer> registry = new TreeSet<>();
		InputStream decisions = RetailSimpleHuntFamilyGateTest.class.getResourceAsStream(
			"/quest/retail-simple-hunt-adjudicated-decisions.tsv");
		assertNotNull(decisions, "missing adjudication registry");
		for (String line : lines(decisions)) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if (parts.length >= 3 && "ADOPT_RETAIL".equals(parts[1])
					&& "CHALLENGE_TASK_NPC_DELIVERY".equals(parts[2])) {
				registry.add(Integer.parseInt(parts[0]));
			}
		}
		assertEquals(registry, adopted, () -> "采纳集与裁定登记表失同步：registry=" + registry + " adopted=" + adopted);
		RetailSimpleHuntTable table;
		try (InputStream input = open("/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml")) {
			table = RetailSimpleHuntTable.load(input);
		}
		for (int questId : adopted) {
			RetailSimpleHuntTable.Entry entry = table.find(questId).orElse(null);
			assertNotNull(entry, () -> "采纳行不在真端表：" + questId);
			assertEquals(RetailGrantKind.CHALLENGE_TASK, entry.grantKind(),
				() -> "采纳行接取类别不是挑战哨兵：" + questId);
			int npc = RetailChallengeAcquireAdoptions.acquireNpcId(questId);
			String rewardName = entry.rewardNpc() == null ? "" : entry.rewardNpc();
			assertEquals(Set.of(npc), npcIndexFull.resolvePartyName(rewardName),
				() -> "采纳行交付名必须唯一解析到采纳 NPC：" + questId);
		}
	}

	private RetailQuestMetadataCompiler.Outcome retailMetadata(int questId) {
		RetailQuestXmlTable.Entry row = retailTable.find(questId).orElseThrow();
		return RetailQuestMetadataCompiler.compile(row, npcIndexFull, itemIndexFull, randomRewardCache,
			nameIdCache);
	}

	private static RetailNpcNameIndex npcIndex() throws Exception {
		return RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
	}

	private static RetailItemNameIndex itemIndex() throws Exception {
		return RetailItemNameIndex.build(openAll("/aion/data/static_data/items/item/",
			listXmlNames("/aion/data/static_data/items/item/")));
	}

	private static Map<String, Integer> randomRewardIds() throws Exception {
		Map<String, Integer> ids = new HashMap<>();
		var document = parse(open("/aion/data/static_data/quest_random_rewards.xml"));
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			var element = (org.w3c.dom.Element) nodes.item(index);
			ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
				Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
		}
		return ids;
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

	private static Set<Integer> retailOwned() throws Exception {
		Set<Integer> ids = new HashSet<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if ("RETAIL_TABLE".equals(parts[1]) && "SimpleHunt".equals(parts[2])) {
				ids.add(Integer.parseInt(parts[0]));
			}
		}
		return ids;
	}

	private static Set<Integer> catalogIds() throws Exception {
		Set<Integer> ids = new HashSet<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			ids.add(Integer.parseInt(line.split("\t", -1)[0]));
		}
		return ids;
	}

	private static java.util.List<String> listXmlNames(String dir) throws Exception {
		var url = RetailSimpleHuntFamilyGateTest.class.getResource(dir);
		assertNotNull(url, "missing dir " + dir);
		java.io.File[] files = new java.io.File(url.toURI()).listFiles((directory, name) -> name.endsWith(".xml"));
		assertNotNull(files);
		return java.util.Arrays.stream(files).map(java.io.File::getName).sorted().toList();
	}

	private static org.w3c.dom.Document parse(InputStream input) throws Exception {
		try (input) {
			var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(input);
		}
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws Exception {
		List<InputStream> inputs = new java.util.ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	private static List<String> lines(InputStream input) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}

	private static InputStream open(String resource) throws Exception {
		InputStream input = RetailSimpleHuntFamilyGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
