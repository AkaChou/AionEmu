package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SimpleSerialHunt 真端驱动门禁（P3）。判据 = 客户端链式阶段契约 + 真端表 + 真端元数据：
 * {@code quest_monster.csv} 的链式 SECTION 门控决定"乱序击杀不计数"，因此合成定义必须是
 * 串行阶梯（节点 = 链式前缀组合，击杀边只推进首个未满段），而不是并行网格。
 * <p>
 * 断言四件事：家族规模冻结 / 串行语义不变量 / 漂移登记同步 / 冻结 IR 指纹 + 保留清单一致。
 * Retail-semantics gate for the SimpleSerialHunt family (serial ladder per the client's chained gates).
 */
class RetailSimpleSerialHuntGateTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String ITEM_DIR = "/aion/data/static_data/items/item/";
	private static final String CATALOG = "/aion/data/static_data/quest/definitions/quest_definition_catalog.xml";
	private static final String RETENTION = "/aion/data/static_data/quest/retail/retail-xml-retention.tsv";
	/** 与历史 XML 的漂移登记。 / Drift registry versus the legacy XML. */
	private static final String DRIFT_REGISTRY = "/quest/retail-simple-serial-hunt-drift.tsv";
	/** 冻结 IR 指纹。 / Frozen IR fingerprints. */
	private static final String FINGERPRINTS = "/quest/retail-simple-serial-hunt-ir-fingerprints.tsv";
	/** 冻结的家族规模。 / Frozen family size. */
	private static final int FROZEN_FAMILY_SIZE = 10;
	/** 可驱动数量下限。 / Floor for retail-drivable quests. */
	private static final int ACCEPTED_FLOOR = 10;
	/** 已退役子集冻结规模（P3 全族退役）。 / Retired subset frozen size (whole family retired). */
	private static final int FROZEN_RETIRED_SIZE = 10;
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailSimpleSerialHuntTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static RetailClientHuntStages huntStages;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Set<Integer> familyIds;
	private static Map<Integer, String> driftRegistry;
	private static Map<Integer, String> fingerprints;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest/retail/Quest_SimpleSerialHunt.xml")) {
			table = RetailSimpleSerialHuntTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest/retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		huntStages = RetailClientHuntStages.defaultHuntStages();
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
			() -> "SimpleSerialHunt 家族规模漂移：期望 " + FROZEN_FAMILY_SIZE + " 实际 " + familyIds.size());
	}

	/** 可驱动集合必须满足客户端链式阶段契约。 / Retail serial semantics of every drivable row. */
	@Test
	void acceptedDefinitionsCarrySerialSemantics() {
		List<String> problems = new ArrayList<>();
		int accepted = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				problems.add(questId + ": 未按串行阶梯合成（" + row.outcome().rejectionCode() + "）");
				continue;
			}
			accepted++;
			inspect(questId, row, problems);
		}
		int drivable = accepted;
		assertTrue(problems.isEmpty(), () -> "SimpleSerialHunt 串行语义缺口："
			+ problems.stream().limit(20).toList());
		assertTrue(drivable >= ACCEPTED_FLOOR, () -> "SimpleSerialHunt 可驱动数量回退："
			+ drivable + " < " + ACCEPTED_FLOOR);
	}

	/** 漂移登记与逐任务分类一致（已退役行冻结）。 / Drift registry stays in sync. */
	@Test
	void driftVersusLegacyXmlIsRegistered() throws Exception {
		Map<Integer, String> classification = classify();
		List<String> problems = new ArrayList<>();
		for (int questId : new TreeSet<>(RetiredQuestIds.all())) {
			if (familyIds.contains(questId) && !driftRegistry.containsKey(questId)) {
				problems.add(questId + ": 已退役但没有漂移登记行（冻结证据缺失）");
			}
		}
		for (int questId : new TreeSet<>(driftRegistry.keySet())) {
			if (RetiredQuestIds.contains(questId)) {
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
		String dump = System.getProperty("retail.serialHunt.equivOut");
		if (dump != null) {
			Files.writeString(Path.of(dump), dumpText(classification));
		}
		assertTrue(driftRegistry.size() == FROZEN_FAMILY_SIZE,
			() -> "漂移登记行数漂移：" + driftRegistry.size());
		assertTrue(problems.isEmpty(), () -> "SimpleSerialHunt 漂移登记失同步："
			+ problems.stream().limit(20).toList());
	}

	/** 冻结 IR 指纹：XML 删除后仍能发现静默漂移。 / Frozen fingerprints, the post-deletion drift guard. */
	@Test
	void frozenFingerprintsCoverExactlyTheRetiredQuests() throws Exception {
		String freezeOut = System.getProperty("retail.serialHunt.fpOut");
		Map<Integer, String> actual = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				continue;
			}
			actual.put(questId, RetailIrFingerprint.fingerprint(row.outcome().definition().definition()));
		}
		if (freezeOut != null) {
			StringBuilder text = new StringBuilder("# SimpleSerialHunt 冻结 IR 指纹（XML 删除后继续守等价证据）\n"
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
		assertTrue(problems.isEmpty(), () -> "SimpleSerialHunt 冻结指纹失同步："
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
		List<RetailClientHuntStages.Stage> stages = huntStages.stages(questId);
		// talk_npc1 = 真端简报步骤（接取置 SECTION_5=1，简报 NPC 的 QUEST_SELECT 一步清 0——P0-2
		// 规范形删除 select2 页链，"见中间人才开计数"语义由目标投影承担）。
		// talk_npc1 is the retail briefing step: accept raises SECTION_5, the briefing NPC's
		// QUEST_SELECT clears it in one step (P0-2 canonical: the select2 chain goes, the
		// must-visit-middleman semantics live in the target projection).
		String talkNpc = row.entry().talkNpc();
		boolean briefing = talkNpc != null && !talkNpc.isBlank();
		// 进度域：每段一个 6 位字段（+ 简报标志位）。 / One 6-bit field per stage, plus the briefing flag.
		int expectedFields = stages.size() + (briefing ? 1 : 0);
		if (definition.progressLayout().fields().size() != expectedFields) {
			problems.add(questId + ": 进度域数量应为 " + expectedFields
				+ "，实际 " + definition.progressLayout().fields().size());
		}
		// 节点：链式前缀组合数 + unaccepted + reward + complete（+ 简报的 started/briefed 对中的 briefed）。
		int expectedCombos = 1;
		for (RetailClientHuntStages.Stage stage : stages) {
			expectedCombos += stage.count();
		}
		int expectedNodes = expectedCombos + 3 + (briefing ? 1 : 0);
		if (definition.nodes().size() != expectedNodes) {
			problems.add(questId + ": 节点数应为 " + expectedNodes + "（链式组合 "
				+ expectedCombos + " + 3），实际 " + definition.nodes().size());
		}
		if (briefing) {
			Map<String, Integer> started = projectionOf(definition, "started");
			Map<String, Integer> briefed = projectionOf(definition, "briefed");
			if (started == null || started.getOrDefault("var5", -1) != 1) {
				problems.add(questId + ": started 必须置 SECTION_5=1（简报未读）");
			}
			if (briefed == null || briefed.getOrDefault("var5", -1) != 0) {
				problems.add(questId + ": briefed 必须清 SECTION_5=0");
			}
			boolean clears = definition.transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					&& "started".equals(transition.sourceNode()) && "briefed".equals(transition.targetNode())
					&& transition.afterCommit().contains(new com.aionemu.gameserver.questEngine.definition.AfterCommitAction.CloseDialog()));
			if (!clears) {
				problems.add(questId + ": 缺 talk_npc1=" + talkNpc + " 的 QUEST_SELECT 一步简报清除路由");
			}
		} else if (definition.progressLayout().field("var5") != null) {
			problems.add(questId + ": 无 talk_npc1 的行不得声明简报标志位 var5");
		}
		Map<Integer, Integer> npcToStage = new HashMap<>();
		for (RetailClientHuntStages.Stage stage : stages) {
			for (int npcId : stage.npcIds()) {
				npcToStage.put(npcId, stage.stage() - 1);
			}
		}
		long badEdges = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
			.filter(transition -> !advanceIsChained(definition, stages, npcToStage, transition))
			.count();
		if (badEdges > 0) {
			problems.add(questId + ": " + badEdges + " 条击杀边不是链式推进（乱序计数）");
		}
		if (!hasAccept(definition, row.acquiredNpc())) {
			problems.add(questId + ": 缺接取路由 npc=" + row.acquiredNpc());
		}
		if (!hasReport(definition, row.rewardNpc(), row.metadata())) {
			problems.add(questId + ": 缺报告路由 npc=" + row.rewardNpc());
		}
		if (!hasComplete(definition, row.rewardNpc())) {
			problems.add(questId + ": 缺完成分支 npc=" + row.rewardNpc());
		}
	}

	/**
	 * 击杀边是否为链式推进：前序段全满、本段计数未满，且目标恰好在本段 +1（乱序不计数）。
	 * Whether the edge advances the chained ladder: earlier stages full, this stage not yet full,
	 * and the target raises exactly this stage's counter by one.
	 */
	private static boolean advanceIsChained(QuestDefinition definition, List<RetailClientHuntStages.Stage> stages,
			Map<Integer, Integer> npcToStage, com.aionemu.gameserver.questEngine.definition.QuestTransition transition) {
		if (!(transition.event() instanceof QuestEvent.KillNpc kill)) {
			return false;
		}
		int stageIndex = npcToStage.getOrDefault(kill.npcId(), -1);
		if (stageIndex < 0 || transition.sourceNode() == null || transition.targetNode() == null) {
			return false;
		}
		var source = projectionOf(definition, transition.sourceNode());
		var target = projectionOf(definition, transition.targetNode());
		if (source == null || target == null) {
			return false;
		}
		for (int index = 0; index < stageIndex; index++) {
			if (source.getOrDefault("var" + index, -1) != stages.get(index).count()) {
				return false;
			}
		}
		int current = source.getOrDefault("var" + stageIndex, -1);
		if (current < 0 || current >= stages.get(stageIndex).count()) {
			return false;
		}
		return target.getOrDefault("var" + stageIndex, -1) == current + 1;
	}

	private static Map<String, Integer> projectionOf(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(node -> node.label().equals(label))
			.map(node -> node.projection().variables()).findFirst().orElse(null);
	}

	private static boolean hasAccept(QuestDefinition definition, int npcId) {
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& (isAction(talk, QuestDialogAction.QUEST_ACCEPT_1)
					|| isAction(talk, QuestDialogAction.QUEST_ACCEPT_SIMPLE))
				&& transition.targetNode() != null
				&& statusOf(definition, transition.targetNode()) == QuestStatus.START);
	}

	/**
	 * P0-2 规范形交付：满段 QUEST_SELECT 直翻 REWARD + 分档查表奖励窗（QE-028，SELECT2 报告页与
	 * 1009 中转删除）。 / P0-2 canonical delivery: the full-node QUEST_SELECT flips REWARD with the
	 * tiered reward window (QE-028); the SELECT2 report page and 1009 hop are gone.
	 */
	private static boolean hasReport(QuestDefinition definition, int npcId, QuestMetadata metadata) {
		int rewardWindow = com.aionemu.gameserver.questEngine.definition.QuestDialogPage
			.rewardWindowForTier(metadata.rewardGroups().size() - 1).orElseThrow().id();
		return definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && isAction(talk, QuestDialogAction.QUEST_SELECT)
				&& transition.afterCommit().contains(
					new com.aionemu.gameserver.questEngine.definition.AfterCommitAction.ShowQuestDialog(
						rewardWindow))
				&& transition.targetNode() != null
				&& statusOf(definition, transition.targetNode()) == QuestStatus.REWARD);
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

	/** 逐任务分类（真端合成定义 vs 历史 XML）。 / Per-quest classification versus the legacy XML. */
	private static Map<Integer, String> classify() throws Exception {
		Map<Integer, String> classification = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			RetailRow row = retailRow(questId);
			if (!row.outcome().accepted()) {
				classification.put(questId, "REJECTED:" + row.outcome().rejectionCode());
				continue;
			}
			if (RetiredQuestIds.contains(questId)) {
				classification.put(questId, driftRegistry.get(questId));
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
		if (line.matches("N\\t\\w.*") || line.startsWith("N\t")) {
			return prefix + "NODE";
		}
		for (String token : List.of("1002", "1003", "1008", "10000", "1009", "31", "39")) {
			if (line.contains("dialogId=" + token)) {
				return prefix + "DIALOG_" + token;
			}
		}
		if (line.startsWith("T\t")) {
			return prefix + "ROUTE";
		}
		return prefix + "OTHER";
	}

	private static String dumpText(Map<Integer, String> classification) {
		Map<String, Integer> histogram = new TreeMap<>();
		classification.values().forEach(kind -> histogram.merge(kind, 1, Integer::sum));
		StringBuilder text = new StringBuilder("# SimpleSerialHunt 漂移登记（禁止手改；重算：-Dretail.serialHunt.equivOut=<path>）\n");
		histogram.forEach((kind, count) -> text.append("# ").append(kind).append('\t').append(count).append('\n'));
		classification.forEach((questId, kind) -> text.append(questId).append('\t').append(kind).append('\n'));
		return text.toString();
	}

	/** 单任务真端编译上下文。 / Retail compile context for one quest row. */
	private record RetailRow(RetailSimpleHuntTable.Entry entry, QuestMetadata metadata, int acquiredNpc,
			int rewardNpc, RetailSimpleHuntDefinitionCompiler.Outcome outcome) {
	}

	private static RetailRow retailRow(int questId) {
		RetailSimpleHuntTable.Entry entry = table.find(questId).orElseThrow();
		var metadata = RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(), npcIndex,
			itemIndex, randomRewards, nameIds);
		Set<Integer> acquired = npcIndex.resolveAll(
			List.of(entry.acquiredNpc() == null ? "" : entry.acquiredNpc())).npcIds();
		Set<Integer> reward = npcIndex.resolveAll(
			List.of(entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
		String talkNpcName = entry.talkNpc() == null ? "" : entry.talkNpc();
		Set<Integer> briefing = npcIndex.resolveAll(List.of(talkNpcName)).npcIds();
		var plan = new com.aionemu.gameserver.questEngine.retail.RetailSimpleHuntPlan(questId, List.of(),
			acquired, reward, 0, List.of(), entry.acquiredNpc() == null ? "" : entry.acquiredNpc(),
			entry.rewardNpc() == null ? "" : entry.rewardNpc());
		var outcome = RetailSimpleHuntDefinitionCompiler.compileSerialChain(plan, huntStages, metadata,
			briefing, talkNpcName);
		return new RetailRow(entry, metadata.metadata(),
			acquired.size() == 1 ? acquired.iterator().next() : -1,
			reward.size() == 1 ? reward.iterator().next() : -1, outcome);
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
		InputStream input = RetailSimpleSerialHuntGateTest.class.getResourceAsStream(resource);
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
		var url = RetailSimpleSerialHuntGateTest.class.getResource(dir);
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
		for (String line : lines(open("/aion/data/static_data/quest/retail/quest_name_string_ids.tsv"))) {
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
			.parse(open("/aion/data/static_data/quest/legacy/quest_random_rewards.xml"));
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			var element = (org.w3c.dom.Element) nodes.item(index);
			ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
				Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
		}
		return ids;
	}
}
