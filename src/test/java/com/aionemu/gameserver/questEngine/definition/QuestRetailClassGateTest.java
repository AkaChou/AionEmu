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
import java.util.TreeSet;

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

	record RetailClassRow(int minLevel, Set<String> tokens) {
	}

	/** 门禁所需的职业元数据视图。 */
	record ClassMeta(int minLevel, Set<String> classes) {
	}

	@BeforeAll
	static void loadFixtures() throws IOException {
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
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(
			QuestRetailClassGateTest.class.getClassLoader());
		for (CompiledQuestDefinition compiled : catalog.all()) {
			QuestMetadata meta = compiled.definition().metadata();
			production.put(compiled.id(), new ClassMeta(meta.minLevel(), meta.permittedClasses()));
		}
		assertFalse(production.isEmpty(), "production catalog must not be empty");
	}

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
				problems.add("contract quest " + qid + " missing from production catalog");
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
			if (productionAliveClasses(production.get(qid)).equals(Set.of("WILDCARD"))
				&& !EVIDENCE_BLOCKED.contains(qid) && !INTENTIONAL_WEAPON_ADAPTATION.contains(qid)) {
				problems.add("quest " + qid + " is a wildcard but retail limits classes to "
					+ retailAliveClasses(entry.getValue()));
			}
		}
		assertTrue(problems.isEmpty(), () -> "unlisted wildcard quests: " + problems);
	}

	/**
	 * 修复代表任务的显式职业集合断言：导师任务族、Kaliga 武器收集族、Dark Poeta/
	 * 守护者英雄分组、机甲星使命、枪星导师与补新职业任务。
	 */
	@Test
	void repairedQuestsExposeExactlyTheirRetailClassSets() {
		Map<Integer, Set<String>> expected = Map.ofEntries(
			Map.entry(3928, Set.of("CLERIC")),
			Map.entry(3929, Set.of("CHANTER")),
			Map.entry(4922, Set.of("GLADIATOR")),
			Map.entry(4926, Set.of("SORCERER")),
			Map.entry(4927, Set.of("SPIRIT_MASTER")),
			Map.entry(4928, Set.of("CLERIC")),
			Map.entry(4929, Set.of("CHANTER")),
			Map.entry(18618, Set.of("ASSASSIN", "GLADIATOR", "RANGER", "TEMPLAR")),
			Map.entry(18621, Set.of("GLADIATOR")),
			Map.entry(18625, Set.of("SORCERER", "SPIRIT_MASTER")),
			Map.entry(28643, Set.of("GUNSLINGER")),
			Map.entry(28648, Set.of("AETHERTECH")),
			Map.entry(80219, Set.of("ASSASSIN", "GLADIATOR", "GUNSLINGER", "RANGER", "TEMPLAR")),
			Map.entry(80220, Set.of("AETHERTECH", "CHANTER", "CLERIC", "SONGWEAVER",
				"SORCERER", "SPIRIT_MASTER")),
			Map.entry(80317, Set.of("CLERIC", "SORCERER", "SPIRIT_MASTER")),
			Map.entry(18614, Set.of("GLADIATOR")),
			Map.entry(28630, Set.of("ASSASSIN", "GLADIATOR", "RANGER")),
			Map.entry(19074, Set.of("GUNSLINGER")),
			Map.entry(14031, Set.of("AETHERTECH")),
			Map.entry(24031, Set.of("AETHERTECH")),
			Map.entry(1466, Set.of("GLADIATOR", "TEMPLAR", "ASSASSIN", "RANGER", "SORCERER",
				"SPIRIT_MASTER", "CLERIC", "CHANTER", "GUNSLINGER", "AETHERTECH", "SONGWEAVER")),
			Map.entry(11076, Set.of("GLADIATOR", "TEMPLAR", "ASSASSIN", "RANGER", "SORCERER",
				"SPIRIT_MASTER", "CLERIC", "CHANTER", "GUNSLINGER", "AETHERTECH",
				"SONGWEAVER")));
		for (Map.Entry<Integer, Set<String>> entry : expected.entrySet()) {
			Set<String> actual = productionAliveClasses(production.get(entry.getKey()));
			assertEquals(entry.getValue(), actual, "quest " + entry.getKey()
				+ " must expose exactly its retail acceptable class set");
		}
	}
}
