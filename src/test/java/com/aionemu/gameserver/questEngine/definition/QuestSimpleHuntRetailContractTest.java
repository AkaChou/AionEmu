package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aionemu.gameserver.questEngine.retail.RetailNpcNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestAiNameGroupsFixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleHunt 击杀计数真端合同门禁。
 * <p>
 * 权威来源：Aion 5.8 真端服务端模板表 {@code Map/XML/Quest_SimpleHunt.xml} 的 {@code countN/monsterN}
 * （经 NPC {@code name_desc} 解析为 npc_id），快照由
 * {@code .agents/summary/scriptdll-quest-driver/reconcile_simple_hunt.py --emit-contract} 生成。
 * <p>
 * 每个计数器按快照里的 {@code model} 断言：
 * <ul>
 * <li>{@code COUNTER_GRID}：必须存在宽 6 的 bit-field（offset = 6*(n-1)），其 dimension 的
 * {@code required} 等于真端 {@code countN}，npc-ids 必须覆盖真端怪集合，且不得超出
 * 「真端怪集合 ∪ 客户端 quest_monster.csv 怪集合」（表为服务端口径，CSV 为客户端口径）；</li>
 * <li>{@code KILL_CHAIN}：串行链的击杀节点数必须等于真端 {@code countN}；</li>
 * <li>其余模型（{@code WIDE_FIELD}/{@code FIELD_NO_KILL_MODEL}/{@code NO_FIELD}/
 * {@code UNRESOLVED_NAME}/{@code MISMATCH}）：锁定「该计数器当前未按计数器建模」，
 * 一旦有人改成 counter-grid，本门禁要求同步更新快照，禁止静默漂移。</li>
 * </ul>
 * Retail-template contract gate for SimpleHunt kill counters.
 */
class QuestSimpleHuntRetailContractTest {

	private static final String CONTRACT_RESOURCE = "/quest/quest-simple-hunt-retail-contract.tsv";
	/** 服务端可达目标例外台账（真端口径之外、本服运行期必须接受的击杀目标）。
	 * Server-side reachable target exceptions: kill targets the runtime must keep accepting. */
	private static final String SERVER_TARGET_EXCEPTIONS = "/quest/quest-simple-hunt-server-target-exceptions.tsv";
	/** NPC 模板文件清单（与真端名索引门禁一致）。 / NPC template files matching the other retail gates. */
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailNpcNameIndex npcIndex;
	private static Map<Integer, Map<String, Set<Integer>>> serverTargetExceptions;

	@BeforeAll
	static void loadFixtures() throws IOException {
		List<InputStream> templates = new ArrayList<>();
		try {
			for (String file : NPC_TEMPLATES) {
				String resource = "/aion/data/static_data/npcs/" + file;
				InputStream input = QuestSimpleHuntRetailContractTest.class.getResourceAsStream(resource);
				assertNotNull(input, "missing npc template " + resource);
				templates.add(input);
			}
			npcIndex = RetailNpcNameIndex.build(templates, RetailQuestAiNameGroupsFixture.streams());
			serverTargetExceptions = loadServerTargetExceptions();
		} finally {
			for (InputStream input : templates) {
				input.close();
			}
		}
	}

	@Test
	void everyPortedSimpleHuntCounterMatchesTheRetailTemplate() throws Exception {
		Map<Integer, List<Row>> contract = loadContract();
		int totalRows = contract.values().stream().mapToInt(List::size).sum();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		Set<Integer> catalogIds = new LinkedHashSet<>();
		catalog.entries().forEach(entry -> catalogIds.add(entry.id()));
		Set<Integer> retiredIds = retiredFixtureIds();

		List<String> violations = new ArrayList<>();
		int checked = 0;
		int skippedUnported = 0;
		for (Map.Entry<Integer, List<Row>> entry : contract.entrySet()) {
			if (!catalogIds.contains(entry.getKey()) && !retiredIds.contains(entry.getKey())) {
				skippedUnported++;
				continue;
			}
			// 已退役任务的 XML 只在 git 历史里：同口径不变量改在生产视图（真端合成定义）上核对。
			// Retired quests have no XML any more; the same invariants are checked on the
			// synthesized production definition instead.
			boolean retired = retiredIds.contains(entry.getKey());
			Document document = retired ? null : loadQuestDocument(entry.getKey());
			for (Row row : entry.getValue()) {
				checked++;
				String problem = retired ? checkRetiredRow(entry.getKey(), row) : checkRow(document, row);
				if (problem != null) {
					violations.add(problem);
				}
			}
		}

		int checkedFinal = checked;
		int unportedFinal = skippedUnported;
		assertEquals(totalRows, checkedFinal,
			() -> "every snapshotted SimpleHunt quest must be owned by the production catalog, unported="
				+ unportedFinal);
		assertTrue(violations.isEmpty(),
			() -> "SimpleHunt retail counter contract violations (" + violations.size() + "):\n"
				+ String.join("\n", violations.subList(0, Math.min(violations.size(), 20))));
	}

	/**
	 * 退役任务的检查路径：定义取生产视图（真端合成），断言与 XML 侧同口径的不变量——
	 * 槽位字段几何（6 位、offset=6*(n-1)）、可计数上限 = 真端 {@code countN}、覆盖真端全部怪。
	 * Retired quests carry no XML, so the counter contract is asserted on the synthesized definition.
	 */
	private static String checkRetiredRow(int questId, Row row) {
		if (com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler.instance().owns(questId)) {
			// P1 原生表驱动任务由 SimpleHuntNativeFamilyGateTest 进行全量真端表行门禁核验
			return null;
		}
		String prefix = "quest " + questId + " counter " + row.counterIndex() + " [" + row.model()
			+ "] (retail): ";
		if (!"COUNTER_GRID".equals(row.model())) {
			// 退役集合只覆盖 COUNTER_GRID；其余模型仍由 XML 侧检查。
			return null;
		}
		QuestDefinition definition = ProductionQuestDefinitions.definition(questId).definition();
		String name = "var" + (row.counterIndex() - 1);
		BitField field = definition.progressLayout().fields().stream()
			.filter(candidate -> candidate.name().equals(name)).findFirst().orElse(null);
		if (field == null) {
			return prefix + "missing 6-bit progress field " + name;
		}
		if (field.width() != 6 || field.offset() != 6 * (row.counterIndex() - 1)) {
			return prefix + "field geometry mismatch: " + field;
		}
		int max = definition.nodes().stream()
			.mapToInt(node -> node.projection().variables().getOrDefault(name, 0)).max().orElse(0);
		if (max != row.required()) {
			return prefix + "max reachable " + max + " but retail countN=" + row.required();
		}
		Set<Integer> counted = new HashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if (transition.event() instanceof QuestEvent.KillNpc kill) {
				counted.add(kill.npcId());
			}
		}
		List<Integer> missing = row.npcIds().stream().filter(id -> !counted.contains(id)).toList();
		if (!missing.isEmpty()) {
			return prefix + "does not count retail monsters " + missing;
		}
		return null;
	}

	private static String checkRow(Document document, Row row) {
		String prefix = "quest " + row.questId() + " counter " + row.counterIndex() + " [" + row.model() + "]: ";
		Element dimension = findDimension(document, row.counterIndex());
		switch (row.model()) {
			case "COUNTER_GRID" -> {
				Element field = findSixBitField(document, row.counterIndex());
				if (field == null) {
					return prefix + "missing 6-bit progress field at offset " + (6 * (row.counterIndex() - 1));
				}
				if (dimension == null) {
					return prefix + "missing counter-grid dimension for " + field.getAttribute("name");
				}
				int required = Integer.parseInt(dimension.getAttribute("required"));
				if (required != row.required()) {
					return prefix + "required=" + required + " but retail countN=" + row.required();
				}
				Set<Integer> ids = parseIds(dimension.getAttribute("npc-ids"));
				Set<Integer> declared = new HashSet<>(row.npcIds());
				declared.addAll(row.extraIds());
				// 同一客户端显示名（name_id）的其它 npc_id 属等价类：本服实刷的可能正是它们。
				// Sibling npc ids sharing the display name belong to the same target equivalence class.
				Set<Integer> allowed = new HashSet<>(npcIndex.withDisplayNameVariants(declared));
				// 台账里的本服可达目标：真端表未列，但运行期必须继续接受（见台账 reason/evidence）。
				allowed.addAll(serverTargetExceptions.getOrDefault(row.questId(), Map.of())
					.getOrDefault(dimension.getAttribute("field"), Set.of()));
				List<Integer> missing = row.npcIds().stream().filter(id -> !ids.contains(id)).toList();
				List<Integer> unexpected = ids.stream().filter(id -> !allowed.contains(id)).toList();
				if (!missing.isEmpty()) {
					return prefix + "does not count retail monsters " + missing;
				}
				if (!unexpected.isEmpty()) {
					return prefix + "counts monsters outside retail+client/display-name set " + unexpected;
				}
				return null;
			}
			case "KILL_CHAIN" -> {
				Element chain = nthElement(document, "kill-chain", row.counterIndex() - 1);
				if (chain == null) {
					return prefix + "expected serial kill-chain but none found";
				}
				int nodes = chain.getAttribute("nodes").trim().split("\\s+").length;
				if (nodes - 1 != row.required()) {
					return prefix + "kill-chain has " + (nodes - 1) + " kills but retail countN=" + row.required();
				}
				return null;
			}
			case "PENDING_MISMATCH" -> {
				// 已知偏差：快照把它标为待修，因此「被对齐」必须显式刷新快照，不能被静默改掉
				if (dimension == null) {
					return prefix + "expected a diverging counter-grid dimension but none found";
				}
				// 具体偏差（多算/少算/口径冲突）记录在
				// .agents/summary/scriptdll-quest-driver/simple-hunt-reconciliation.tsv，
				// 门禁此处只锁「仍是 counter-grid 形态」，避免静默改动。
				parseIds(dimension.getAttribute("npc-ids"));
				return null;
			}
			case "UNRESOLVED_NAME" -> {
				// 真端怪名在本仓库 NPC 表中不存在（未移植怪）：只锁形态，不做数值断言
				return null;
			}
			case "FIELD_NO_KILL_MODEL" -> {
				// 6 位字段在，但没有 dimension / kill-chain：仍然不算真正建模，锁住该形态
				if (dimension != null) {
					return prefix + "now models this counter as counter-grid; refresh the retail contract snapshot";
				}
				return null;
			}
			default -> {
				// NO_FIELD / WIDE_FIELD：当前没有 6 位计数器字段，一旦出现就必须刷新快照
				if (findSixBitField(document, row.counterIndex()) != null) {
					return prefix + "now has a 6-bit counter field; refresh the retail contract snapshot";
				}
				return null;
			}
		}
	}

	private static Element findSixBitField(Document document, int counterIndex) {
		int offset = 6 * (counterIndex - 1);
		for (Element field : elements(document, "bit-field")) {
			if (Integer.parseInt(field.getAttribute("width")) == 6
				&& Integer.parseInt(field.getAttribute("offset")) == offset) {
				return field;
			}
		}
		return null;
	}

	private static Element findDimension(Document document, int counterIndex) {
		Element field = sixBitFieldName(document, counterIndex);
		if (field == null) {
			return null;
		}
		for (Element dimension : elements(document, "dimension")) {
			if (field.getAttribute("name").equals(dimension.getAttribute("field"))) {
				return dimension;
			}
		}
		return null;
	}

	private static Element sixBitFieldName(Document document, int counterIndex) {
		Element direct = findSixBitField(document, counterIndex);
		if (direct != null) {
			return direct;
		}
		// WIDE_FIELD/NO_FIELD 场景：仍尝试按 offset 覆盖范围找字段，便于区分「已建模」与「未建模」
		int offset = 6 * (counterIndex - 1);
		for (Element field : elements(document, "bit-field")) {
			int start = Integer.parseInt(field.getAttribute("offset"));
			int width = Integer.parseInt(field.getAttribute("width"));
			if (start <= offset && offset < start + width) {
				return field;
			}
		}
		return null;
	}

	private static Element nthElement(Document document, String tag, int index) {
		List<Element> found = elements(document, tag);
		return index >= 0 && index < found.size() ? found.get(index) : null;
	}

	private static List<Element> elements(Document document, String tag) {
		NodeList nodes = document.getElementsByTagName(tag);
		List<Element> result = new ArrayList<>(nodes.getLength());
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node instanceof Element element) {
				result.add(element);
			}
		}
		return result;
	}

	private static Set<Integer> parseIds(String raw) {
		Set<Integer> ids = new LinkedHashSet<>();
		if (raw == null || raw.isBlank() || "-".equals(raw.trim())) {
			return ids;
		}
		for (String token : raw.trim().split("\\s+")) {
			if (!token.isBlank()) {
				ids.add(Integer.parseInt(token));
			}
		}
		return ids;
	}

	/** 读取例外台账：任务 → 字段 → 允许的额外 npc id。 / Loads the server-side target exception ledger. */
	private static Map<Integer, Map<String, Set<Integer>>> loadServerTargetExceptions() throws IOException {
		Map<Integer, Map<String, Set<Integer>>> ledger = new LinkedHashMap<>();
		try (InputStream input = QuestSimpleHuntRetailContractTest.class
			.getResourceAsStream(SERVER_TARGET_EXCEPTIONS)) {
			assertNotNull(input, "missing server target exception ledger");
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("#") || line.isBlank() || line.startsWith("quest_id\t")) {
						continue;
					}
					String[] cols = line.split("\t", -1);
					assertEquals(5, cols.length, "exception row must have 5 columns: " + line);
					assertTrue(Set.of("KILL_DECLARATION", "SERVER_SPAWN_VARIANT", "RETAIL_TARGET_UNREACHABLE")
						.contains(cols[3]), "unknown exception reason: " + line);
					assertFalse(cols[4].isBlank(), "exception row must carry evidence: " + line);
					ledger.computeIfAbsent(Integer.parseInt(cols[0]), key -> new LinkedHashMap<>())
						.computeIfAbsent(cols[1], key -> new LinkedHashSet<>())
						.add(Integer.parseInt(cols[2]));
				}
			}
		}
		assertFalse(ledger.isEmpty(), "server target exception ledger must not be empty");
		return ledger;
	}

	private static Document loadQuestDocument(int questId) throws Exception {
		try (InputStream input = QuestXmlFixtures.open(questId)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			return factory.newDocumentBuilder().parse(input);
		}
	}

	/** 已退役任务 id（保留清单 owner=RETAIL_TABLE）。 / Retired quest ids from the retention manifest. */
	private static Set<Integer> retiredFixtureIds() {
		return RetiredQuestIds.all();
	}

	private static Map<Integer, List<Row>> loadContract() throws Exception {
		Map<Integer, List<Row>> contract = new HashMap<>();
		try (InputStream input = QuestSimpleHuntRetailContractTest.class.getResourceAsStream(CONTRACT_RESOURCE)) {
			assertNotNull(input, "missing retail SimpleHunt contract snapshot");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.startsWith("#") || line.startsWith("quest_id\t")) {
						continue;
					}
					String[] cols = line.split("\t", -1);
					assertEquals(6, cols.length, "contract row must have 6 columns: " + line);
					Row row = new Row(Integer.parseInt(cols[0]), Integer.parseInt(cols[1]), cols[2],
						Integer.parseInt(cols[3]), parseIds(cols[4]), parseIds(cols[5]));
					contract.computeIfAbsent(row.questId(), key -> new ArrayList<>()).add(row);
				}
			}
		}
		assertFalse(contract.isEmpty(), "retail SimpleHunt contract must not be empty");
		return contract;
	}

	/** 快照行 / one line of the retail counter contract snapshot. */
	private record Row(int questId, int counterIndex, String model, int required, Set<Integer> npcIds,
			Set<Integer> extraIds) {
	}
}
