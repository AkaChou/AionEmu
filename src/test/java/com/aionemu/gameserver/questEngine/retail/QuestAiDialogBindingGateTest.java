package com.aionemu.gameserver.questEngine.retail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * D1 常设门：XML 车道任务的对话/交付 NPC 绑定 vs 原版 Quest-AI 注册面。
 * <p>
 * 证据表 = {@code retail-quest-ai-registrations.xml}（生成器
 * {@code .agents/summary/quest-engine-native/p11-quest-ai-lane/emit_quest_ai_registrations.py}，
 * 度量 + 常量导出 {@code measure_binding_gate.py --emit-constants}）：原版
 * {@code FUN_180cb5920(L"name", questId)} 注册面（ScriptDLL 18787 个调用点 / 7043 个任务）
 * × 原版 {@code npcs.xml quest_ai_name} 展开的 npc id，名匹配按原版 {@code _wcsicmp}
 * 语义**大小写不敏感**（证据：{@code fun_249.cpp:3018} 名→id 表二分用 {@code _wcsicmp}，
 * 调用点 {@code fun_040.cpp:9923} = NPCDB::Load quest_ai_name；ScriptDLL 对话名 map 遍历
 * {@code ScriptDLL64.c:2075978/2076005}）。
 * <p>
 * 守卫面：①证据表覆盖 XML 车道**恰好**（733 行 = {@code definitions/quests} 文件名 id
 * 全量，行序升序）且列形自洽（升序去重、行 id ⊆ 全局 id 列）；②对话绑定面逐元素冻结——
 * 每份 XML 的 {@code <dialog npc-id>} / {@code <npc-complete npc-id>} 命中全局 Quest-AI
 * 集的引用，必须落在该任务的注册展开里，例外只有两张显式冻结清单：
 * <ul>
 *   <li>{@link #FROZEN_CROSS_QUEST}：原版注册面按「(注册名, 任务) 对」组织，而**同一 NPC
 *       承接多条任务**（例：204700 Thor 注册于 2514/2611/2619/2641/2646/24053，本仓 2633
 *       的对话位也是它；799763 event_Sonaran 注册于 80016/80017，被 80298–80309 事件链引用）
 *       ⇒ 这类跨界引用不是缺陷，逐元素冻结；</li>
 *   <li>{@link #FROZEN_UNREGISTERED_TASKS}：原版**无任何 Quest-AI 注册**（18787 个调用点里
 *       查无此 id）但本仓 XML 带对话引用的任务，等价于「原版不由 Quest-AI NPC 驱动」——
 *       待逐件裁决，冻结不阻塞。</li>
 * </ul>
 * 新增任一跨界引用/未注册任务即红；收缩须同批改常量（本门不做静默放行）。
 * <p>
 * D1 gate for the XML-lane dialog NPC bindings against the retail Quest-AI registration surface
 * (evidence table {@code retail-quest-ai-registrations.xml}, generator under
 * {@code .agents/summary/quest-engine-native/p11-quest-ai-lane/}). The evidence table must cover the
 * XML lane exactly (733 rows) with a self-consistent shape, and the dialog/complete NPC references
 * that hit the global Quest-AI id set must fall inside each quest's registration expansion — except
 * for two explicitly frozen lists (cross-quest same-NPC references, and quests with no retail Quest-AI
 * registration at all). Growth of either frozen list fails the gate; shrinkage must update the
 * constants in the same batch.
 */
class QuestAiDialogBindingGateTest {

	private static final String TABLE = "/aion/data/static_data/quest/retail/retail-quest-ai-registrations.xml";
	private static final String XML_LANE_DIR = "aion/data/static_data/quest/definitions/quests";
	/** XML 车道文件名 id 形（NNNN.xml）。 / The XML-lane file-name id form. */
	private static final Pattern XML_ID_FILE = Pattern.compile("(\\d+)\\.xml");
	/** 对话绑定面：对话页 + 交付/领奖页（击杀/掉落等目标位不是 Quest-AI 对话位）。 / The dialog binding surface. */
	private static final Pattern DIALOG_REF = Pattern.compile("<(?:dialog|npc-complete)\\b[^>]*npc-id=\"(\\d+)\"");
	/** XML 车道文件数（2026-10-03 生产目录全量）。 / The XML-lane file count (production snapshot). */
	private static final int XML_LANE_FILES = 733;
	/** 证据表里解析出注册展开的行数（636）与全局 Quest-AI NPC id 数（10136）。 */
	private static final int COVERED_ROWS = 636;
	private static final int GLOBAL_AI_NPC_IDS = 10136;
	/** 对话绑定面命中全局集的 (任务, NPC) 对数（1402 = 1657 条去重引用中的命中数）。 */
	private static final int DIALOG_HITS_ON_AI_SURFACE = 1402;

	/**
	 * 跨界冻结（21 任务 / 25 引用）：原版注册面是 (注册名, 任务) 对，同一 Quest-AI NPC 可承接
	 * 多条任务，故既有 XML 的对话 NPC 不必然出现在本任务的注册展开里（逐元素冻结）。
	 * 观测面 21 任务 / 25 引用，逐元素冻结（新增即红，收缩须同批改常量）。
	 */
	private static final Map<Integer, Set<Integer>> FROZEN_CROSS_QUEST = Map.ofEntries(
			Map.entry(2633, Set.of(204700)),
			Map.entry(3210, Set.of(798332)),
			Map.entry(3212, Set.of(798321)),
			Map.entry(4210, Set.of(204283, 798332)),
			Map.entry(4212, Set.of(204283, 798058, 798065)),
			Map.entry(4914, Set.of(204837)),
			Map.entry(14016, Set.of(700141, 700142)),
			Map.entry(18510, Set.of(799522)),
			Map.entry(28209, Set.of(205320)),
			Map.entry(80298, Set.of(799763)),
			Map.entry(80299, Set.of(799763)),
			Map.entry(80300, Set.of(799763)),
			Map.entry(80301, Set.of(799763)),
			Map.entry(80302, Set.of(799763)),
			Map.entry(80303, Set.of(799763)),
			Map.entry(80304, Set.of(799763)),
			Map.entry(80305, Set.of(799763)),
			Map.entry(80306, Set.of(799763)),
			Map.entry(80307, Set.of(799763)),
			Map.entry(80308, Set.of(799763)),
			Map.entry(80309, Set.of(799763)));

	/** 原版无 Quest-AI 注册但 XML 带对话引用的任务（18 件，逐元素冻结）。 */
	private static final Set<Integer> FROZEN_UNREGISTERED_TASKS = Set.of(
			1195, 10032, 10112, 10527, 10530, 17540, 20031, 20035,
			20112, 20527, 20530, 21030, 27540, 50038, 50040, 50041,
			50125, 51125);

	private static Set<Integer> laneIds = Set.of();
	private static Map<Integer, Set<Integer>> registeredByQuest = Map.of();
	private static Map<Integer, String> registeredNamesByQuest = Map.of();
	private static Map<Integer, String> registeredIdsByQuest = Map.of();
	private static Set<Integer> globalAi = Set.of();
	private static Map<Integer, Set<Integer>> dialogRefs = Map.of();

	@BeforeAll
	static void setUp() throws Exception {
		laneIds = scanXmlLaneIds();
		readEvidenceTable();
		dialogRefs = scanDialogRefs();
	}

	/** ①证据表覆盖 XML 车道恰好，且列形自洽（升序去重、行 id ⊆ 全局集、注册名必解析）。 */
	@Test
	void evidenceTableCoversTheXmlLaneExactly() {
		assertEquals(XML_LANE_FILES, laneIds.size(), "XML 车道路径形状变了：文件数不等于冻结快照");
		assertEquals(laneIds, new TreeSet<>(registeredByQuest.keySet()),
			"证据表行 id 必须与 XML 车道文件名 id 全等；新增/删除定义后须重跑生成器（--emit）");

		assertEquals(GLOBAL_AI_NPC_IDS, globalAi.size(),
			"全局 Quest-AI NPC id 数漂移：重跑 measure_binding_gate.py 并复核实端注册面");
		long covered = registeredByQuest.values().stream().filter(ids -> !ids.isEmpty()).count();
		assertEquals(COVERED_ROWS, covered, "有注册展开的行数漂移：重跑生成器并复核");
		for (Map.Entry<Integer, Set<Integer>> entry : registeredByQuest.entrySet()) {
			assertTrue(globalAi.containsAll(entry.getValue()),
				() -> "任务 " + entry.getKey() + " 的注册 id 不在全局 id 列内：" + entry.getValue());
		}
		assertTrue(registeredNamesAreSortedUnique(), "registered_names 必须升序去重（生成器形）");
		assertTrue(registeredIdsAreSortedUnique(), "registered_npc_ids 必须升序去重（生成器形）");
		assertEquals(Set.of(), namesWithoutResolvedIds(),
			"这些任务有注册名却解析不出 npc：原版 _wcsicmp 折叠口径下应全解析，须复核 npcs.xml/注册名拼写");
	}

	/** ②对话绑定面：命中全局 Quest-AI 集的引用除两张冻结清单外必须落在本任务注册展开内。 */
	@Test
	void dialogBindingsMatchTheFrozenRetailSurface() {
		Map<Integer, Set<Integer>> violations = new TreeMap<>();
		Map<Integer, Set<Integer>> unregistered = new TreeMap<>();
		int hits = 0;
		for (Map.Entry<Integer, Set<Integer>> entry : dialogRefs.entrySet()) {
			int quest = entry.getKey();
			Set<Integer> onAiSurface = new TreeSet<>(entry.getValue());
			onAiSurface.retainAll(globalAi);
			hits += onAiSurface.size();
			// 「未注册任务」= 原版注册面没有该 id 的任何 (name, questId) 调用 ⇒ 本行为空注册名列。
			// "Unregistered" means the retail registration surface has no call for this quest id.
			if (registeredNamesByQuest.getOrDefault(quest, "").isBlank()) {
				if (!onAiSurface.isEmpty()) {
					unregistered.put(quest, onAiSurface);
				}
				continue;
			}
			Set<Integer> bad = new TreeSet<>(onAiSurface);
			bad.removeAll(registeredByQuest.getOrDefault(quest, Set.of()));
			if (!bad.isEmpty()) {
				violations.put(quest, bad);
			}
		}
		assertEquals(DIALOG_HITS_ON_AI_SURFACE, hits,
			"对话绑定面命中全局 Quest-AI 集的引用数漂移：重跑 measure_binding_gate.py 复核");
		assertEquals(FROZEN_CROSS_QUEST, violations,
			"跨界对话引用集合漂移（新增即红）：核对原版注册面后同批改 FROZEN_CROSS_QUEST");
		assertEquals(FROZEN_UNREGISTERED_TASKS, new TreeSet<>(unregistered.keySet()),
			"未注册但有对话引用的任务集合漂移（新增即红）：核对原版注册面后同批改 FROZEN_UNREGISTERED_TASKS");
	}

	// ---- 读取面 ---------------------------------------------------------------------------------

	private static void readEvidenceTable() throws Exception {
		try (InputStream input = open(TABLE)) {
			Document document = RetailLedgerXml.parse(input);
			List<org.w3c.dom.Element> globalRows = RetailLedgerXml.rows(document, "quest_ai_npc_ids");
			assertEquals(1, globalRows.size(), "证据表必须恰有一个 quest_ai_npc_ids 元素");
			globalAi = parseIds(globalRows.get(0).getTextContent());
			Map<Integer, Set<Integer>> rows = new LinkedHashMap<>();
			Map<Integer, String> names = new LinkedHashMap<>();
			Map<Integer, String> ids = new LinkedHashMap<>();
			for (org.w3c.dom.Element row : RetailLedgerXml.rows(document, "quest_ai_registration")) {
				String quest = RetailLedgerXml.text(row, "quest_id");
				assertNotNull(quest, "证据表行缺 quest_id");
				int questId = Integer.parseInt(quest);
				String rawNames = RetailLedgerXml.text(row, "registered_names");
				String rawIds = RetailLedgerXml.text(row, "registered_npc_ids");
				rows.put(questId, parseIds(rawIds));
				names.put(questId, rawNames == null ? "" : rawNames);
				ids.put(questId, rawIds == null ? "" : rawIds);
			}
			registeredByQuest = Map.copyOf(rows);
			registeredNamesByQuest = Map.copyOf(names);
			registeredIdsByQuest = Map.copyOf(ids);
		}
	}

	private static Set<Integer> scanXmlLaneIds() throws Exception {
		Set<Integer> ids = new TreeSet<>();
		for (File file : xmlLaneFiles()) {
			Matcher matcher = XML_ID_FILE.matcher(file.getName());
			if (!matcher.matches()) {
				throw new AssertionError("XML 车道目录出现非 NNNN.xml 文件：" + file.getName());
			}
			ids.add(Integer.parseInt(matcher.group(1)));
		}
		return ids;
	}

	private static Map<Integer, Set<Integer>> scanDialogRefs() throws Exception {
		Map<Integer, Set<Integer>> refs = new TreeMap<>();
		for (File file : xmlLaneFiles()) {
			int quest = Integer.parseInt(file.getName().substring(0, file.getName().length() - ".xml".length()));
			Set<Integer> ids = new LinkedHashSet<>();
			String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			Matcher matcher = DIALOG_REF.matcher(text);
			while (matcher.find()) {
				ids.add(Integer.parseInt(matcher.group(1)));
			}
			if (!ids.isEmpty()) {
				refs.put(quest, new TreeSet<>(ids));
			}
		}
		return Map.copyOf(refs);
	}

	private static List<File> xmlLaneFiles() throws Exception {
		URL dirUrl = QuestAiDialogBindingGateTest.class.getClassLoader().getResource(XML_LANE_DIR);
		assertNotNull(dirUrl, () -> "类路径缺 " + XML_LANE_DIR);
		File dir = new File(URI.create(dirUrl.toExternalForm().replace(" ", "%20")));
		File[] files = dir.listFiles(File::isFile);
		assertNotNull(files, () -> XML_LANE_DIR + " 必须是可列举的目录（测试在 target/classes 上跑）");
		return Arrays.stream(files).sorted().toList();
	}

	private static Set<Integer> parseIds(String commaList) {
		if (commaList == null || commaList.isBlank()) {
			return Set.of();
		}
		Set<Integer> ids = new LinkedHashSet<>();
		for (String token : commaList.split(",")) {
			ids.add(Integer.parseInt(token.trim()));
		}
		return ids;
	}

	private static boolean registeredNamesAreSortedUnique() {
		return registeredNamesByQuest.values().stream().allMatch(QuestAiDialogBindingGateTest::isSortedUnique);
	}

	private static boolean registeredIdsAreSortedUnique() {
		return registeredIdsByQuest.values().stream()
			.allMatch(ids -> isSortedUnique(ids, Integer::parseInt));
	}

	private static Set<Integer> namesWithoutResolvedIds() {
		Set<Integer> quests = new TreeSet<>();
		registeredNamesByQuest.forEach((quest, names) -> {
			if (!names.isBlank() && registeredByQuest.getOrDefault(quest, Set.of()).isEmpty()) {
				quests.add(quest);
			}
		});
		return quests;
	}

	private static boolean isSortedUnique(String commaList) {
		return isSortedUnique(commaList, token -> token);
	}

	private static <T extends Comparable<T>> boolean isSortedUnique(String commaList,
			Function<String, T> parse) {
		if (commaList.isBlank()) {
			return true;
		}
		String[] tokens = commaList.split(",");
		for (int i = 1; i < tokens.length; i++) {
			if (parse.apply(tokens[i - 1]).compareTo(parse.apply(tokens[i])) >= 0) {
				return false;
			}
		}
		return true;
	}

	private static InputStream open(String resource) {
		InputStream input = QuestAiDialogBindingGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, () -> "missing " + resource);
		return input;
	}
}
