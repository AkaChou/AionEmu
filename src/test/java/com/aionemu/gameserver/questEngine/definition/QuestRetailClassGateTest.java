package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

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
import java.util.TreeSet;

import com.aionemu.gameserver.questEngine.retail.RetailLedgerRows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务职业权限（class axis）真端合同门禁。
 * <p>
 * 以 Aion 5.8 真端 quest.xml 的职业 token 快照
 * ({@code /quest/quest-class-retail-contract.tsv}) 为权威，锁定"实际可接受职业集合"：
 * <ul>
 * <li>真端职业 token 与服务端 PlayerClass 一一映射（warrior->WARRIOR、
 * fighter->GLADIATOR、knight->TEMPLAR、elementallist->SPIRIT_MASTER、
 * priest->CLERIC、engineer->TECHNIST、rider->AETHERTECH、artist->MUSE 等）；</li>
 * <li>接取等级 >= 10（转职）后，真端 base token（warrior/scout/mage/cleric/
 * engineer/artist）与服务端遗留 base 声明都是不可达的死条目，比对前双方删除；</li>
 * <li>生产 classes 为空 = 全职业通配，只允许出现在真端全集或例外清单中。</li>
 * </ul>
 * 例外（每条逐项列明，禁止通配豁免）：
 * <ul>
 * <li>EVIDENCE_BLOCKED：24050~24054（真端缺 MUSE 疑似笔误）、3910、4931（双向差异，
 * 17 级使命链语义缺仓库内证据）；</li>
 * <li>INTENTIONAL 武器适配：3121/30350（密码之刃=技匠系武器）、30237（新枪矛=战士系
 * 武器）——服务端按奖励武器限定职业，真端未按武器更新。</li>
 * </ul>
 */
class QuestRetailClassGateTest {

	private static final String CONTRACT_RESOURCE = "/quest/quest-class-retail-contract.tsv";
	/** 真端元数据层已登记的职业轴分歧（真端优先）。 / Registered retail-priority class-axis divergences. */
	private static final String DIVERGENCE_RESOURCE = "/quest/retail-metadata-divergences.tsv";
	/** 保留清单：owner=RETAIL_TABLE 的任务由真端定义驱动。 / Retention manifest: retail-driven quest owners. */
	private static final String RETENTION_RESOURCE = "/aion/data/static_data/quest/retail/retail-xml-retention.xml";

	private static final String RETAIL_PLACEHOLDER = "RETAIL_PLACEHOLDER";

	/** 转职（>=10 级）后不再存在的 base 职业：真端 token 与生产遗留声明均按死条目删除。 */
	private static final Set<String> DEAD_BASE_CLASSES = Set.of(
		"WARRIOR", "SCOUT", "MAGE", "PRIEST", "TECHNIST", "MUSE");

	/** 真端职业 token -> 服务端 PlayerClass。 */
	private static final Map<String, String> TOKEN_MAP = Map.ofEntries(
		Map.entry("warrior", "WARRIOR"), Map.entry("fighter", "GLADIATOR"),
		Map.entry("knight", "TEMPLAR"), Map.entry("scout", "SCOUT"),
		Map.entry("assassin", "ASSASSIN"), Map.entry("ranger", "RANGER"),
		Map.entry("mage", "MAGE"), Map.entry("wizard", "SORCERER"),
		Map.entry("elementallist", "SPIRIT_MASTER"), Map.entry("cleric", "PRIEST"),
		Map.entry("priest", "CLERIC"), Map.entry("chanter", "CHANTER"),
		Map.entry("engineer", "TECHNIST"), Map.entry("gunner", "GUNSLINGER"),
		Map.entry("rider", "AETHERTECH"), Map.entry("artist", "MUSE"),
		Map.entry("bard", "SONGWEAVER"));

	/** EVIDENCE_BLOCKED：仓库内无法判定真端语义，保持生产现状并记录取证方向。 */
	private static final Set<Integer> EVIDENCE_BLOCKED = Set.of(
		24050, 24051, 24052, 24053, 24054, 3910, 4931);

	/** INTENTIONAL：服务端按奖励武器类型限定的职业适配（真端未按武器更新）。 */
	private static final Set<Integer> INTENTIONAL_WEAPON_ADAPTATION = Set.of(3121, 30237, 30350);

	private static Map<Integer, RetailClassRow> contract;
	private static Map<Integer, ClassMeta> production;
	private static Set<Integer> registeredClassDivergences;
	private static Set<Integer> retailOwnedIds;

	record RetailClassRow(int minLevel, Set<String> tokens) {
	}

	/** 门禁所需的职业元数据视图。 */
	record ClassMeta(int minLevel, Set<String> classes) {
	}

	@BeforeAll
	static void loadFixtures() throws IOException {
		registeredClassDivergences = registeredClassDivergences();
		retailOwnedIds = retailOwnedIds();
		contract = new HashMap<>();
		try (BufferedReader reader = open(CONTRACT_RESOURCE)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("quest_id\t")) {
					continue;
				}
				String[] cols = line.split("\t", -1);
				assertEquals(3, cols.length, "contract row must have 3 columns: " + line);
				int qid = Integer.parseInt(cols[0]);
				int min = RETAIL_PLACEHOLDER.equals(cols[1]) ? -1 : Integer.parseInt(cols[1]);
				contract.put(qid, new RetailClassRow(min, Set.of(cols[2].split(" "))));
			}
		}
		assertFalse(contract.isEmpty(), "class contract must not be empty");

		production = new HashMap<>();
		// 生产视图 = XML 目录 + 真端 overlay：SimpleTalk 全族已迁到真端驱动。
		// Production view = XML directory plus the retail overlay.
		QuestCatalog catalog = com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.overlay(
			QuestDefinitionDirectoryLoader.compile(QuestRetailClassGateTest.class.getClassLoader()));
		for (CompiledQuestDefinition compiled : catalog.all()) {
			QuestMetadata meta = compiled.definition().metadata();
			production.put(compiled.id(), new ClassMeta(meta.minLevel(), meta.permittedClasses()));
		}
		assertFalse(production.isEmpty(), "production catalog must not be empty");
	}

	/** 读取测试作用域资源（非任务 XML）。 / Reads a test-scope resource that is not a quest definition. */
	private static BufferedReader open(String resource) {
		InputStream input = QuestRetailClassGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, resource + " must exist on the test classpath");
		return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
	}

	/** 真端 token 集 -> 转职后实际可接受职业集合（死条目删除）。 */
	private static Set<String> retailAliveClasses(RetailClassRow row) {
		Set<String> mapped = new TreeSet<>();
		for (String token : row.tokens()) {
			String mappedClass = TOKEN_MAP.get(token);
			assertNotNull(mappedClass, "unmapped retail class token: " + token);
			mapped.add(mappedClass);
		}
		if (row.minLevel() >= 10) {
			mapped.removeAll(DEAD_BASE_CLASSES);
		}
		return mapped;
	}

	/** 保留清单里 owner=RETAIL_TABLE 的任务（定义来自真端合成器）。 / Retail-driven quest ids. */
	private static Set<Integer> retailOwnedIds() throws IOException {
		Set<Integer> ids = new TreeSet<>();
		for (Element row : RetailLedgerRows.rows(RETENTION_RESOURCE, "quest")) {
			if ("RETAIL_TABLE".equals(RetailLedgerRows.cell(row, "owner"))) {
				ids.add(Integer.parseInt(RetailLedgerRows.cell(row, "quest_id")));
			}
		}
		return Set.copyOf(ids);
	}

	/** 真端元数据分歧登记里 axis=classes 的任务（真端优先）。 / Quests with a registered class-axis divergence. */
	private static Set<Integer> registeredClassDivergences() throws IOException {
		Set<Integer> ids = new TreeSet<>();
		try (BufferedReader reader = open(DIVERGENCE_RESOURCE)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length >= 3 && "classes".equals(parts[1])) {
					ids.add(Integer.parseInt(parts[0]));
				}
			}
		}
		return Set.copyOf(ids);
	}

	/** 生产声明 -> 实际可接受职业集合（空 = 通配；死条目删除）。 */
	private static Set<String> productionAliveClasses(ClassMeta meta) {
		Set<String> classes = new TreeSet<>(meta.classes());
		if (classes.isEmpty()) {
			return Set.of("WILDCARD");
		}
		if (meta.minLevel() >= 10) {
			classes.removeAll(DEAD_BASE_CLASSES);
		}
		return classes;
	}

	/** 生产实际可接受职业集合必须与真端一致，或命中逐条列明的例外。 */
	@Test
	void acceptableClassSetsMatchTheRetailContractWithListedExceptions() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailClassRow> entry : contract.entrySet()) {
			int qid = entry.getKey();
			ClassMeta meta = production.get(qid);
			if (meta == null) {
				// P8 重锚（§执行位取证 p8/class-axis-execution-position.zh-CN.md）：native 行
				// （owner RETAIL_TABLE，七族 ∨ DD 1467 行）退出 typed 目录，无目录载体不算缺失——
				// **不是**因为职业轴是显示元数据（该旧表述被反编译取证推翻：真端 CanAcquireQuest
				// 第一道闸就是职业位测试，先于等级）；native 行的职业轴由
				// {@code NativeQuestStartPort.eligibilityVerdict}（CLASS_BLOCKED）与
				// {@code NativeFactionRotation.eligible} 直读真端 quest.xml 执行，
				// 与本合同快照同源（恒等断言见 nativeClassAxisIsEnforcedThroughTheSharedPortWordlist）。
				// 非 native 行缺目录载体仍是真回归。
				// P8 re-anchor: native rows left the typed catalog so a missing carrier is not a
				// defect; the retail class axis is a server-side acquire gate enforced for native
				// rows by the start port and faction rotation reading quest.xml directly. For
				// non-native rows a missing carrier still is a regression.
				if (!retailOwnedIds.contains(qid)) {
					problems.add("contract quest " + qid + " missing from production catalog");
				}
				continue;
			}
			Set<String> retailAlive = retailAliveClasses(entry.getValue());
			Set<String> prodAlive = productionAliveClasses(meta);
			if (prodAlive.equals(retailAlive)) {
				continue;
			}
			if (EVIDENCE_BLOCKED.contains(qid) || INTENTIONAL_WEAPON_ADAPTATION.contains(qid)) {
				continue;
			}
			// 职业轴分歧已登记为真端优先（retail-metadata-divergences.tsv，axis=classes）：
			// 真端 base token 在转职后可展开为两条进阶线，客户端契约表的窄口径不再作为判据。
			if (registeredClassDivergences.contains(qid)) {
				continue;
			}
			problems.add("quest " + qid + " acceptable classes=" + prodAlive
				+ " but retail=" + retailAlive);
		}
		assertTrue(problems.isEmpty(), () -> "class mismatches: " + problems);
	}

	/** 生产通配必须只出现在真端全集行或例外清单中；基线行内出现通配即失败。 */
	@Test
	void wildcardsAreLimitedToRetailFullSetsAndListedExceptions() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailClassRow> entry : contract.entrySet()) {
			int qid = entry.getKey();
			if (production.get(qid) == null && retailOwnedIds.contains(qid)) {
				continue; // P8：native 行无 typed 目录载体（见第一个测试的执行位取证注）。 / Native rows: no typed carrier (see the execution-position note).
			}
			if (productionAliveClasses(production.get(qid)).equals(Set.of("WILDCARD"))
				&& !EVIDENCE_BLOCKED.contains(qid) && !INTENTIONAL_WEAPON_ADAPTATION.contains(qid)) {
				problems.add("quest " + qid + " is a wildcard but retail limits classes to "
					+ retailAliveClasses(entry.getValue()));
			}
		}
		assertTrue(problems.isEmpty(), () -> "unlisted wildcard quests: " + problems);
	}

	/** 真端 base token 在转职（>=10 级）后展开为两条进阶线。 / Base token expansion after class change. */
	private static final Map<String, List<String>> BASE_ADVANCED = Map.of(
		"warrior", List.of("GLADIATOR", "TEMPLAR"), "scout", List.of("ASSASSIN", "RANGER"),
		"mage", List.of("SORCERER", "SPIRIT_MASTER"), "cleric", List.of("CLERIC", "CHANTER"),
		"engineer", List.of("GUNSLINGER", "AETHERTECH"), "artist", List.of("SONGWEAVER"));

	/** 修复代表任务：导师任务族、Kaliga 武器收集族、Dark Poeta/守护者英雄分组、机甲星使命、枪星导师。 */
	private static final Set<Integer> REPAIRED_QUESTS = new TreeSet<>(List.of(
		3928, 3929, 4922, 4926, 4927, 4928, 4929, 18618, 18621, 18625, 28643, 28648,
		80219, 80220, 80317, 18614, 28630, 19074, 14031, 24031, 1466, 11076));

	/**
	 * 修复代表任务的显式职业集合断言。
	 * <p>
	 * 2026-09-23 口径（真端优先）：这批任务的职业轴在 {@code retail-metadata-divergences.tsv} 里登记为
	 * {@code classes/RETAIL_PRIORITY}——真端 {@code class_permitted} 的 base token 在转职后展开为两条进阶线，
	 * 旧的"客户端窄口径 + 死条目删除"期望不再作为判据，断言改为与真端展开口径一致。
	 * Representative quests must expose the retail-expanded class set (registered retail priority).
	 */
	@Test
	void repairedQuestsExposeExactlyTheirRetailClassSets() {
		for (int questId : REPAIRED_QUESTS) {
			RetailClassRow row = contract.get(questId);
			// 口径按 owner 分流：真端驱动任务按真端展开口径；XML 保留任务仍按客户端窄口径。
			// The expectation follows the owner: retail-expanded for retail-driven quests, client-narrow otherwise.
			Set<String> expected;
			if (row == null) {
				expected = FULL_ADVANCED_CLASSES;
			} else if (retailOwnedIds.contains(questId)) {
				expected = retailExpandedClasses(row);
			} else {
				expected = retailAliveClasses(row);
			}
			assertTrue(registeredClassDivergences.contains(questId),
				"quest " + questId + " must be registered in the classes divergence ledger");
			if (production.get(questId) == null) {
				// P8：native 行无 typed 目录载体——展开口径由真端 quest.xml 元数据承担
				// （执行位 = NativeQuestStartPort.eligibilityVerdict，见执行位取证注）。
				continue;
			}
			assertEquals(expected, productionAliveClasses(production.get(questId)),
				"quest " + questId + " must expose exactly its retail-expanded class set");
		}
	}

	/**
	 * 执行位恒等（P8 取证批，`p8/class-axis-execution-position.zh-CN.md`）：真端职业轴是服务端接取
	 * 闸门（CanAcquireQuest 第一道位测试，先于等级），native 行的执行位 = start port / 阵营轮换
	 * 直读真端 quest.xml。本断言把三点钉死：**评审快照 ↔ 真端行原文 ↔ port 词表**——对合同快照
	 * 每一行，从 {@code NativeQuestXmlTable} 直读原文交给 port 的同一映射函数，结果必须等于门禁
	 * 展开口径（快照陈旧 / quest.xml 重入仓漂移 / 词表映射改动，任何一端漂移即红）。
	 * <p>
	 * Execution-position identity: the retail class axis is a server-side acquire gate enforced for
	 * native rows by the start port and faction rotation reading quest.xml directly. For every
	 * contract row the same port mapping function on the raw retail row must equal the gate's
	 * expanded expectation — drift on any side turns this red.
	 */
	@Test
	void nativeClassAxisIsEnforcedThroughTheSharedPortWordlist() {
		int checked = 0;
		for (Map.Entry<Integer, RetailClassRow> entry : contract.entrySet()) {
			int questId = entry.getKey();
			com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable.QuestRow row =
				com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable.instance()
					.find(questId)
					.orElseThrow(() -> new AssertionError(
						"contract quest " + questId + " has no retail quest.xml row"));
			int minLevel = row.integer("minlevel_permitted") == null
				? 0 : row.integer("minlevel_permitted");
			Set<String> portWordlist = com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler
				.permittedClassNames(row.text("class_permitted"), minLevel);
			// port 口径：≥16 token = 全集通配（空集）；合同快照人口实测 1..13 token，该边界为防御位。
			// Port semantics: >=16 tokens mean the unrestricted full set (empty wordlist).
			Set<String> expected = entry.getValue().tokens().size() >= 16
				? Set.of()
				: retailExpandedClasses(entry.getValue());
			assertEquals(expected, portWordlist,
				() -> "quest " + questId + ": reviewed class contract, retail quest.xml row and the"
					+ " port wordlist must stay one fact");
			checked++;
		}
		assertTrue(checked > 100, "class contract population must stay meaningful: " + checked);
	}

	/** 真端全集行展开后的 11 个进阶职业。 / Every advanced class, i.e. the expanded full retail set. */
	private static final Set<String> FULL_ADVANCED_CLASSES = Set.of(
		"GLADIATOR", "TEMPLAR", "ASSASSIN", "RANGER", "SORCERER", "SPIRIT_MASTER", "CLERIC",
		"CHANTER", "GUNSLINGER", "AETHERTECH", "SONGWEAVER");

	/** 真端 token 展开口径：base token 转职后展开两条进阶线，其余直接映射。 / Retail token expansion. */
	private static Set<String> retailExpandedClasses(RetailClassRow row) {
		Set<String> classes = new TreeSet<>();
		for (String token : row.tokens()) {
			String mapped = TOKEN_MAP.get(token);
			if (mapped == null) {
				continue;
			}
			List<String> advanced = BASE_ADVANCED.get(token);
			if (advanced != null && row.minLevel() >= 10) {
				classes.addAll(advanced);
			} else {
				classes.add(mapped);
			}
		}
		return classes;
	}
}
