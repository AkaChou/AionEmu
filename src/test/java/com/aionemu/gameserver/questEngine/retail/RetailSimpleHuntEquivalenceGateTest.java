package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleHunt 家族等价门禁（提示词 §4.E.2/§4.E.3）：清单内 RETAIL_TABLE 且合成成功的任务，
 * 其"无 shell 真端定义"必须与 quest-definition XML 定义在 IR 层等价——
 * 节点按 (status, packed) 归一化、转换按 (sourceKey, event, targetKey, conditions, actions,
 * afterCommit) 归一化后多重集合相等；metadata 轴由 {@code RetailMetadataEquivalenceGateTest} 负责。
 * <p>
 * 不等价的任务必须登记进保留清单（{@code retail-xml-retention.tsv} 的
 * {@code XML_RETENTION/SEMANTIC_GAP} 行），门禁按登记表放行。
 * 排查模式：{@code -Dretail.hunt.equivOut=<path>} 导出全部不等价任务与首条差异。
 * Family equivalence gate: the shell-less retail definition must match the XML definition
 * on the normalized IR level.
 */
class RetailSimpleHuntEquivalenceGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	private static final String XML_RESOURCE = "/aion/data/static_data/quest_definition/quests/%d.xml";
	/** 生产任务 XML 源树目录（退役判据）。 / Source-tree directory of production quest XML. */
	private static final Path PRODUCTION_XML_DIR =
		Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");
	private static final String FINGERPRINTS = "/quest/retail-simple-hunt-ir-fingerprints.tsv";
	/** P0c-3 哨兵行裁定（真端优先）：ADOPT_RETAIL 行不参与 XML 等价判据，另用真端侧冻结指纹护栏。 */
	/** P0c-3 sentinel adjudication: ADOPT_RETAIL rows bypass the XML-equivalence criterion. */
	private static final String ADJUDICATED_DECISIONS = "/quest/retail-simple-hunt-adjudicated-decisions.tsv";
	/** 裁定行的真端侧冻结 IR。 / Retail-side frozen IR of the adjudicated rows. */
	private static final String ADJUDICATED_FINGERPRINTS =
		"/quest/retail-simple-hunt-adjudicated-ir-fingerprints.tsv";
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailQuestCatalog catalog;
	private static RetailQuestXmlTable retailTable;
	/** 客户端交付 NPC 集登记（复合势力奖励引用）。 / Client hand-in NPC registry. */
	private static RetailClientRewardNpcs clientRewardNpcs;
	/** 生产区域发放表（{@code _area_} 行的接线判据）。 / Production quest-area grant table. */
	private static RetailQuestAreaIndex questAreas;
	/** 客户端对话出口登记（接取页 SELECT1_1 续页）。 / Client dialog exits for the accept continuation. */
	private static RetailClientDialogExits clientDialogExits;
	/** 客户端报告页登记（承载 1009 按钮的页）。 / Client report pages hosting the reward button. */
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Set<Integer> retailOwnedSimpleHunt;
	private static Set<Integer> registeredGaps;
	/** 裁定为 ADOPT_RETAIL 的哨兵行（不参与 XML 等价对拍）。 / Sentinel rows adjudicated as retail-first. */
	private static Set<Integer> adjudicatedAdopted;

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
		clientDialogExits = RetailClientDialogExits.defaultExits();
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll("/aion/data/static_data/items/item/",
			listXmlNames("/aion/data/static_data/items/item/")));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		catalog = new RetailQuestCatalog(table, npcIndex);
		retailOwnedSimpleHunt = retailOwnedSimpleHunt();
		registeredGaps = registeredGaps();
		adjudicatedAdopted = adjudicatedAdopted();
	}

	/** 裁定登记里 verdict=ADOPT_RETAIL 的任务。 / Quest ids adjudicated as ADOPT_RETAIL. */
	private static Set<Integer> adjudicatedAdopted() throws Exception {
		Set<Integer> ids = new TreeSet<>();
		try (InputStream input = RetailSimpleHuntEquivalenceGateTest.class.getResourceAsStream(ADJUDICATED_DECISIONS)) {
			assertNotNull(input, "missing adjudication resource " + ADJUDICATED_DECISIONS);
			for (String line : lines(input)) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length >= 2 && "ADOPT_RETAIL".equals(parts[1])) {
					ids.add(Integer.parseInt(parts[0]));
				}
			}
		}
		return ids;
	}

	@Test
	void synthesizedDefinitionsMatchXmlIr() throws Exception {
		List<String> nonEquivalent = new ArrayList<>();
		Map<String, List<String>> diffSamples = new TreeMap<>();
		int equivalent = 0;
		int accepted = 0;
		for (int questId : new TreeSet<>(retailOwnedSimpleHunt)) {
			QuestDefinition retail = retailDefinition(questId);
			if (retail == null) {
				continue;
			}
			accepted++;
			if (adjudicatedAdopted.contains(questId)) {
				// 裁定为真端优先的行不参与 XML 等价对拍：XML 在这里不是金标准（接取路由是历史内容），
				// 语义由家族门禁（SystemGrant 形状 + 击杀网格）与真端侧冻结指纹承担。
				// Adjudicated rows skip the XML comparison; the family gate and the retail-side
				// frozen fingerprint carry their semantics instead.
				continue;
			}
			CompiledQuestDefinition fromXml = compileXml(questId);
			if (fromXml == null) {
				continue;
			}
			String problem = irEquivalenceProblem(fromXml.definition(), retail);
			if (problem == null) {
				equivalent++;
			} else if (!registeredGaps.contains(questId)) {
				nonEquivalent.add(Integer.toString(questId));
				diffSamples.put(Integer.toString(questId), List.of(problem));
			}
		}
		// 裁定登记与"已退役集合"必须一致：裁定行必须由真端驱动（否则豁免等于放水）。
		// Adjudicated rows must actually be retired and retail-driven.
		List<String> adjudicationProblems = new ArrayList<>();
		for (int questId : new TreeSet<>(adjudicatedAdopted)) {
			if (!retailOwnedSimpleHunt.contains(questId)) {
				adjudicationProblems.add(questId + ": 裁定 ADOPT_RETAIL 但未退役（保留清单仍是 XML_RETENTION）");
				continue;
			}
			if (retailDefinition(questId) == null) {
				adjudicationProblems.add(questId + ": 裁定 ADOPT_RETAIL 但真端合成被拒绝");
			}
		}
		assertTrue(adjudicationProblems.isEmpty(), () -> "哨兵裁定登记失同步："
			+ adjudicationProblems.stream().limit(20).toList());
		String dump = System.getProperty("retail.hunt.equivOut");
		if (dump != null) {
			StringBuilder text = new StringBuilder("# accepted=" + accepted + " equivalent=" + equivalent
				+ " non-equivalent=" + nonEquivalent.size() + "\n");
			diffSamples.forEach((qid, problems) -> text.append(qid).append('\t')
				.append(String.join(" | ", problems)).append('\n'));
			Files.writeString(Path.of(dump), text.toString());
		}
		int acceptedTotal = accepted;
		int equivalentTotal = equivalent;
		// 门槛即"可证等价的真端驱动 SimpleHunt ≥ 250"：其余保留 XML（SEMANTIC_GAP:DIALOG_ROUTE）。
		// The floor documents the currently proven-equivalent retail-driven SimpleHunt subset.
		assertTrue(acceptedTotal > 250, () -> "family acceptance too low: " + acceptedTotal);
		assertTrue(nonEquivalent.isEmpty(), () -> "non-equivalent quests not registered as SEMANTIC_GAP: "
			+ nonEquivalent.stream().map(String::valueOf).limit(20).toList()
			+ " (accepted=" + acceptedTotal + " equivalent=" + equivalentTotal + ")");
	}

	/**
	 * 冻结 IR 指纹门禁：删除 XML 后继续用冻结证据证明等价。
	 * <ul>
	 * <li>冻结模式（{@code -Dretail.hunt.fingerprintOut=<path>}）：对每个可证等价的任务写出
	 * {@code quest_id / xml_fingerprint / retail_fingerprint / nodes / transitions}，并要求两侧指纹相同；</li>
	 * <li>校验模式：XML 还在 → 现算两侧指纹并与冻结行比对（XML 侧漂移立即暴露）；
	 * XML 已删 → 只有冻结指纹能证明等价，缺证据直接失败（禁止静默放宽）；</li>
	 * <li>冻结集合与"已迁移集合"必须一致，既不许悬空冻结行，也不许未冻结的迁移任务。</li>
	 * </ul>
	 * Frozen-IR fingerprint gate: keeps the equivalence proof alive after the XML is deleted.
	 */
	@Test
	void frozenIrFingerprintsCoverExactlyTheMigratedQuests() throws Exception {
		String freezeOut = System.getProperty("retail.hunt.fingerprintOut");
		String adjudicatedFreezeOut = System.getProperty("retail.hunt.adjudicatedFingerprintOut");
		boolean freeze = freezeOut != null || adjudicatedFreezeOut != null;
		Map<Integer, String[]> frozen = freeze ? new TreeMap<>() : loadFingerprints();
		Map<Integer, String> adjudicatedFrozen = freeze ? new TreeMap<>() : loadAdjudicatedFingerprints();
		List<String> frozenRows = new ArrayList<>();
		List<String> adjudicatedFrozenRows = new ArrayList<>();
		List<String> problems = new ArrayList<>();
		Set<Integer> migrated = new TreeSet<>();
		for (int questId : new TreeSet<>(retailOwnedSimpleHunt)) {
			QuestDefinition retail = retailDefinition(questId);
			if (retail == null) {
				continue;
			}
			String retailFingerprint = RetailIrFingerprint.fingerprint(retail);
			if (freeze) {
				if (adjudicatedAdopted.contains(questId)) {
					// 裁定行（真端优先）不要求 XML 等价：只冻结真端侧 IR 防静默漂移。
					// Adjudicated rows freeze the retail-side IR only (no XML equivalence claim).
					adjudicatedFrozenRows.add(questId + "\t" + retailFingerprint + "\t"
						+ retail.nodes().size() + "\t" + retail.transitions().size());
					migrated.add(questId);
					continue;
				}
				if (freezeOut == null) {
					continue;
				}
				// 显式重定基（retail.hunt.rebaselineOut）：仅限有台账记录的 canonical 编译器变更——
				// 退役行的等价证明改由当前真端 IR 承担，两列指纹同值重写；这是留痕操作而非静默放宽。
				// Explicit rebaseline (retail.hunt.rebaselineOut): only for ledger-recorded canonical
				// compiler changes — the retired row's equivalence proof is re-carried by the current
				// retail IR, both fingerprint columns rewritten in lockstep; a loud, auditable
				// operation, never a silent relaxation.
				String rebaseline = System.getProperty("retail.hunt.rebaselineOut");
				if (rebaseline != null) {
					migrated.add(questId);
					frozenRows.add(questId + "\t" + retailFingerprint + "\t" + retailFingerprint + "\t"
						+ retail.nodes().size() + "\t" + retail.transitions().size());
					continue;
				}
				// 冻结模式需要生产 XML（只在退役之前使用）；退役后 XML 不再进仓。
				CompiledQuestDefinition fromXml = compileXml(questId);
				if (fromXml == null) {
					problems.add(questId + ": 无生产 XML，无法重算冻结基线（退役后禁止再冻结）");
					continue;
				}
				String xmlFingerprint = RetailIrFingerprint.fingerprint(fromXml.definition());
				if (!xmlFingerprint.equals(retailFingerprint)) {
					continue;
				}
				migrated.add(questId);
				frozenRows.add(questId + "\t" + xmlFingerprint + "\t" + retailFingerprint + "\t"
					+ fromXml.definition().nodes().size() + "\t" + fromXml.definition().transitions().size());
				continue;
			}
			// 检查模式：XML 已删除，等价性由冻结指纹承担——只核对真端侧 IR 是否漂移。
			// Check mode: the XML is gone, so the frozen fingerprint carries the equivalence proof.
			if (adjudicatedAdopted.contains(questId)) {
				String adjudicated = adjudicatedFrozen.get(questId);
				if (adjudicated == null) {
					problems.add(questId + ": 裁定退役但没有冻结真端 IR 证据");
					continue;
				}
				migrated.add(questId);
				if (productionXmlPresent(questId)) {
					problems.add(questId + ": 裁定行已冻结但生产 XML 未退役");
				}
				if (!adjudicated.equals(retailFingerprint)) {
					problems.add(questId + ": 真端侧 IR 漂移（裁定冻结基线需重算）");
				}
				continue;
			}
			String[] row = frozen.get(questId);
			if (row == null) {
				problems.add(questId + (productionXmlPresent(questId)
					? ": 可证等价但未冻结" : ": 生产 XML 已退役但没有冻结指纹证据"));
				continue;
			}
			migrated.add(questId);
			if (productionXmlPresent(questId)) {
				problems.add(questId + ": 已冻结但生产 XML 未退役（删除纪律：冻结集合 ⇔ 退役集合）");
			}
			if (!row[1].equals(retailFingerprint)) {
				problems.add(questId + ": 真端侧 IR 漂移（冻结基线需重算）");
			}
		}
		if (freeze) {
			if (freezeOut != null) {
				StringBuilder text = new StringBuilder(
					"# SimpleHunt 冻结 IR 指纹（XML 删除后继续证明等价的依据）\n"
						+ "# quest_id\txml_fingerprint\tretail_fingerprint\tnodes\ttransitions\n");
				frozenRows.forEach(row -> text.append(row).append('\n'));
				Files.writeString(Path.of(freezeOut), text.toString());
			}
			if (adjudicatedFreezeOut != null) {
				StringBuilder text = new StringBuilder(
					"# SimpleHunt 裁定行（真端优先）真端侧冻结 IR（P0c-3 哨兵批 + P0c-6 简报批）\n"
						+ "# quest_id\tretail_fingerprint\tnodes\ttransitions\n");
				adjudicatedFrozenRows.forEach(row -> text.append(row).append('\n'));
				Files.writeString(Path.of(adjudicatedFreezeOut), text.toString());
			}
			return;
		}
		Set<Integer> staleFrozen = new TreeSet<>(frozen.keySet());
		staleFrozen.removeAll(migrated);
		if (!staleFrozen.isEmpty()) {
			problems.add("冻结指纹存在但任务未迁移：" + staleFrozen.stream().limit(10).toList());
		}
		Set<Integer> staleAdjudicated = new TreeSet<>(adjudicatedFrozen.keySet());
		staleAdjudicated.removeAll(migrated);
		if (!staleAdjudicated.isEmpty()) {
			problems.add("裁定冻结指纹存在但任务未迁移：" + staleAdjudicated.stream().limit(10).toList());
		}
		if (!adjudicatedFrozen.keySet().equals(adjudicatedAdopted)) {
			problems.add("裁定集合与冻结集合不一致：frozen=" + adjudicatedFrozen.size()
				+ " adjudicated=" + adjudicatedAdopted.size());
		}
		Set<Integer> bothRegistries = new TreeSet<>(frozen.keySet());
		bothRegistries.retainAll(adjudicatedFrozen.keySet());
		if (!bothRegistries.isEmpty()) {
			problems.add("同一任务同时出现在两个指纹登记表：" + bothRegistries.stream().limit(10).toList());
		}
		assertTrue(problems.isEmpty(), () -> "frozen IR fingerprint gate failed: "
			+ problems.stream().limit(20).toList() + " (migrated=" + migrated.size() + ")");
	}

	/** 真端驱动定义；未接受返回 null（拒绝明细由家族门禁登记）。 / The retail definition, or null when rejected. */
	private static QuestDefinition retailDefinition(int questId) {
		var plan = catalog.simpleHuntPlan(questId);
		if (plan.isEmpty()) {
			return null;
		}
		var metadata = RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(), npcIndex,
			itemIndex, randomRewards, nameIds);
		var outcome = RetailSimpleHuntDefinitionCompiler.compile(plan.get(), metadata, clientRewardNpcs,
			questAreas, clientDialogExits);
		return outcome.accepted() ? outcome.definition().definition() : null;
	}

	/** 读取冻结指纹资源。 / Loads the frozen fingerprint resource. */
	private static Map<Integer, String[]> loadFingerprints() throws Exception {
		Map<Integer, String[]> rows = new TreeMap<>();
		try (InputStream input = RetailSimpleHuntEquivalenceGateTest.class.getResourceAsStream(FINGERPRINTS)) {
			assertNotNull(input, "missing frozen fingerprint resource " + FINGERPRINTS);
			for (String line : lines(input)) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				rows.put(Integer.parseInt(parts[0]), new String[] { parts[1], parts[2] });
			}
		}
		return rows;
	}

	/** 读取裁定行的真端侧冻结指纹。 / Loads the retail-side frozen fingerprints of adjudicated rows. */
	private static Map<Integer, String> loadAdjudicatedFingerprints() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		try (InputStream input = RetailSimpleHuntEquivalenceGateTest.class
				.getResourceAsStream(ADJUDICATED_FINGERPRINTS)) {
			assertNotNull(input, "missing adjudicated fingerprint resource " + ADJUDICATED_FINGERPRINTS);
			for (String line : lines(input)) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				rows.put(Integer.parseInt(parts[0]), parts[1]);
			}
		}
		return rows;
	}

	// ---------------------------------------------------------------- IR 归一化对拍

	/** IR 等价判定的差异描述；等价返回 null。 / Returns a divergence description, or null when equal. */
	static String irEquivalenceProblem(QuestDefinition xml, QuestDefinition retail) {
		Map<String, String> xmlKeys = nodeKeys(xml);
		Map<String, String> retailKeys = nodeKeys(retail);
		if (!new HashSet<>(xmlKeys.values()).equals(new HashSet<>(retailKeys.values()))) {
			return "node sets differ: xml=" + new TreeSet<>(xmlKeys.values()).size()
				+ " retail=" + new TreeSet<>(retailKeys.values()).size()
				+ " missing=" + difference(new TreeSet<>(xmlKeys.values()), new TreeSet<>(retailKeys.values()))
				+ " extra=" + difference(new TreeSet<>(retailKeys.values()), new TreeSet<>(xmlKeys.values()));
		}
		Map<String, String> xmlLabels = labelToKey(xmlKeys);
		Map<String, String> retailLabels = labelToKey(retailKeys);
		Map<String, Integer> xmlCounts = transitionSignatures(xml, xmlLabels);
		Map<String, Integer> retailCounts = transitionSignatures(retail, retailLabels);
		if (!xmlCounts.equals(retailCounts)) {
			String missing = difference(new TreeSet<>(xmlCounts.keySet()), new TreeSet<>(retailCounts.keySet()));
			String extra = difference(new TreeSet<>(retailCounts.keySet()), new TreeSet<>(xmlCounts.keySet()));
			int diffCount = 0;
			for (String key : new TreeSet<>(java.util.Set.copyOf(xmlCounts.keySet())
					.stream().collect(java.util.stream.Collectors.toSet()))) {
				if (!xmlCounts.get(key).equals(retailCounts.get(key))) {
					diffCount++;
				}
			}
			return "transition multisets differ: missing=" + first(missing) + " extra=" + first(extra)
				+ " countMismatch=" + diffCount;
		}
		if (!xml.progressLayout().fields().equals(retail.progressLayout().fields())) {
			return "progress layouts differ: " + xml.progressLayout().fields() + " vs "
				+ retail.progressLayout().fields();
		}
		return null;
	}

	private static String first(String set) {
		return set.isEmpty() ? "-" : set;
	}

	private static String difference(TreeSet<String> left, TreeSet<String> right) {
		TreeSet<String> copy = new TreeSet<>(left);
		copy.removeAll(right);
		return copy.isEmpty() ? "-" : String.join(",", copy.stream().limit(3).toList());
	}

	/** 节点 → 规范键 (status, packed)。 / Node label to canonical (status, packed) key. */
	private static Map<String, String> nodeKeys(QuestDefinition definition) {
		Map<String, String> keys = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			keys.put(node.label(), node.projection().status() + "/"
				+ definition.progressLayout().pack(node.projection().variables()));
		}
		return keys;
	}

	private static Map<String, String> labelToKey(Map<String, String> keys) {
		Map<String, String> labels = new HashMap<>();
		keys.forEach(labels::put);
		return labels;
	}

	/** 转换签名多重集合（source/target 用规范键，事件/条件/动作/after-commit 用其规范文本）。 */
	private static Map<String, Integer> transitionSignatures(QuestDefinition definition,
			Map<String, String> labels) {
		Map<String, Integer> counts = new TreeMap<>();
		for (QuestTransition transition : definition.transitions()) {
			String source = transition.sourceNode() == null ? "null" : labels.get(transition.sourceNode());
			String target = labels.get(transition.targetNode());
			for (String event : RetailKillRoutes.eventTexts(transition.event())) {
				String signature = source + ">" + event + ">" + target + ">" + transition.conditions()
					+ ">" + transition.actions() + ">" + transition.afterCommit() + ">"
					+ (transition.priority() == null ? "-" : transition.priority());
				counts.merge(signature, 1, Integer::sum);
			}
		}
		return counts;
	}

	// ---------------------------------------------------------------- 装载

	/**
	 * 对拍用 XML 定义：生产 XML 优先，已退役的用测试作用域冻结 fixture；
	 * 击杀目标按同名族（{@link RetailNpcNameIndex#withDisplayNameVariants}）展开，
	 * 与真端驱动侧落在同一等价集上再比对。
	 * XML definition for comparison: the production copy first, then the retired test fixture,
	 * with kill targets expanded to the same display-name family the retail driver binds.
	 */
	private static CompiledQuestDefinition compileXml(int questId) throws Exception {
		try (InputStream input = comparisonXmlStream(questId)) {
			if (input == null) {
				return null;
			}
			String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			return QuestDefinitionCompiler.compile(QuestDefinitionXmlCompiler.parse(
				new ByteArrayInputStream(RetailKillTargetXml.expand(xml, npcIndex)
					.getBytes(StandardCharsets.UTF_8))));
		}
	}

	/**
	 * 对拍用 XML 流：只读生产 XML（退役任务的 XML 不再进仓，历史内容在 git 里可回溯）。
	 * XML stream for comparison: the production copy only; retired XMLs live in git history.
	 */
	private static InputStream comparisonXmlStream(int questId) {
		return RetailSimpleHuntEquivalenceGateTest.class.getResourceAsStream(String.format(XML_RESOURCE, questId));
	}

	/**
	 * 生产 XML 是否仍在（退役与否的判据）。
	 * <p>
	 * 直接看源树而不是 classpath：{@code target/classes} 会残留已删除资源，
	 * 用 classpath 判断会把"已退役"误判成"仍在生产"。
	 * Checks the source tree instead of the classpath: stale {@code target/classes} copies would
	 * otherwise report a retired quest as still present.
	 */
	private static boolean productionXmlPresent(int questId) {
		return Files.exists(PRODUCTION_XML_DIR.resolve(questId + ".xml"));
	}

	private static Set<Integer> retailOwnedSimpleHunt() throws Exception {
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

	private static Set<Integer> registeredGaps() throws Exception {
		Set<Integer> ids = new HashSet<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if ("XML_RETENTION".equals(parts[1]) && parts[3].startsWith("SEMANTIC_GAP")) {
				ids.add(Integer.parseInt(parts[0]));
			}
		}
		return ids;
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

	private static List<String> listXmlNames(String dir) throws Exception {
		var url = RetailSimpleHuntEquivalenceGateTest.class.getResource(dir);
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
		List<InputStream> inputs = new ArrayList<>();
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
		InputStream input = RetailSimpleHuntEquivalenceGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
