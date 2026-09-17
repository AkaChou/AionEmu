package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务接取元数据（start-metadata）真端合同门禁。
 * <p>
 * 以 Aion 5.8 真端解包 quest.xml 的快照
 * ({@code /quest/quest-start-metadata-retail-contract.tsv}) 为权威，逐任务锁定：
 * <ul>
 * <li>min-level 精确匹配（真端占位任务跳过）；</li>
 * <li>max-level sentinel 语义（真端 0/998/999 与生产 2147483647/999/998 均为无上限，
 * 服务端有意封顶值 82 以例外清单放行，其余数值必须精确匹配）；</li>
 * <li>faction 等价表达（真端 pc_light pc_dark = 生产 PC_ALL）与阵营拆分配对例外；</li>
 * <li>gender 与 max_repeat_count 全量匹配（当前全库 0 差异，防止回归）。</li>
 * </ul>
 * 覆盖全部 catalog 条目：EXECUTABLE 取编译后 metadata，METADATA_ONLY 按其 resource
 * XML 解析 metadata（两类任务对玩家都生效接取资格）。
 * 基线由 .agents/summary/quest-systemic-goal/build_retail_contract_tsv.py 从真端数据再算。
 */
class QuestRetailStartMetadataGateTest {

	private static final String CONTRACT_RESOURCE = "/quest/quest-start-metadata-retail-contract.tsv";
	private static final String CAP_EXCEPTION_RESOURCE = "/quest/quest-start-metadata-retail-cap-exceptions.tsv";
	private static final String CATALOG_RESOURCE =
		"/aion/data/static_data/quest_definition/quest_definition_catalog.xml";

	/** 真端占位任务（minlevel_permitted=999）：不可接取，min-level 不纳入比对。 */
	private static final String RETAIL_PLACEHOLDER = "RETAIL_PLACEHOLDER";
	private static final String UNLIMITED = "UNLIMITED";
	/** 服务端有意版本封顶的唯一合法值（全局配置上限 83 的邻近封顶，5.8 玩家 66 级封顶无可见影响）。 */
	private static final int INTENTIONAL_SERVER_CAP = 82;

	/**
	 * 阵营拆分配对例外：真端为双阵营，服务端按阵营拆成 1xxxx/2xxxx 成对任务。
	 * 每条必须同时证明成对镜像存在且阵营互补，禁止新增无配对证据的单边例外。
	 */
	private static final Map<Integer, Integer> FACTION_SPLIT_PAIRS = Map.of(15205, 25205);

	/** 服务端自有任务（真端数据包不存在），全部为 EVENT/活动派发任务。 */
	private static final Set<Integer> SERVER_ONLY_QUESTS = Set.of(
		50110, 50111, 50123, 50124, 51110, 51111, 89999);

	private static Map<Integer, RetailRow> contract;
	private static Set<Integer> capExceptions;
	private static Map<Integer, StartMeta> production;

	record RetailRow(int minLevel, String maxLevel, String faction, String gender, int maxRepeat) {
		boolean isPlaceholder() {
			return minLevel < 0;
		}
	}

	/** 门禁所需的接取元数据视图（EXECUTABLE 与 METADATA_ONLY 统一）。 */
	record StartMeta(int minLevel, int maxLevel, Set<String> races, String gender, int maxRepeat) {
	}

	@BeforeAll
	static void loadFixtures() throws Exception {
		contract = new HashMap<>();
		try (BufferedReader reader = open(CONTRACT_RESOURCE)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("quest_id\t")) {
					continue;
				}
				String[] cols = line.split("\t", -1);
				assertEquals(6, cols.length, "contract row must have 6 columns: " + line);
				int qid = Integer.parseInt(cols[0]);
				int min = RETAIL_PLACEHOLDER.equals(cols[1]) ? -1 : Integer.parseInt(cols[1]);
				contract.put(qid, new RetailRow(min, cols[2], cols[3], cols[4],
					Integer.parseInt(cols[5])));
			}
		}
		assertFalse(contract.isEmpty(), "retail contract must not be empty");

		capExceptions = new java.util.HashSet<>();
		try (BufferedReader reader = open(CAP_EXCEPTION_RESOURCE)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("quest_id\t")) {
					continue;
				}
				String[] cols = line.split("\t", -1);
				assertEquals(INTENTIONAL_SERVER_CAP, Integer.parseInt(cols[1]),
					"cap exceptions may only carry the intentional server cap value: " + line);
				capExceptions.add(Integer.parseInt(cols[0]));
			}
		}
		assertFalse(capExceptions.isEmpty(), "cap exception ledger must not be empty");

		production = new HashMap<>();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(
			QuestRetailStartMetadataGateTest.class.getClassLoader());
		for (CompiledQuestDefinition compiled : catalog.all()) {
			QuestMetadata meta = compiled.definition().metadata();
			production.put(compiled.id(), new StartMeta(meta.minLevel(), meta.maxLevel(),
				meta.permittedRaces(), meta.permittedGender(),
				meta.repeatPolicy().maxRepeatCount()));
		}
		for (int qid : metadataOnlyQuestIds()) {
			production.putIfAbsent(qid, parseMetadataOnly(qid));
		}
		assertFalse(production.isEmpty(), "production catalog must not be empty");
	}

	/** METADATA_ONLY 条目不经过执行编译器，按其 resource XML 解析接取元数据。 */
	/** 供同类门禁复用：METADATA_ONLY 条目的 resource 元数据解析。 */
	static List<Integer> metadataOnlyQuestIds() throws Exception {
		return metadataOnlyQuestIdsInternal();
	}

	/** 供同类门禁复用：解析任务 XML 的 metadata（min/max/races/gender/repeat）。 */
	static QuestMetadata parseQuestMetadata(int questId) throws Exception {
		Element root = parseXml(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")
			.getDocumentElement();
		Element metadata = (Element) root.getElementsByTagName("metadata").item(0);
		Set<String> races = new java.util.HashSet<>();
		var raceNodes = metadata.getElementsByTagName("race");
		for (int i = 0; i < raceNodes.getLength(); i++) {
			races.add(((Element) raceNodes.item(i)).getAttribute("id"));
		}
		String gender = "";
		var genderNodes = metadata.getElementsByTagName("gender");
		if (genderNodes.getLength() > 0) {
			gender = ((Element) genderNodes.item(0)).getAttribute("id");
		}
		int maxRepeat = 1;
		var repeatNodes = metadata.getElementsByTagName("repeat");
		if (repeatNodes.getLength() > 0) {
			maxRepeat = Integer.parseInt(
				((Element) repeatNodes.item(0)).getAttribute("max-repeat-count"));
		}
		// 档位 1 数值奖励：平铺 <rewards>，多档任务取第一个 <group>
		List<QuestReward> rewards = new ArrayList<>();
		var groups = metadata.getElementsByTagName("group");
		var rewardNodes = groups.getLength() > 0
			? ((Element) groups.item(0)).getElementsByTagName("reward")
			: metadata.getElementsByTagName("reward");
		for (int i = 0; i < rewardNodes.getLength(); i++) {
			Element reward = (Element) rewardNodes.item(i);
			rewards.add(new QuestReward(reward.getAttribute("kind"),
				Integer.parseInt(reward.getAttribute("id")),
				Long.parseLong(reward.getAttribute("amount"))));
		}
		return new QuestMetadata(metadata.getAttribute("name"),
			Integer.parseInt(metadata.getAttribute("display-name-id")),
			Integer.parseInt(metadata.getAttribute("min-level")),
			Integer.parseInt(metadata.getAttribute("max-level")),
			races, metadata.getAttribute("category"),
			maxRepeat == 1 ? RepeatPolicy.once()
				: new RepeatPolicy(maxRepeat, 0, false, false),
			Set.of(), List.of(), rewards, List.of(), Set.of(), gender, 0, 1, 1,
			false, false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0,
			List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
			Map.of(), List.of(), List.of(), List.of());
	}

	private static List<Integer> metadataOnlyQuestIdsInternal() throws Exception {
		Element root = parseXml(CATALOG_RESOURCE).getDocumentElement();
		List<Integer> ids = new ArrayList<>();
		var nodes = root.getElementsByTagName("definition");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element definition = (Element) nodes.item(i);
			if ("METADATA_ONLY".equals(definition.getAttribute("mode"))) {
				ids.add(Integer.parseInt(definition.getAttribute("id")));
			}
		}
		return ids;
	}

	private static StartMeta parseMetadataOnly(int questId) throws Exception {
		Element root = parseXml(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")
			.getDocumentElement();
		Element metadata = (Element) root.getElementsByTagName("metadata").item(0);
		Set<String> races = new java.util.HashSet<>();
		var raceNodes = metadata.getElementsByTagName("race");
		for (int i = 0; i < raceNodes.getLength(); i++) {
			races.add(((Element) raceNodes.item(i)).getAttribute("id"));
		}
		String gender = "";
		var genderNodes = metadata.getElementsByTagName("gender");
		if (genderNodes.getLength() > 0) {
			gender = ((Element) genderNodes.item(0)).getAttribute("id");
		}
		int maxRepeat = 1;
		var repeatNodes = metadata.getElementsByTagName("repeat");
		if (repeatNodes.getLength() > 0) {
			maxRepeat = Integer.parseInt(
				((Element) repeatNodes.item(0)).getAttribute("max-repeat-count"));
		}
		return new StartMeta(Integer.parseInt(metadata.getAttribute("min-level")),
			Integer.parseInt(metadata.getAttribute("max-level")), races, gender, maxRepeat);
	}

	private static org.w3c.dom.Document parseXml(String resource) throws Exception {
		try (InputStream input = QuestRetailStartMetadataGateTest.class
				.getResourceAsStream(resource)) {
			assertNotNull(input, resource + " must exist on the test classpath");
			var factory = DocumentBuilderFactory.newInstance();
			factory.setAttribute("http://apache.org/xml/features/disallow-doctype-decl", true);
			var builder = factory.newDocumentBuilder();
			return builder.parse(input);
		}
	}

	private static BufferedReader open(String resource) {
		InputStream input = QuestRetailStartMetadataGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, resource + " must exist on the test classpath");
		return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
	}

	private static String normalizeProductionMaxLevel(int maxLevel) {
		// 生产无上限表达：2147483647（Integer.MAX_VALUE）；999/998 为不可达大值，等效无上限。
		if (maxLevel == Integer.MAX_VALUE || maxLevel == 999 || maxLevel == 998) {
			return UNLIMITED;
		}
		return String.valueOf(maxLevel);
	}

	/**
	 * 生产目录中的每个任务都必须有真端合同行（服务端自有任务除外），
	 * 且合同行不得引用生产目录之外的任务，保证基线与目录同步收敛。
	 */
	@Test
	void contractCoversExactlyTheProductionCatalog() {
		List<String> problems = new ArrayList<>();
		for (Integer qid : production.keySet()) {
			if (!contract.containsKey(qid) && !SERVER_ONLY_QUESTS.contains(qid)) {
				problems.add("quest " + qid + " has no retail contract row");
			}
		}
		for (Integer qid : contract.keySet()) {
			if (!production.containsKey(qid)) {
				problems.add("contract row " + qid + " has no production definition");
			}
		}
		assertTrue(problems.isEmpty(), () -> "contract/catalog mismatch: " + problems);
	}

	/** min-level 必须与真端精确一致；真端占位任务跳过。 */
	@Test
	void minLevelsMatchTheRetailContract() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailRow> entry : contract.entrySet()) {
			RetailRow row = entry.getValue();
			if (row.isPlaceholder()) {
				continue;
			}
			int actual = production.get(entry.getKey()).minLevel();
			if (actual != row.minLevel()) {
				problems.add("quest " + entry.getKey() + " min-level=" + actual
					+ " but retail=" + row.minLevel());
			}
		}
		assertTrue(problems.isEmpty(), () -> "min-level mismatches: " + problems);
	}

	/**
	 * max-level 必须与真端一致（sentinel 归一后）；
	 * 服务端有意封顶 82 只允许出现在例外清单中，清单外一律按真端对齐。
	 */
	@Test
	void maxLevelsMatchTheRetailContractWithSentinelsAndCapLedger() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailRow> entry : contract.entrySet()) {
			int qid = entry.getKey();
			RetailRow row = entry.getValue();
			String actual = normalizeProductionMaxLevel(production.get(qid).maxLevel());
			if (actual.equals(row.maxLevel())) {
				continue;
			}
			boolean capException = UNLIMITED.equals(row.maxLevel())
				&& actual.equals(String.valueOf(INTENTIONAL_SERVER_CAP))
				&& capExceptions.contains(qid);
			if (!capException) {
				problems.add("quest " + qid + " max-level=" + actual
					+ " but retail=" + row.maxLevel());
			}
		}
		// 例外清单必须精确等于仍存活的封顶行，防止例外长期漂移或残留。
		Set<Integer> liveCapRows = new java.util.HashSet<>();
		for (Map.Entry<Integer, RetailRow> entry : contract.entrySet()) {
			String actual = normalizeProductionMaxLevel(production.get(entry.getKey()).maxLevel());
			if (!actual.equals(entry.getValue().maxLevel())
				&& actual.equals(String.valueOf(INTENTIONAL_SERVER_CAP))) {
				liveCapRows.add(entry.getKey());
			}
		}
		assertEquals(liveCapRows, capExceptions,
			"cap exception ledger must exactly match the live intentional-cap rows");
		assertTrue(problems.isEmpty(), () -> "max-level mismatches: " + problems);
	}

	/**
	 * faction：真端单阵营必须精确一致；真端双阵营接受 PC_ALL 等价表达；
	 * 15205/25205 类服务端阵营拆分必须有互补配对任务佐证。
	 */
	@Test
	void factionsMatchTheRetailContractWithPairedSplitExceptions() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailRow> entry : contract.entrySet()) {
			int qid = entry.getKey();
			String retailFaction = entry.getValue().faction();
			Set<String> races = production.get(qid).races();
			boolean wildcard = races.isEmpty() || races.contains("PC_ALL");
			boolean both = races.contains("ELYOS") && races.contains("ASMODIANS");
			switch (retailFaction) {
				case "PC_ALL" -> {
					if (!wildcard && !both) {
						// 服务端阵营拆分（真端双阵营 -> 生产 1xxxx/2xxxx 单阵营成对任务）。
						Integer mirror = FACTION_SPLIT_PAIRS.get(qid);
						if (mirror == null) {
							problems.add("quest " + qid + " races=" + races
								+ " but retail=PC_ALL without a split pair");
						} else {
							assertSplitPair(qid, mirror,
								races.contains("ELYOS") ? "ELYOS" : "ASMODIANS", problems);
						}
					}
				}
				case "ELYOS", "ASMODIANS" -> {
					if (wildcard || both) {
						problems.add("quest " + qid + " races=" + races
							+ " is broader than retail=" + retailFaction);
					} else if (!races.contains(retailFaction)) {
						problems.add("quest " + qid + " races=" + races
							+ " but retail=" + retailFaction);
					}
				}
				default -> problems.add("quest " + qid + " unknown retail faction "
					+ retailFaction);
			}
		}
		assertTrue(problems.isEmpty(), () -> "faction mismatches: " + problems);
	}

	private static void assertSplitPair(int qid, int mirror, String faction,
			List<String> problems) {
		StartMeta mirrorMeta = production.get(mirror);
		if (mirrorMeta == null) {
			problems.add("split pair quest " + qid + " references missing mirror " + mirror);
			return;
		}
		Set<String> mirrorRaces = mirrorMeta.races();
		String expectedMirrorRace = "ELYOS".equals(faction) ? "ASMODIANS" : "ELYOS";
		if (!(mirrorRaces.contains(expectedMirrorRace) && mirrorRaces.size() == 1)) {
			problems.add("split pair " + qid + "/" + mirror + " is not complementary: mirror races="
				+ mirrorRaces);
		}
	}

	/** gender 与重复次数：当前全库与真端 0 差异，锁定为基线防回归。 */
	@Test
	void genderAndRepeatMatchTheRetailContract() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailRow> entry : contract.entrySet()) {
			int qid = entry.getKey();
			RetailRow row = entry.getValue();
			StartMeta meta = production.get(qid);
			String actualGender = meta.gender();
			String expectedGender = "ALL".equals(row.gender()) ? "" : row.gender();
			if (!actualGender.equals(expectedGender)) {
				problems.add("quest " + qid + " gender=" + actualGender
					+ " but retail=" + row.gender());
			}
			int actualRepeat = meta.maxRepeat();
			if (actualRepeat != row.maxRepeat()) {
				problems.add("quest " + qid + " max-repeat-count=" + actualRepeat
					+ " but retail=" + row.maxRepeat());
			}
		}
		assertTrue(problems.isEmpty(), () -> "gender/repeat mismatches: " + problems);
	}

	/**
	 * P1 修复的 9 个 min-level 任务与其镜像任务互证：同一族天/魔任务保持真端一致的同档等级。
	 */
	@Test
	void repairedMinLevelQuestsAgreeWithTheirMirrors() {
		Map<Integer, Integer> expectedMirrorMinLevels = Map.ofEntries(
			Map.entry(1648, 42), Map.entry(2648, 42),
			Map.entry(19000, 50), Map.entry(29000, 50),
			Map.entry(19001, 50), Map.entry(29001, 50),
			Map.entry(19002, 50), Map.entry(29002, 50),
			Map.entry(19003, 50), Map.entry(29003, 50),
			Map.entry(25407, 68), Map.entry(15407, 68),
			Map.entry(25408, 68), Map.entry(15408, 68));
		for (Map.Entry<Integer, Integer> entry : expectedMirrorMinLevels.entrySet()) {
			int actual = production.get(entry.getKey()).minLevel();
			assertEquals(entry.getValue(), actual, "quest " + entry.getKey()
				+ " must keep the retail mirror min-level " + entry.getValue());
		}
		assertEquals(41, production.get(2641).minLevel(),
			"2641 keeps its own retail min-level 41 (mirror 1641 is 42 in retail by design)");
	}
}
