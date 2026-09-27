package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestRecipeOwnership;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CombineTask 真端驱动门禁（M4-b 起）。判据是 <b>真端语义</b>，不是"必须与历史 quest-definition XML 等价"：
 * XML 自身带历史错误（用户口径 2026-09-23），只有在真端表达不了时才保留。
 * <p>
 * 本族初始迁移时 574/574 的合成结果与退役前 XML 逐行 IR 等价；奖励窗全局 108
 * 去重后为有意的 IR 演进，须审阅并重算冻结指纹。后续继续用本门禁阻止无意漂移。
 * <p>
 * 断言三件事：
 * <ol>
 * <li><b>家族规模冻结</b>：真端 {@code Quest_CombineTask.xml} ∩ 生产宇宙（catalog ∪ 退役清单）= 574；</li>
 * <li><b>真端语义不变量</b>：每行的接取（分量 + 配方）、交付（产物条件 + 回收分量）、完成区间、
 * 放弃（忘配方）、四节点 {@code var0=0} 必须与真端表/元数据一致，且可驱动数不许回退；</li>
 * <li><b>冻结 IR 指纹</b>：{@code retail-combine-task-ir-fingerprints.tsv} 与当前合成结果逐行一致
 * （XML 删除后仍能发现静默漂移；{@code -Dretail.combine.fpOut=<path>} 重算）。</li>
 * </ol>
 * Retail-semantics gate for the CombineTask family.
 */
class RetailCombineTaskGateTest {

	private static final String CATALOG = "/aion/data/static_data/quest_definition/quest_definition_catalog.xml";
	private static final String COMBINE_TABLE = "/aion/data/static_data/quest_retail/Quest_CombineTask.xml";
	private static final String RECIPE_TEMPLATES = "/aion/data/static_data/recipe/recipe_templates.xml";
	private static final String FINGERPRINTS = "/quest/retail-combine-task-ir-fingerprints.tsv";
	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String ITEM_DIR = "/aion/data/static_data/items/item/";
	/** 冻结家族规模（真端表 ∩ 生产宇宙）。 / Frozen family size. */
	private static final int FROZEN_FAMILY_SIZE = 574;
	/** 可驱动数量下限，防止静默回退。 / Floor for retail-drivable quests. */
	private static final int ACCEPTED_FLOOR = 574;
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static RetailCombineTaskTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static RetailRecipeIndex recipeIndex;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;
	private static Set<Integer> familyIds;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open(COMBINE_TABLE)) {
			table = RetailCombineTaskTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest_retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		recipeIndex = RetailRecipeIndex.build(List.of(open(RECIPE_TEMPLATES)));
		npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_TEMPLATES), RetailQuestAiNameGroupsFixture.streams());
		itemIndex = RetailItemNameIndex.build(openAll(ITEM_DIR, listXmlNames(ITEM_DIR)));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
		familyIds = familyIds();
	}

	@Test
	void familyScopeIsFrozen() {
		assertTrue(familyIds.size() == FROZEN_FAMILY_SIZE,
			() -> "CombineTask 家族规模漂移：期望 " + FROZEN_FAMILY_SIZE + " 实际 " + familyIds.size());
		assertTrue(table.size() == FROZEN_FAMILY_SIZE,
			() -> "真端 CombineTask 表行数漂移：" + table.size());
	}

	/** 每行的真端语义不变量：接取分量/配方、交付产物、完成区间、放弃忘配方。 / Per-row retail semantics. */
	@Test
	void acceptedDefinitionsCarryRetailSemantics() throws Exception {
		List<String> problems = new ArrayList<>();
		int accepted = 0;
		for (int questId : new TreeSet<>(familyIds)) {
			RetailCombineTaskTable.Entry row = table.find(questId).orElseThrow();
			var outcome = compile(questId, row);
			if (!outcome.accepted()) {
				problems.add(questId + " 拒绝：" + outcome.rejectionCode() + " " + outcome.detail());
				continue;
			}
			accepted++;
			QuestDefinition definition = outcome.definition().definition();
			List<Integer> npcIds = resolveNpcs(row);
			checkNodes(questId, definition, problems);
			int productId = itemIndex.resolve(row.products().get(0).name());
			int recipeId = recipeIndex.resolveUnique(skillId(row), productId).orElseThrow();
			for (int npcId : npcIds) {
				checkAccept(questId, npcId, definition, row, recipeId, problems);
				checkReport(questId, npcId, definition, row, productId, recipeId, problems);
				checkComplete(questId, npcId, definition, productId, recipeId, problems);
			}
			checkGlobalComplete(questId, npcIds, definition, productId, recipeId, problems);
			checkAbandon(questId, definition, recipeId, problems);
		}
		int acceptedCount = accepted;
		assertTrue(acceptedCount >= ACCEPTED_FLOOR,
			() -> "可驱动数量回退：accepted=" + acceptedCount + " / floor=" + ACCEPTED_FLOOR);
		assertTrue(problems.isEmpty(), () -> "真端语义不变量失败 " + problems.size() + " 行：\n"
			+ String.join("\n", problems.subList(0, Math.min(20, problems.size()))));
	}

	/** 冻结 IR 指纹：XML 删除后仍能发现静默漂移。 / Frozen fingerprints, the post-deletion drift guard. */
	@Test
	void frozenFingerprintsMatch() throws Exception {
		Map<Integer, String> expected = fingerprints();
		Map<Integer, String> actual = new TreeMap<>();
		for (int questId : new TreeSet<>(familyIds)) {
			RetailCombineTaskTable.Entry row = table.find(questId).orElseThrow();
			var outcome = compile(questId, row);
			if (outcome.accepted()) {
				actual.put(questId, RetailIrFingerprint.fingerprint(outcome.definition().definition()));
			}
		}
		String out = System.getProperty("retail.combine.fpOut");
		if (out != null) {
			Path target = Path.of(out);
			Files.createDirectories(target.getParent());
			StringBuilder text = new StringBuilder("# CombineTask 冻结 IR 指纹（XML 删除后继续证明等价的依据）\n"
				+ "# quest_id\tretail_fingerprint\tnodes\ttransitions\n");
			for (Map.Entry<Integer, String> entry : actual.entrySet()) {
				QuestDefinition definition = compile(entry.getKey(), table.find(entry.getKey()).orElseThrow())
					.definition().definition();
				text.append(entry.getKey()).append('\t').append(entry.getValue()).append('\t')
					.append(definition.nodes().size()).append('\t').append(definition.transitions().size())
					.append('\n');
			}
			Files.writeString(target, text.toString());
		}
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, String> entry : expected.entrySet()) {
			String observed = actual.get(entry.getKey());
			if (observed == null) {
				problems.add(entry.getKey() + " 未产生可驱动定义");
			} else if (!observed.equals(entry.getValue())) {
				problems.add(entry.getKey() + " 指纹漂移：" + entry.getValue() + " -> " + observed);
			}
		}
		assertTrue(problems.isEmpty(), () -> "冻结指纹漂移 " + problems.size() + " 行：\n"
			+ String.join("\n", problems.subList(0, Math.min(20, problems.size()))));
	}

	private static void checkNodes(int questId, QuestDefinition definition, List<String> problems) {
		List<String> statuses = definition.nodes().stream().map(node -> node.projection().status().name()).toList();
		if (!statuses.equals(List.of("NONE", "START", "REWARD", "COMPLETE"))) {
			problems.add(questId + " 节点状态=" + statuses);
		}
		for (QuestNode node : definition.nodes()) {
			if (definition.progressLayout().pack(node.projection().variables()) != 0) {
				problems.add(questId + " 节点 " + node.label() + " 投影非 0");
			}
		}
	}

	private static void checkAccept(int questId, int npcId, QuestDefinition definition,
			RetailCombineTaskTable.Entry row, int recipeId, List<String> problems) {
		List<QuestAction> expected = new ArrayList<>();
		for (RetailCombineTaskTable.Slot component : row.components()) {
			expected.add(new QuestAction.GiveItem(itemIndex.resolve(component.name()), component.count()));
		}
		expected.add(new QuestAction.LearnRecipe(recipeId, QuestRecipeOwnership.QUEST_OWNED));
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_ACCEPT_1,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
			boolean found = definition.transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
					&& talk.dialogId() != null && talk.dialogId() == action.id()
					&& transition.actions().equals(expected));
			if (!found) {
				problems.add(questId + " NPC " + npcId + " 接取动作缺失：" + action);
			}
		}
	}

	private static void checkReport(int questId, int npcId, QuestDefinition definition,
			RetailCombineTaskTable.Entry row, int productId, int recipeId, List<String> problems) {
		int productCount = row.products().get(0).count();
		List<QuestAction> expectedRemove = row.components().stream()
			.map(component -> (QuestAction) new QuestAction.RemoveItem(
				itemIndex.resolve(component.name()), QuestAction.RemoveItem.ALL))
			.toList();
		boolean success = definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()
				&& transition.conditions().equals(List.of(new QuestCondition.HasItem(productId, productCount)))
				&& transition.actions().equals(expectedRemove)
				&& "reward".equals(transition.targetNode()) && Integer.valueOf(0).equals(transition.priority()));
		boolean fallback = definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()
				&& transition.conditions().isEmpty() && Integer.valueOf(10).equals(transition.priority())
				&& transition.afterCommit().contains(
					new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_2.id())));
		if (!success) {
			problems.add(questId + " NPC " + npcId + " 交付产物路由缺失（recipe=" + recipeId + "）");
		}
		if (!fallback) {
			problems.add(questId + " NPC " + npcId + " 交付失败回退路由缺失");
		}
	}

	private static void checkComplete(int questId, int npcId, QuestDefinition definition,
			int productId, int recipeId, List<String> problems) {
		List<QuestAction> expected = List.of(new QuestAction.RemoveItem(productId, QuestAction.RemoveItem.ALL),
			new QuestAction.ForgetRecipe(recipeId), new QuestAction.CompleteQuest(0));
		Set<Integer> observed = new HashSet<>();
		for (var transition : definition.transitions()) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& talk.dialogId() != null && "complete".equals(transition.targetNode())
				&& transition.actions().equals(expected)) {
				observed.add(talk.dialogId());
			}
		}
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			if (!observed.contains(id)) {
				problems.add(questId + " NPC " + npcId + " 完成动作缺失：" + id);
				return;
			}
		}
	}

	private static void checkGlobalComplete(int questId, List<Integer> npcIds, QuestDefinition definition,
			int productId, int recipeId, List<String> problems) {
		int dialogId = QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id();
		List<QuestTransition> global = definition.transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.QuestDialog(dialogId)))
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.toList();
		if (global.size() != 1) {
			problems.add(questId + " 全局奖励确认路由数量=" + global.size());
			return;
		}
		QuestTransition route = global.getFirst();
		List<QuestAction> expectedActions = List.of(
			new QuestAction.RemoveItem(productId, QuestAction.RemoveItem.ALL),
			new QuestAction.ForgetRecipe(recipeId), new QuestAction.CompleteQuest(0));
		List<AfterCommitAction> expectedAfterCommit = List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.CloseDialog());
		if (!"complete".equals(route.targetNode()) || !route.conditions().isEmpty()
				|| route.priority() != null || !expectedActions.equals(route.actions())
				|| !expectedAfterCommit.equals(route.afterCommit())) {
			problems.add(questId + " 全局奖励确认合同不匹配");
		}
		for (int npcId : npcIds) {
			long npcRouteCount = definition.transitions().stream()
				.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)))
				.filter(transition -> "reward".equals(transition.sourceNode()))
				.count();
			if (npcRouteCount != 1) {
				problems.add(questId + " NPC " + npcId + " 奖励确认路由数量=" + npcRouteCount);
			}
		}
	}

	private static void checkAbandon(int questId, QuestDefinition definition, int recipeId,
			List<String> problems) {
		boolean found = definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.Abandon
				&& transition.actions().equals(List.of(new QuestAction.ForgetRecipe(recipeId))));
		if (!found) {
			problems.add(questId + " 放弃路由缺失（忘配方 " + recipeId + "）");
		}
	}

	private static RetailCombineTaskDefinitionCompiler.Outcome compile(int questId,
			RetailCombineTaskTable.Entry row) {
		var metadata = RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(), npcIndex,
			itemIndex, randomRewards, nameIds);
		return RetailCombineTaskDefinitionCompiler.compile(row, npcIndex, itemIndex, recipeIndex, metadata);
	}

	private static List<Integer> resolveNpcs(RetailCombineTaskTable.Entry row) {
		List<Integer> ids = new ArrayList<>();
		for (String npc : row.taskNpcs()) {
			ids.add(npcIndex.resolveAll(List.of(npc)).npcIds().iterator().next());
		}
		return ids.stream().distinct().toList();
	}

	private static int skillId(RetailCombineTaskTable.Entry row) {
		return RetailQuestMetadataCompiler.combineSkillId(row.combineSkill());
	}

	/** 真端表 ∩ 生产宇宙（catalog ∪ 退役清单）。 / Retail table intersected with the production universe. */
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

	private static Map<Integer, String> fingerprints() throws Exception {
		Map<Integer, String> rows = new TreeMap<>();
		for (String line : lines(open(FINGERPRINTS))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			rows.put(Integer.parseInt(parts[0]), parts[1]);
		}
		return rows;
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
		var url = RetailCombineTaskGateTest.class.getResource(dir);
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
		InputStream input = RetailCombineTaskGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}
}
