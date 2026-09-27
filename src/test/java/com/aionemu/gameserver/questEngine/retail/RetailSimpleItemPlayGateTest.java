package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleItemPlay 全族门禁（P1 wave-2 退役后口径）：真端表 43 行中与本服生产相交的 15 行
 * 全部经 {@link RetailSimpleItemPlayDefinitionCompiler} 编译。
 * <p>
 * wave-1 六行（接取发物 → 用物品演出 → 交付）已按"与退役前生产 XML IR 逐字等价"的实测证据退役
 * （XML 删除，内容在 git 历史）；本门禁改用**真端侧冻结指纹护栏**（P0c-3 裁定行先例）：
 * 合成 IR 指纹必须与冻结值逐一相等，任何 IR 变化都强制显式重冻结。
 * 其余 9 行按稳定拒绝码保留 XML，且 retention owner 行必须与登记码一致；
 * 28 行真端独有开发/测试任务（无生产对应）不参与门禁。
 * 冻结模式：{@code -Dretail.itemPlay.fingerprintOut=<path>} 重写冻结表（须人工核对差异后入仓）；
 * 排查模式：{@code -Dretail.itemPlay.equivOut=<path>} 导出逐行结果。
 * Family gate for SimpleItemPlay (post-retirement): the six retired rows are guarded by frozen
 * retail-side IR fingerprints; the nine KEEP rows must keep their registered rejection codes and
 * retention rows in lockstep.
 */
class RetailSimpleItemPlayGateTest {

	/** wave-1 采纳面（已退役，owner=RETAIL_TABLE）。 / Wave-one adoption set (retired, RETAIL_TABLE). */
	private static final Set<Integer> WAVE_ONE = Set.of(13704, 13708, 19048, 23704, 23708, 29048);
	/** 稳定拒绝码白名单（码 → 预期任务；新增/消失必须先改本测试与裁定表）。 */
	private static final Map<String, Set<Integer>> REGISTERED_REJECTS = Map.of(
		"RETAIL_ACQUIRE_NPC_UNRESOLVED", Set.of(18828, 28828, 50048),
		"RETAIL_ACQUIRE_NPC_SENTINEL", Set.of(39713, 49713),
		"RETAIL_TALK_CHAIN", Set.of(18213, 28213),
		"RETAIL_ADVANCE_UNEXPRESSED", Set.of(80255, 80256));
	/**
	 * 缺口批 1 逐行裁定的码：接取/交付 NPC 在本服数据缺失的 5 行转 {@code ADJUDICATED:<码>} 保留
	 * （裁定=本服无该 NPC，无法合成接取/交付边；家族门 fail-closed 复核仍以同码被拒）。
	 * Codes adjudicated in gap batch 1: their rows keep XML with the {@code ADJUDICATED:} prefix.
	 */
	private static final Set<String> ADJUDICATED_CODES =
		Set.of("RETAIL_ACQUIRE_NPC_UNRESOLVED", "RETAIL_ACQUIRE_NPC_SENTINEL");
	/** wave-1 六行的真端侧冻结 IR 指纹（退役证明，任何重算须显式入仓）。 */
	private static final String FINGERPRINTS = "/quest/retail-simple-item-play-ir-fingerprints.tsv";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	/** 生产任务 XML 源树目录（退役/在盘判据，不信 target/classes 残留）。 */
	private static final Path PRODUCTION_XML_DIR =
		Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");
	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailSimpleItemPlayTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Map<Integer, String> frozenFingerprints;
	/** retention 中 SimpleItemPlay 的 owner 分区与 reason。 / Retention partition for the family. */
	private static Map<Integer, String> retentionOwners;
	private static Map<Integer, String> retentionReasons;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest_retail/Quest_SimpleItemPlay.xml")) {
			table = RetailSimpleItemPlayTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll("/aion/data/static_data/items/item/",
			listXmlNames("/aion/data/static_data/items/item/")));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		// 冻结表允许暂缺（首跑冻结模式生成）；非冻结模式下缺表会让不变量 1 以"缺退役证明"失败。
		try (InputStream input = RetailSimpleItemPlayGateTest.class.getResourceAsStream(FINGERPRINTS)) {
			frozenFingerprints = input == null ? new HashMap<>() : loadTwoColumn(input);
		}
		retentionOwners = new HashMap<>();
		retentionReasons = new HashMap<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if ("SimpleItemPlay".equals(parts[2])) {
				retentionOwners.put(Integer.parseInt(parts[0]), parts[1]);
				retentionReasons.put(Integer.parseInt(parts[0]), parts[3]);
			}
		}
	}

	@Test
	void familyMatchesFrozenFingerprintsAndRegisteredRejections() throws Exception {
		Map<Integer, String> fingerprints = new TreeMap<>();
		Map<Integer, String> rejectCodes = new TreeMap<>();
		List<String> problems = new ArrayList<>();
		String freezeOut = System.getProperty("retail.itemPlay.fingerprintOut");
		for (int questId : new TreeSet<>(table.questIds())) {
			var plan = table.find(questId);
			assertTrue(plan.isPresent(), () -> "row " + questId + " present");
			if (!retentionOwners.containsKey(questId)) {
				continue;
			}
			var meta = retailTable.find(questId);
			if (meta.isEmpty()) {
				problems.add(questId + ": no retail metadata row");
				continue;
			}
			var outcome = RetailSimpleItemPlayDefinitionCompiler.compile(plan.get(), itemIndex, npcIndex,
				RetailQuestMetadataCompiler.compile(meta.get(), npcIndex, itemIndex, randomRewards, nameIds));
			if (!outcome.accepted()) {
				rejectCodes.put(questId, outcome.rejectionCode());
				continue;
			}
			fingerprints.put(questId, RetailIrFingerprint.fingerprint(outcome.definition().definition()));
		}
		String dump = System.getProperty("retail.itemPlay.equivOut");
		if (dump != null) {
			StringBuilder text = new StringBuilder("# SimpleItemPlay 族门禁逐行结果\n"
				+ "# quest_id\tresult\tfingerprint\n");
			for (int questId : new TreeSet<>(table.questIds())) {
				String result = fingerprints.containsKey(questId) ? "FROZEN"
					: rejectCodes.getOrDefault(questId,
						retentionOwners.containsKey(questId) ? "PROBLEM" : "NO_PRODUCTION");
				text.append(questId).append('\t').append(result).append('\t')
					.append(fingerprints.getOrDefault(questId, "-")).append('\n');
			}
			Files.writeString(Path.of(dump), text.toString());
		}
		if (freezeOut != null) {
			StringBuilder text = new StringBuilder("# SimpleItemPlay wave-1 六行真端侧冻结 IR 指纹\n"
				+ "# 由 -Dretail.itemPlay.fingerprintOut 重算生成；任何变化必须人工核对后入仓\n"
				+ "# quest_id\tir_fingerprint\n");
			for (int questId : new TreeSet<>(fingerprints.keySet())) {
				text.append(questId).append('\t').append(fingerprints.get(questId)).append('\n');
			}
			Files.writeString(Path.of(freezeOut), text.toString());
		}
		// 不变量 0：真端表形状快照 + retention 族分区精确（6 RETAIL_TABLE + 9 XML_RETENTION，无第三态）。
		assertEquals(43, table.size(), () -> "retail table shape snapshot");
		assertEquals(WAVE_ONE, retentionOwners.entrySet().stream()
				.filter(entry -> "RETAIL_TABLE".equals(entry.getValue()))
				.map(Map.Entry::getKey).collect(Collectors.toSet()),
			() -> "RETAIL_TABLE partition must be exactly the wave-one set");
		assertEquals(REGISTERED_REJECTS.values().stream().flatMap(Set::stream).collect(Collectors.toSet()),
			retentionOwners.entrySet().stream()
				.filter(entry -> "XML_RETENTION".equals(entry.getValue()))
				.map(Map.Entry::getKey).collect(Collectors.toSet()),
			() -> "XML_RETENTION partition must be exactly the registered KEEP set");
		// 不变量 1：wave-1 六行合成 IR 指纹与冻结值逐一相等（退役证明；变化必须显式重冻结）。
		if (freezeOut == null) {
			assertEquals(frozenFingerprints, fingerprints,
				() -> "retail IR fingerprints must match the frozen proof; problems=" + problems);
		}
		// 不变量 2：9 行 KEEP 逐一命中登记稳定码，且 retention reason 与登记码一致，XML 仍在盘。
		// 缺口批 1 裁定的 5 行用 ADJUDICATED: 前缀，其余 KEEP 行保持 SEMANTIC_GAP: 前缀。
		assertEquals(REGISTERED_REJECTS, rejectCodes.entrySet().stream()
			.collect(Collectors.groupingBy(Map.Entry::getValue,
				Collectors.mapping(Map.Entry::getKey, Collectors.toSet()))),
			() -> "rejection codes must match the registered whitelist");
		for (Map.Entry<String, Set<Integer>> registered : REGISTERED_REJECTS.entrySet()) {
			String prefix = ADJUDICATED_CODES.contains(registered.getKey()) ? "ADJUDICATED:" : "SEMANTIC_GAP:";
			for (int questId : registered.getValue()) {
				assertEquals(prefix + registered.getKey(), retentionReasons.get(questId),
					() -> "retention reason for " + questId);
				assertTrue(sourceTreeXmlPresent(questId), () -> "KEEP xml must stay on disk: " + questId);
			}
		}
		// 不变量 3：wave-1 六行退役态保持（生产 XML 已删；磁盘回插即失败）。
		for (int questId : WAVE_ONE) {
			assertFalse(sourceTreeXmlPresent(questId),
				() -> "retired xml must stay deleted (content lives in git history): " + questId);
			assertNotNull(retailTable.find(questId).orElse(null),
				() -> "retail metadata row stays present for " + questId);
		}
	}

	/** 生产归属看源树而非 classpath（target/classes 会残留已删资源）。 */
	private static boolean sourceTreeXmlPresent(int questId) {
		return Files.exists(PRODUCTION_XML_DIR.resolve(questId + ".xml"));
	}

	private static Map<Integer, String> loadTwoColumn(InputStream input) throws Exception {
		Map<Integer, String> out = new HashMap<>();
		for (String line : lines(input)) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			out.put(Integer.parseInt(parts[0]), parts[1]);
		}
		return out;
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
		var url = RetailSimpleItemPlayGateTest.class.getResource(dir);
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
		InputStream input = RetailSimpleItemPlayGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
