package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailLedgerRows;
import com.aionemu.gameserver.questEngine.retail.RetailNpcNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.retail.RetailQuestXmlTable;
import com.aionemu.gameserver.questEngine.retail.RetailQuestAiNameGroupsFixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原版元数据层等价门禁：对全部生产目录任务证明
 * "原版 quest.xml 行 → {@link RetailQuestMetadataCompiler} 的元数据" 与
 * quest-definition XML 元数据一致（除已登记的口径分歧）。
 * <p>
 * - 已知全局口径：name 轴采用原版 Qxxxx 约定（英文名为在库人工资产，无运行时消费方）；
 * - 已知登记轴：max-level 82 封顶（复用 {@code quest-start-metadata-retail-cap-exceptions.tsv}）；
 * - 其余任何轴差异都必须出现在 {@code /quest/retail-metadata-divergences.tsv}（逐任务逐轴登记）；
 * - 原版行缺失或含未解析符号名的任务必须登记（axis=RETAIL_MISSING / RETAIL_UNRESOLVED）。
 * <p>
 * 排查模式：{@code -Dretail.metadata.diffOut=<path>} 导出全部差异后失败，供生成登记表。
 * The retail metadata equivalence gate over the whole production catalog.
 */
class RetailMetadataEquivalenceGateTest {

	private static final String RESOURCE_PREFIX = "/aion/data/static_data/";
	private static final String RETAIL_QUEST_XML = RESOURCE_PREFIX + "quest/retail/quest.xml";
	private static final String NAME_IDS_XML = RESOURCE_PREFIX + "quest/retail/quest_name_string_ids.xml";
	private static final String NPC_DIR = RESOURCE_PREFIX + "npcs/";
	private static final String ITEM_DIR = RESOURCE_PREFIX + "items/item/";
	private static final String RANDOM_REWARDS = RESOURCE_PREFIX + "quest/legacy/quest_random_rewards.xml";
	private static final String CATALOG = RESOURCE_PREFIX + "quest/definitions/quest_definition_catalog.xml";
	private static final String CAP_EXCEPTIONS = "/quest/quest-start-metadata-retail-cap-exceptions.tsv";
	private static final String DIVERGENCES = "/quest/retail-metadata-divergences.tsv";

	/** 与生产 npc 模板文件清单一致（与 RetailQuestCatalogTest 相同）。 / NPC template files. */
	private static final List<String> NPC_FILES = List.of("npc_template_200000_216188.xml",
		"npc_template_216189_235748.xml", "npc_template_235749_247606.xml", "npc_template_247607_270057.xml",
		"npc_template_270058_286320.xml", "npc_template_286321_800030.xml", "npc_template_800031_834289.xml",
		"npc_template_834290_885645.xml");

	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static Map<String, Integer> randomRewardIds;
	private static Map<Integer, Integer> nameStringIds;
	private static Map<Integer, Set<String>> registered;
	private static Set<Integer> cappedAt82;
	private static Path dumpPath;

	record CatalogEntry(int id, String resource, String mode) {
	}

	@BeforeAll
	static void loadFixtures() throws Exception {
		retailTable = RetailQuestXmlTable.load(open(RETAIL_QUEST_XML));
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_FILES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll(ITEM_DIR, listXmlNames(ITEM_DIR)));
		randomRewardIds = loadRandomRewardIds();
		nameStringIds = loadNameIds();
		registered = loadDivergences();
		cappedAt82 = loadCapExceptions();
		String dump = System.getProperty("retail.metadata.diffOut");
		dumpPath = dump == null ? null : Path.of(dump);
	}

	@Test
	void retailMetadataMatchesProductionXml() throws Exception {
		List<CatalogEntry> catalog = loadCatalog();
		// XML 目录已收敛（退役任务由原版驱动拥有，不再有 XML）；本门禁只对拍 XML 侧任务。
		// The XML directory has converged; this gate compares the XML-owned quests only.
		assertEquals(6217, catalog.size() + RetiredQuestIds.all().size(),
			"XML-owned + retail-owned must cover the frozen universe");
		// 下限不再用魔法数（M4-b 3866 / P0c 系列推进后旧值必然失效，且与上一行恒等式重复）：
		// 精确覆盖由上一行的 6217 恒等式锁定——目录与保留清单任一侧被误删都会立刻红。
		// No magic floor: the 6217 identity above pins the exact XML-owned coverage, so an accidentally
		// emptied directory (or a mis-registered retirement) fails that assertion instead.
		Map<String, List<String>> diffs = new TreeMap<>();
		int cleanRetail = 0;
		int accounted = 0;
		int retailMissing = 0;
		for (CatalogEntry entry : catalog) {
			RetailQuestXmlTable.Entry row = retailTable.find(entry.id()).orElse(null);
			if (row == null) {
				retailMissing++;
				requireRegistered(entry.id(), "RETAIL_MISSING", diffs);
				continue;
			}
			RetailQuestMetadataCompiler.Outcome outcome = RetailQuestMetadataCompiler.compile(row, npcIndex,
				itemIndex, randomRewardIds, nameStringIds);
			if (!outcome.unresolved().isEmpty()) {
				if (!isRegistered(entry.id(), "RETAIL_UNRESOLVED")) {
					diffs.computeIfAbsent("RETAIL_UNRESOLVED", key -> new ArrayList<>())
						.add(entry.id() + "\t-\t" + outcome.unresolved());
				}
				continue;
			}
			QuestMetadata xml = xmlMetadata(entry);
			if (xml == null) {
				continue;
			}
			accounted++;
			cleanRetail += outcome.clean() ? 1 : 0;
			compare(entry.id(), xml, outcome.metadata(), diffs);
		}
		if (dumpPath != null) {
			writeDump(diffs);
		}
		List<String> failures = new ArrayList<>();
		diffs.forEach((axis, rows) -> failures.add(axis + " x" + rows.size() + " first=" + rows.get(0)));
		assertTrue(failures.isEmpty(), () -> "retail metadata divergences:\n" + String.join("\n", failures));
		assertEquals(catalog.size(), accounted + retailMissing,
			"every catalog quest must be retail-compiled or explicitly registered as missing");
	}

	// ---------------------------------------------------------------- 对比轴

	private static void compare(int questId, QuestMetadata xml, QuestMetadata retail,
			Map<String, List<String>> diffs) {
		check(questId, "displayNameId", xml.displayNameId(), retail.displayNameId(), diffs);
		check(questId, "minLevel", xml.minLevel(), retail.minLevel(), diffs);
		if (!(xml.maxLevel() == 82 && retail.maxLevel() == Integer.MAX_VALUE && cappedAt82.contains(questId))) {
			check(questId, "maxLevel", maxKey(xml.maxLevel()), maxKey(retail.maxLevel()), diffs);
		}
		check(questId, "races", xml.permittedRaces(), retail.permittedRaces(), diffs);
		check(questId, "category", xml.category(), retail.category(), diffs);
		check(questId, "repeat", policyKey(xml.repeatPolicy()), policyKey(retail.repeatPolicy()), diffs);
		check(questId, "prerequisites", xml.prerequisites(), retail.prerequisites(), diffs);
		check(questId, "startConditions", conditionsKey(xml.startConditions()),
			conditionsKey(retail.startConditions()), diffs);
		check(questId, "items", itemsKey(xml.itemRequirements()), itemsKey(retail.itemRequirements()), diffs);
		check(questId, "rewards", rewardsKey(xml.rewards()), rewardsKey(retail.rewards()), diffs);
		check(questId, "rewardGroups", xml.rewardGroups().size(), retail.rewardGroups().size(), diffs);
		check(questId, "extendedRewards", rewardsKey(xml.extendedRewards()), rewardsKey(retail.extendedRewards()),
			diffs);
		check(questId, "drops", dropsKey(xml.drops()), dropsKey(retail.drops()), diffs);
		check(questId, "classes", xml.permittedClasses(), retail.permittedClasses(), diffs);
		check(questId, "gender", xml.permittedGender(), retail.permittedGender(), diffs);
		check(questId, "rank", xml.rank(), retail.rank(), diffs);
		check(questId, "cannotShare", xml.cannotShare(), retail.cannotShare(), diffs);
		check(questId, "cannotGiveup", xml.cannotGiveup(), retail.cannotGiveup(), diffs);
		check(questId, "useClassReward", xml.useClassReward(), retail.useClassReward(), diffs);
		check(questId, "combineSkill", xml.combineSkill(), retail.combineSkill(), diffs);
		check(questId, "combineSkillPoint", xml.combineSkillPoint(), retail.combineSkillPoint(), diffs);
		check(questId, "timer", xml.timer(), retail.timer(), diffs);
		check(questId, "npcFactionId", xml.npcFactionId(), retail.npcFactionId(), diffs);
		check(questId, "targetType", xml.targetType(), retail.targetType(), diffs);
		check(questId, "inventoryItems", itemsKey(xml.inventoryItems()), itemsKey(retail.inventoryItems()), diffs);
		check(questId, "questWorkItems", itemsKey(xml.questWorkItems()), itemsKey(retail.questWorkItems()), diffs);
		check(questId, "bonuses", bonusesKey(xml.bonuses()), bonusesKey(retail.bonuses()), diffs);
		check(questId, "kills", killsKey(xml.kills()), killsKey(retail.kills()), diffs);
		check(questId, "classRewards", classRewardsKey(xml.classRewards()), classRewardsKey(retail.classRewards()),
			diffs);
	}

	private static void check(int questId, String axis, Object xml, Object retail,
			Map<String, List<String>> diffs) {
		if (!Objects.equals(xml, retail) && !isRegistered(questId, axis)) {
			diffs.computeIfAbsent(axis, key -> new ArrayList<>())
				.add(questId + "\t" + abbreviate(xml) + "\t" + abbreviate(retail));
		}
	}

	private static String abbreviate(Object value) {
		String text = String.valueOf(value);
		return text.length() > 220 ? text.substring(0, 220) + "…" : text;
	}

	private static String maxKey(int maxLevel) {
		// 生产/原版均以 0/998/999 与 2147483647 表达无上限（与既有 retail 门禁同口径）。
		// Production and retail both express "unlimited" as 0/998/999 or Integer.MAX_VALUE.
		return maxLevel >= Integer.MAX_VALUE || maxLevel == 998 || maxLevel == 999 || maxLevel == 0
			? "UNLIMITED" : Integer.toString(maxLevel);
	}

	private static String policyKey(RepeatPolicy policy) {
		return policy.maxRepeatCount() + "/" + policy.rewardRepeatCount() + "/" + policy.cooldownSeconds() + "/"
			+ policy.daily() + "/" + policy.weekly();
	}

	private static String conditionsKey(List<QuestStartCondition> conditions) {
		List<String> keys = new ArrayList<>();
		for (QuestStartCondition condition : conditions) {
			keys.add(condition.type() + ":" + condition.questId() + ":" + condition.rewardMode());
		}
		Collections.sort(keys);
		return keys.toString();
	}

	private static String itemsKey(List<QuestItemRequirement> items) {
		List<String> keys = new ArrayList<>();
		for (QuestItemRequirement item : items) {
			keys.add(item.itemId() + "x" + item.count());
		}
		Collections.sort(keys);
		return keys.toString();
	}

	private static String rewardsKey(List<QuestReward> rewards) {
		List<String> keys = new ArrayList<>();
		for (QuestReward reward : rewards) {
			keys.add(reward.kind() + ":" + reward.id() + ":" + reward.amount());
		}
		Collections.sort(keys);
		return keys.toString();
	}

	/**
	 * 掉落键：npc 轴按同名族闭包归一（同一客户端显示名的 npc_id 视为同一只怪）。
	 * 两侧同口径展开，避免"原版驱动的掉落绑在别名 id 上"被误判成差异。
	 * Drop keys normalize the npc axis through the display-name closure on both sides.
	 */
	private static String dropsKey(List<QuestDrop> drops) {
		Set<String> keys = new TreeSet<>();
		for (QuestDrop drop : drops) {
			for (int npcId : npcIndex.withDisplayNameVariants(Set.of(drop.npcId()))) {
				keys.add(npcId + ":" + drop.itemId() + ":" + drop.chance() + ":" + drop.eachMember() + ":"
					+ drop.collectingStep() + ":" + drop.scope());
			}
		}
		return keys.toString();
	}

	private static String bonusesKey(List<QuestBonus> bonuses) {
		List<String> keys = new ArrayList<>();
		for (QuestBonus bonus : bonuses) {
			keys.add(bonus.type() + ":" + bonus.level() + ":" + bonus.skill());
		}
		Collections.sort(keys);
		return keys.toString();
	}

	private static String killsKey(List<QuestKill> kills) {
		List<String> keys = new ArrayList<>();
		for (QuestKill kill : kills) {
			keys.add(kill.sequence() + "=" + new TreeSet<>(kill.npcIds()));
		}
		Collections.sort(keys);
		return keys.toString();
	}

	private static String classRewardsKey(Map<String, List<QuestReward>> classRewards) {
		Map<String, String> keys = new TreeMap<>();
		classRewards.forEach((key, value) -> keys.put(key, rewardsKey(value)));
		return keys.toString();
	}

	// ---------------------------------------------------------------- 装载

	private static List<CatalogEntry> loadCatalog() throws Exception {
		Document document = parse(open(CATALOG));
		List<CatalogEntry> entries = new ArrayList<>();
		var nodes = document.getDocumentElement().getElementsByTagName("definition");
		for (int index = 0; index < nodes.getLength(); index++) {
			Element element = (Element) nodes.item(index);
			entries.add(new CatalogEntry(Integer.parseInt(element.getAttribute("id")),
				element.getAttribute("resource"), element.getAttribute("mode")));
		}
		// 已退役任务的 XML 不再进仓（历史内容在 git 里可回溯）：XML↔原版 的元数据对拍只覆盖仍由 XML 拥有的任务。
		// 退役集合改由保留清单冻结（RetailOwnershipGateTest），本门禁不再读取冻结副本。
		// Retired quests no longer ship their XML, so the XML-vs-retail comparison covers XML-owned quests only.
		return entries;
	}

	private static QuestMetadata xmlMetadata(CatalogEntry entry) throws IOException {
		try (InputStream input = open("/" + entry.resource())) {
			return QuestDefinitionXmlCompiler.parse(input).metadata();
		}
	}

	private static Map<String, Integer> loadRandomRewardIds() throws Exception {
		Document document = parse(open(RANDOM_REWARDS));
		Map<String, Integer> ids = new HashMap<>();
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			Element element = (Element) nodes.item(index);
			ids.put(text(element, "name"), Integer.parseInt(text(element, "id")));
		}
		return ids;
	}

	private static Map<Integer, Integer> loadNameIds() throws IOException {
		Map<Integer, Integer> ids = new HashMap<>();
		for (Element row : RetailLedgerRows.rows(NAME_IDS_XML, "name_string_id")) {
			ids.put(Integer.parseInt(RetailLedgerRows.cell(row, "key").substring("STR_QUEST_NAME_Q".length())),
				Integer.parseInt(RetailLedgerRows.cell(row, "string_id")));
		}
		return ids;
	}

	private static Map<Integer, Set<String>> loadDivergences() throws IOException {
		Map<Integer, Set<String>> registered = new HashMap<>();
		if (RetailMetadataEquivalenceGateTest.class.getResource(DIVERGENCES) == null) {
			return registered;
		}
		for (String line : lines(open(DIVERGENCES))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			registered.computeIfAbsent(Integer.parseInt(parts[0]), key -> new HashSet<>()).add(parts[1]);
		}
		return registered;
	}

	private static Set<Integer> loadCapExceptions() throws IOException {
		Set<Integer> ids = new HashSet<>();
		for (String line : lines(open(CAP_EXCEPTIONS))) {
			if (line.startsWith("#") || line.isBlank() || line.startsWith("quest_id")) {
				continue;
			}
			ids.add(Integer.parseInt(line.split("\t")[0]));
		}
		return ids;
	}

	private static boolean isRegistered(int questId, String axis) {
		Set<String> axes = registered.get(questId);
		return axes != null && axes.contains(axis);
	}

	private static void requireRegistered(int questId, String axis, Map<String, List<String>> diffs) {
		if (!isRegistered(questId, axis)) {
			diffs.computeIfAbsent(axis, key -> new ArrayList<>()).add(questId + "\t-\t-");
		}
	}

	private static void writeDump(Map<String, List<String>> diffs) throws IOException {
		try (var writer = Files.newBufferedWriter(dumpPath, StandardCharsets.UTF_8)) {
			writer.write("# axis\tquest_id\txml\tretail\n");
			for (Map.Entry<String, List<String>> entry : diffs.entrySet()) {
				for (String row : entry.getValue()) {
					writer.write(entry.getKey() + "\t" + row + "\n");
				}
			}
		}
	}

	private static Document parse(InputStream input) throws IOException {
		try (input) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			return factory.newDocumentBuilder().parse(input);
		} catch (Exception e) {
			throw new IOException("xml parse failed", e);
		}
	}

	private static String text(Element parent, String tag) {
		var nodes = parent.getElementsByTagName(tag);
		return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
	}

	private static InputStream open(String resource) throws IOException {
		InputStream input = RetailMetadataEquivalenceGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws IOException {
		List<InputStream> inputs = new ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	/** 列举 classpath 目录下的 XML 文件名（测试在文件协议下运行）。 / Lists XML names in a classpath dir. */
	private static List<String> listXmlNames(String dir) throws IOException {
		var url = RetailMetadataEquivalenceGateTest.class.getResource(dir);
		assertNotNull(url, "missing dir " + dir);
		if (!"file".equals(url.getProtocol())) {
			throw new IOException("unsupported protocol " + url);
		}
		File[] files;
		try {
			files = new File(url.toURI()).listFiles((directory, name) -> name.endsWith(".xml"));
		} catch (URISyntaxException e) {
			throw new IOException("bad dir uri " + url, e);
		}
		assertNotNull(files, "unreadable dir " + dir);
		List<String> names = new ArrayList<>();
		for (File file : files) {
			names.add(file.getName());
		}
		Collections.sort(names);
		return names;
	}

	private static List<String> lines(InputStream input) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}
}
