package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.definition.QuestXmlFixtures;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleUseItem 真端驱动门禁（P3b）。判据 = 真端表 + 客户端契约：用物品接取
 * （{@code UseItem} → 接取确认窗）、无目标对话接受/拒绝/关窗（{@code QuestDialog 1002/1003/1008}）、
 * 报告 NPC SELECT5（QUEST_SELECT + 1009 交付）、npc-complete 确认 8..23；
 * 领奖投影 = 客户端任务书末行行号（QE-051）。
 * <p>
 * 断言四件事：家族规模冻结 / 规范语义不变量 / 漂移登记同步 / 冻结 IR 指纹 + 保留清单一致。
 * Retail-semantics gate for the SimpleUseItem family (item-use accept + targetless dialogs).
 */
class RetailSimpleUseItemGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String ITEM_DIR = "/aion/data/static_data/items/item/";
	private static final String CATALOG = "/aion/data/static_data/quest_definition/quest_definition_catalog.xml";
	private static final String RETENTION = "/aion/data/static_data/quest_retail/retail-xml-retention.tsv";
	/** 与历史 XML 的漂移登记。 / Drift registry versus the legacy XML. */
	private static final String DRIFT_REGISTRY = "/quest/retail-simple-use-item-drift.tsv";
	/** 冻结 IR 指纹。 / Frozen IR fingerprints. */
	private static final String FINGERPRINTS = "/quest/retail-simple-use-item-ir-fingerprints.tsv";
	/** 冻结的家族规模。 / Frozen family size. */
	private static final int FROZEN_FAMILY_SIZE = 104;
	/** 可驱动数量下限。 / Floor for retail-drivable quests. */
	private static final int ACCEPTED_FLOOR = 104;
	/** 已退役子集冻结规模（P3b 采纳子集退役）。 / Retired subset frozen size. */
	private static final int FROZEN_RETIRED_SIZE = 104;
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailSimpleUseItemTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static RetailClientSummaryRows clientSummaryRows;
	private static RetailClientUseItemReport clientReportModes;
	private static RetailClientDialogExits clientDialogExits;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Set<Integer> familyIds;
	private static Map<Integer, String> driftRegistry;
	private static Map<Integer, String> fingerprints;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest_retail/Quest_SimpleUseItem.xml")) {
			table = RetailSimpleUseItemTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		clientSummaryRows = RetailClientSummaryRows.defaultSummaryRows();
		clientReportModes = RetailClientUseItemReport.defaultReport();
		clientDialogExits = RetailClientDialogExits.defaultExits();
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll(ITEM_DIR, listXmlNames(ITEM_DIR)));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		familyIds = familyIds();
		driftRegistry = driftRegistry();
		fingerprints = fingerprints();
	}

	@Test
	void familyScopeIsFrozen() {
		assertTrue(familyIds.size() == FROZEN_FAMILY_SIZE,
			() -> "SimpleUseItem 家族规模漂移：期望 " + FROZEN_FAMILY_SIZE + " 实际 " + familyIds.size());
	}

	/** 可驱动集合必须满足规范语义（用物品接取 + 无目标对话 + 报告/完成）。 / Canonical semantics. */
	@Test
	void acceptedDefinitionsCarryCanonicalSemantics() {
		List<String> problems = new ArrayList<>();
		int accepted = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				continue;
			}
			accepted++;
			inspect(questId, row, problems);
		}
		int drivable = accepted;
		assertTrue(problems.isEmpty(), () -> "SimpleUseItem 语义缺口："
			+ problems.stream().limit(20).toList());
		assertTrue(drivable >= ACCEPTED_FLOOR, () -> "SimpleUseItem 可驱动数量回退："
			+ drivable + " < " + ACCEPTED_FLOOR);
	}

	/** 未退役 XML 的漂移登记与逐任务分类一致。 / Drift registry covers retained XML. */
	@Test
	void driftVersusLegacyXmlIsRegistered() throws Exception {
		Map<Integer, String> classification = classify();
		List<String> problems = new ArrayList<>();
		for (int questId : new TreeSet<>(driftRegistry.keySet())) {
			if (RetiredQuestIds.contains(questId)) {
				problems.add(questId + ": 已退役行不应由 XML 漂移登记拥有");
				continue;
			}
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
		String dump = System.getProperty("retail.useItem.equivOut");
		if (dump != null) {
			Files.writeString(Path.of(dump), dumpText(classification));
		}
		assertTrue(driftRegistry.size() == FROZEN_FAMILY_SIZE - FROZEN_RETIRED_SIZE,
			() -> "漂移登记行数漂移：" + driftRegistry.size());
		assertTrue(problems.isEmpty(), () -> "SimpleUseItem 漂移登记失同步："
			+ problems.stream().limit(20).toList());
	}

	/** 冻结 IR 指纹：XML 删除后仍能发现静默漂移。 / Frozen fingerprints, the post-deletion drift guard. */
	@Test
	void frozenFingerprintsCoverExactlyTheRetiredQuests() throws Exception {
		String freezeOut = System.getProperty("retail.useItem.fpOut");
		Map<Integer, String> actual = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				continue;
			}
			actual.put(questId, RetailIrFingerprint.fingerprint(row.outcome().definition().definition()));
		}
		if (freezeOut != null) {
			StringBuilder text = new StringBuilder("# SimpleUseItem 冻结 IR 指纹（XML 删除后继续守等价证据）\n"
				+ "# quest_id\tretail_fingerprint\tnodes\ttransitions\n");
			for (Map.Entry<Integer, String> entry : actual.entrySet()) {
				if (!RetiredQuestIds.contains(entry.getKey())) {
					continue;
				}
				QuestDefinition definition = retailRow(entry.getKey()).outcome().definition().definition();
				text.append(entry.getKey()).append('\t').append(entry.getValue()).append('\t')
					.append(definition.nodes().size()).append('\t').append(definition.transitions().size())
					.append('\n');
			}
			Files.writeString(Path.of(freezeOut), text.toString());
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
		assertTrue(problems.isEmpty(), () -> "SimpleUseItem 冻结指纹失同步："
			+ problems.stream().limit(20).toList());
	}

	/** 保留清单必须把可驱动集合标为 RETAIL_TABLE。 / Manifest agreement with the driver. */
	@Test
	void retentionManifestMatchesDriverOwnership() throws Exception {
		Map<Integer, String> owners = retentionOwners();
		List<String> problems = new ArrayList<>();
		int owned = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			String owner = owners.get(questId);
			if (row.outcome().accepted() && "RETAIL_TABLE".equals(owner)) {
				owned++;
				continue;
			}
			if (row.outcome().accepted() && "XML_RETENTION".equals(owner) && !RetiredQuestIds.contains(questId)) {
				problems.add(questId + ": 可驱动却被留在 XML");
			}
		}
		int ownedSnapshot = owned;
		assertTrue(ownedSnapshot >= FROZEN_RETIRED_SIZE,
			() -> "真端驱动拥有的任务数量回退：" + ownedSnapshot + " < " + FROZEN_RETIRED_SIZE);
		assertTrue(problems.isEmpty(), () -> "保留清单与真端驱动不一致："
			+ problems.stream().limit(20).toList());
	}

	// ------------------------------------------------------------------ 语义不变量

	private static void inspect(int questId, RetailRow row, List<String> problems) {
		QuestDefinition definition = row.outcome().definition().definition();
		if (definition.progressLayout().fields().size() != 1
			|| !"var0".equals(definition.progressLayout().fields().get(0).name())) {
			problems.add(questId + ": 进度域应为单一 var0");
		}
		if (!hasUseItemAccept(definition)) {
			problems.add(questId + ": 缺用物品接取路由");
		}
		if (!hasTargetlessDialog(definition, QuestDialogAction.QUEST_ACCEPT_1.id(), QuestStatus.START)) {
			problems.add(questId + ": 缺无目标接受路由（1002）");
		}
		if (!hasTargetlessDialog(definition, QuestDialogAction.QUEST_REFUSE_1.id(), QuestStatus.NONE)
			|| !hasTargetlessDialog(definition, QuestDialogAction.FINISH_DIALOG.id(), QuestStatus.NONE)) {
			problems.add(questId + ": 缺无目标拒绝/关窗路由（1003/1008）");
		}
		for (int rewardNpc : row.rewardNpcs()) {
			if (!hasReport(definition, rewardNpc)) {
				problems.add(questId + ": 缺报告路由 npc=" + rewardNpc);
			}
			if (!hasComplete(definition, rewardNpc)) {
				problems.add(questId + ": 缺完成分支 npc=" + rewardNpc);
			}
		}
	}

	/** 用物品接取：UseItem 事件弹接取确认窗。 / Item-use accept pops the ask window. */
	private static boolean hasUseItemAccept(QuestDefinition definition) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.UseItem
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())));
	}

	/** 无目标对话路由（QuestDialog 事件）。 / A targetless dialog route. */
	private static boolean hasTargetlessDialog(QuestDefinition definition, int dialogId, QuestStatus target) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.QuestDialog dialog
				&& dialog.dialogId() == dialogId
				&& statusOf(definition, transition.targetNode()) == target);
	}

	/**
	 * P0-2 规范形交付：QUEST_SELECT 直翻 REWARD + 分档查表奖励窗（QE-028；本族单档由
	 * completeFlow 的多档拒绝不变量保证，档位窗查表 0 档）——SELECT5 报告页与 1009 中转删除。
	 * P0-2 canonical delivery: QUEST_SELECT flips REWARD with the tiered reward window (QE-028;
	 * the single tier is guaranteed by the completeFlow multi-tier rejection invariant) — the
	 * SELECT5 page and 1009 hop are gone.
	 */
	private static boolean hasReport(QuestDefinition definition, int npcId) {
		int rewardWindow = QuestDialogPage.rewardWindowForTier(0).orElseThrow().id();
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.QUEST_SELECT)
				&& statusOf(definition, transition.targetNode()) == QuestStatus.REWARD
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(rewardWindow)));
	}

	private static boolean hasComplete(QuestDefinition definition, int npcId) {
		int first = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
		int last = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
		long count = definition.transitions().stream().filter(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() != null
				&& talk.dialogId() >= first && talk.dialogId() <= last
				&& statusOf(definition, transition.targetNode()) == QuestStatus.COMPLETE).count();
		return count == last - first + 1;
	}

	private static QuestStatus statusOf(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(node -> node.label().equals(label))
			.map(node -> node.projection().status()).findFirst().orElse(null);
	}

	private static boolean isAction(QuestEvent.TalkToNpc talk, QuestDialogAction action) {
		return talk.dialogId() != null && talk.dialogId() == action.id();
	}

	// ------------------------------------------------------------------ 分类与装载

	/** 未退役任务分类（真端合成定义 vs XML）。 / Retained-XML classifications. */
	private static Map<Integer, String> classify() throws Exception {
		Map<Integer, String> classification = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			if (RetiredQuestIds.contains(questId)) {
				continue;
			}
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				classification.put(questId, "REJECTED:" + row.outcome().rejectionCode());
				continue;
			}
			CompiledQuestDefinition fromXml = QuestXmlFixtures.compile(questId);
			if (fromXml == null) {
				classification.put(questId, "NO_XML");
				continue;
			}
			String retailText = RetailIrFingerprint.canonicalText(row.outcome().definition().definition());
			String xmlText = RetailIrFingerprint.canonicalText(fromXml.definition());
			classification.put(questId, retailText.equals(xmlText) ? "EQUIVALENT"
				: "DIFF:" + axisText(retailText, xmlText));
		}
		return classification;
	}

	private static String axisText(String retailText, String xmlText) {
		Set<String> retail = new HashSet<>(List.of(retailText.split("\n")));
		Set<String> xml = new HashSet<>(List.of(xmlText.split("\n")));
		Set<String> axes = new TreeSet<>();
		for (String line : xml) {
			if (!retail.contains(line)) {
				axes.add(axisOf(line, false));
			}
		}
		for (String line : retail) {
			if (!xml.contains(line)) {
				axes.add(axisOf(line, true));
			}
		}
		return String.join(" ", axes);
	}

	private static String axisOf(String line, boolean retailOnly) {
		String prefix = retailOnly ? "RETAIL_EXTRA:" : "XML_EXTRA:";
		if (line.contains("BitField[name=var")) {
			return prefix + "VAR_FIELD";
		}
		if (line.startsWith("N\t")) {
			return prefix + "NODE";
		}
		if (line.contains("UseItem")) {
			return prefix + "USE_ITEM";
		}
		for (String token : List.of("1002", "1003", "1008", "1009", "31", "-1", "10000", "39", "20002")) {
			if (line.contains("dialogId=" + token)) {
				return prefix + "DIALOG_" + token;
			}
		}
		if (line.contains("GrantReward")) {
			return prefix + "GRANT";
		}
		if (line.startsWith("T\t")) {
			return prefix + "ROUTE";
		}
		return prefix + "OTHER";
	}

	private static String dumpText(Map<Integer, String> classification) {
		Map<String, Integer> histogram = new TreeMap<>();
		classification.values().removeIf(Objects::isNull);
		classification.values().forEach(kind -> histogram.merge(kind, 1, Integer::sum));
		StringBuilder text = new StringBuilder("# SimpleUseItem 漂移登记（禁止手改；重算：-Dretail.useItem.equivOut=<path>）\n");
		histogram.forEach((kind, count) -> text.append("# ").append(kind).append('\t').append(count).append('\n'));
		classification.forEach((questId, kind) -> text.append(questId).append('\t').append(kind).append('\n'));
		return text.toString();
	}

	/** 单任务真端编译上下文。 / Retail compile context for one quest row. */
	private record RetailRow(RetailSimpleUseItemTable.Entry entry, QuestMetadata metadata, int acquiredNpc,
			Set<Integer> rewardNpcs, RetailSimpleUseItemDefinitionCompiler.Outcome outcome) {
	}

	private static RetailRow retailRow(int questId) {
		RetailSimpleUseItemTable.Entry entry = table.find(questId).orElseThrow();
		var metadata = RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(), npcIndex,
			itemIndex, randomRewards, nameIds);
		Set<Integer> reward = npcIndex.resolvePartyName(entry.rewardNpc() == null ? "" : entry.rewardNpc());
		var outcome = RetailSimpleUseItemDefinitionCompiler.compile(entry, itemIndex, npcIndex, metadata,
			clientSummaryRows, clientReportModes, clientDialogExits);
		return new RetailRow(entry, metadata.metadata(), -1, reward, outcome);
	}

	/** 真端表 ∩ 生产宇宙。 / Retail table intersected with the production universe. */
	private static Set<Integer> familyIds() throws Exception {
		String text = new String(open(CATALOG).readAllBytes(), StandardCharsets.UTF_8);
		Set<Integer> catalog = new HashSet<>();
		var matcher = java.util.regex.Pattern.compile("<definition id=\"(\\d+)\"").matcher(text);
		while (matcher.find()) {
			catalog.add(Integer.parseInt(matcher.group(1)));
		}
		catalog.addAll(RetiredQuestIds.all());
		Set<Integer> ids = new TreeSet<>(table.questIds());
		ids.retainAll(catalog);
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
		InputStream input = RetailSimpleUseItemGateTest.class.getResourceAsStream(resource);
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
		var url = RetailSimpleUseItemGateTest.class.getResource(dir);
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
