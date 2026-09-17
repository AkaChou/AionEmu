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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 固定奖励道具与可选奖励（item/selectable axis）真端合同门禁。
 * <p>
 * 以 Aion 5.8 真端 quest.xml 的道具快照
 * ({@code /quest/quest-item-selectable-retail-contract.tsv}) 为权威：
 * <ul>
 * <li>固定道具：生产档位 1 容器（平铺 &lt;rewards&gt; 或第一个 &lt;group&gt;）中
 * kind=ITEM 的 (id,amount) 多重集合必须与真端 reward_item1_N 一致；
 * 真端字段缺失（RETAIL_UNSET）= 真端未配置，跳过；</li>
 * <li>可选道具：生产三种等价发放来源的并集——metadata SELECTABLE_ITEM 声明、
 * 显式 SELECTED_QUEST_REWARD 分支的可变 grant-reward ITEM、npc-complete choice
 * 指向的 SELECTABLE_ITEM——必须与真端 selectable_reward_item1_N 集合一致。</li>
 * </ul>
 * 例外（逐条列明，禁止通配豁免）：
 * <ul>
 * <li>16921/26921：真端唯一一项可选（188052438）允许以固定 ITEM 声明/发放表达；</li>
 * <li>2392：分支档位形态——生产三分支发放 (8/4/4 药水 + 2 币) 与真端单组显示
 * (8 药水 + 2 币) 的最大档完全一致，已验收形态；</li>
 * <li>2345：双路线任务（reward0/reward1 两组奖励组），真端单组字段无法判定组归属，
 * EVIDENCE_BLOCKED（取证方向：旧 handler 2345 的路线发放代码）。</li>
 * </ul>
 * 真端道具名不可映射的行（956 条）不入基线，整类 EVIDENCE_BLOCKED。
 */
class QuestRewardItemGateTest {

	private static final String CONTRACT_RESOURCE =
		"/quest/quest-item-selectable-retail-contract.tsv";
	private static final String RETAIL_UNSET = "RETAIL_UNSET";

	/** 真端唯一可选 = 固定发放的等价表达。 */
	private static final Set<Integer> SINGLE_SELECTABLE_AS_FIXED = Set.of(16921, 26921);
	/** 分支档位形态（发放集合覆盖真端显示组）。 */
	private static final Set<Integer> BRANCH_TIER_EQUIVALENT = Set.of(2392);
	/** 双路线任务，组归属无法从真端单组字段判定。 */
	private static final Set<Integer> DUAL_ROUTE_EVIDENCE_BLOCKED = Set.of(2345);

	private static Map<Integer, RetailItems> contract;
	private static Map<Integer, ProductionItems> production;

	record RetailItems(boolean fixedUnset, List<String> fixed, Set<Integer> selectable) {
	}

	/** 生产档位 1 容器声明 + 三种可选来源。 */
	record ProductionItems(List<String> fixed, Set<Integer> selectable,
		Set<Integer> branchGranted) {
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
				assertEquals(3, cols.length, "contract row must have 3 columns: " + line);
				boolean fixedUnset = RETAIL_UNSET.equals(cols[1]);
				List<String> fixed = new ArrayList<>();
				if (!fixedUnset && !cols[1].isEmpty()) {
					for (String pair : cols[1].split(";")) {
						fixed.add(pair);
					}
				}
				Set<Integer> selectable = new TreeSet<>();
				if (!cols[2].isEmpty() && !"-".equals(cols[2])) {
					for (String id : cols[2].split(",")) {
						selectable.add(Integer.parseInt(id));
					}
				}
				contract.put(Integer.parseInt(cols[0]),
					new RetailItems(fixedUnset, fixed, selectable));
			}
		}
		assertFalse(contract.isEmpty(), "item contract must not be empty");

		production = new HashMap<>();
		for (Integer qid : contract.keySet()) {
			production.put(qid, parseProduction(qid));
		}
		assertFalse(production.isEmpty(), "production items must not be empty");
	}

	private static Element firstElementChild(Element parent) {
		for (var n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				return (Element) n;
			}
		}
		return null;
	}

	/** DOM 无 nextElementSibling 时的直接兄弟元素遍历。 */
	private static Element getNextElement(Element current) {
		var next = current.getNextSibling();
		while (next != null && next.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
			next = next.getNextSibling();
		}
		return (Element) next;
	}

	private static BufferedReader open(String resource) {
		InputStream input = QuestRewardItemGateTest.class.getResourceAsStream(resource);
		assertNotNull(input, resource + " must exist on the test classpath");
		return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
	}

	/** 档位 1 容器声明 + 显式分支 + npc-complete choice 三来源解析。 */
	private static ProductionItems parseProduction(int questId) throws Exception {
		Element root = QuestRetailStartMetadataGateTest.parseQuestXml(questId)
			.getDocumentElement();
		Element metadata = (Element) root.getElementsByTagName("metadata").item(0);
		List<String> fixed = new ArrayList<>();
		Set<Integer> selectableDeclared = new TreeSet<>();
		// npc-complete 的 reward-index 指向整个容器平铺顺序（含 EXP/GOLD/ITEM）
		List<Element> flatRewards = new ArrayList<>();
		// 档位 1 容器：平铺 <rewards> 或多档任务 <reward-groups> 的第一个 <group>；
		// group 查找必须限定在 reward-groups 下（start-condition-groups 也有 <group>）
		Element tierOne;
		var rewardGroupsContainers = metadata.getElementsByTagName("reward-groups");
		var rewardsContainers = metadata.getElementsByTagName("rewards");
		if (rewardGroupsContainers.getLength() > 0) {
			var groups = ((Element) rewardGroupsContainers.item(0))
				.getElementsByTagName("group");
			tierOne = groups.getLength() > 0 ? (Element) groups.item(0) : null;
		} else if (rewardsContainers.getLength() > 0) {
			tierOne = (Element) rewardsContainers.item(0);
		} else {
			tierOne = null;
		}
		// 只取容器直属 reward 行（非递归）：class-rewards 等其他子树的
		// reward 标签不属于档位 1 容器
		if (tierOne != null) {
			for (Element child = firstElementChild(tierOne); child != null;
					child = getNextElement(child)) {
				if ("reward".equals(child.getTagName())) {
					flatRewards.add(child);
				}
			}
		}
		for (Element reward : flatRewards) {
			String kind = reward.getAttribute("kind");
			if ("ITEM".equals(kind)) {
				fixed.add(reward.getAttribute("id") + ":" + reward.getAttribute("amount"));
			} else if ("SELECTABLE_ITEM".equals(kind)) {
				selectableDeclared.add(Integer.parseInt(reward.getAttribute("id")));
			}
		}

		Set<Integer> explicitBranch = new TreeSet<>();
		var transitions = root.getElementsByTagName("transition");
		int branchCount = 0;
		Map<Integer, Integer> branchItemHits = new HashMap<>();
		for (int i = 0; i < transitions.getLength(); i++) {
			Element tr = (Element) transitions.item(i);
			var dialogs = tr.getElementsByTagName("dialog");
			boolean selected = false;
			for (int d = 0; d < dialogs.getLength(); d++) {
				String action = (dialogs.item(d).getAttributes()
					.getNamedItem("action") == null ? "" : dialogs.item(d).getAttributes()
					.getNamedItem("action").getNodeValue())
					+ " " + (dialogs.item(d).getAttributes()
					.getNamedItem("actions") == null ? "" : dialogs.item(d).getAttributes()
					.getNamedItem("actions").getNodeValue());
				if (action.contains("SELECTED_QUEST_REWARD")) {
					selected = true;
				}
			}
			if (!selected) {
				continue;
			}
			var grants = tr.getElementsByTagName("grant-reward");
			Set<Integer> branchIds = new TreeSet<>();
			for (int g = 0; g < grants.getLength(); g++) {
				Element grant = (Element) grants.item(g);
				if ("ITEM".equals(grant.getAttribute("kind"))) {
					branchIds.add(Integer.parseInt(grant.getAttribute("id")));
				}
			}
			if (branchIds.isEmpty()) {
				continue;
			}
			branchCount++;
			branchIds.forEach(id -> branchItemHits.merge(id, 1, Integer::sum));
		}
		for (Map.Entry<Integer, Integer> entry : branchItemHits.entrySet()) {
			if (branchCount >= 2 && entry.getValue() < branchCount) {
				explicitBranch.add(entry.getKey());
			}
		}

		Set<Integer> choiceSelectables = new TreeSet<>();
		var completions = root.getElementsByTagName("npc-complete");
		for (int i = 0; i < completions.getLength(); i++) {
			Element nc = (Element) completions.item(i);
			var choices = nc.getElementsByTagName("choice");
			for (int c = 0; c < choices.getLength(); c++) {
				int idx = Integer.parseInt(
					((Element) choices.item(c)).getAttribute("reward-index"));
				if (0 <= idx && idx < flatRewards.size()
					&& "SELECTABLE_ITEM".equals(flatRewards.get(idx).getAttribute("kind"))) {
					choiceSelectables.add(Integer.parseInt(
						flatRewards.get(idx).getAttribute("id")));
				}
			}
		}
		Set<Integer> allSelectable = new TreeSet<>(selectableDeclared);
		allSelectable.addAll(explicitBranch);
		allSelectable.addAll(choiceSelectables);
		return new ProductionItems(fixed, allSelectable, new TreeSet<>());
	}

	/** 固定道具多重集合必须与真端一致（真端字段缺失跳过）。 */
	@Test
	void fixedItemsMatchTheRetailContract() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailItems> entry : contract.entrySet()) {
			RetailItems retail = entry.getValue();
			if (retail.fixedUnset()) {
				continue;
			}
			if (DUAL_ROUTE_EVIDENCE_BLOCKED.contains(entry.getKey())
				|| BRANCH_TIER_EQUIVALENT.contains(entry.getKey())) {
				// 已定性：双路线组归属待取证 / 分支档位平铺与真端最大档一致
				continue;
			}
			List<String> actual = new ArrayList<>(production.get(entry.getKey()).fixed());
			actual.sort(null);
			List<String> expected = new ArrayList<>(retail.fixed());
			expected.sort(null);
			if (!actual.equals(expected)) {
				problems.add("quest " + entry.getKey() + " fixed items=" + actual
					+ " but retail=" + expected);
			}
		}
		assertTrue(problems.isEmpty(), () -> "fixed item mismatches: " + problems);
	}

	/** 可选道具三来源并集必须与真端一致（等价表达与 EVIDENCE_BLOCKED 逐条例外）。 */
	@Test
	void selectableItemsMatchTheRetailContractWithListedExceptions() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailItems> entry : contract.entrySet()) {
			int qid = entry.getKey();
			RetailItems retail = entry.getValue();
			if (retail.selectable().isEmpty()) {
				continue;
			}
			Set<Integer> actual = production.get(qid).selectable();
			if (actual.equals(retail.selectable())) {
				continue;
			}
			if (DUAL_ROUTE_EVIDENCE_BLOCKED.contains(qid)
				|| BRANCH_TIER_EQUIVALENT.contains(qid)) {
				continue;
			}
			if (SINGLE_SELECTABLE_AS_FIXED.contains(qid)) {
				// 唯一可选项允许由固定声明表达，断言该 id 在生产档位 1 声明中存在
				boolean covered = production.get(qid).fixed().stream()
					.anyMatch(pair -> retail.selectable().stream().allMatch(id ->
						pair.startsWith(id + ":")));
				assertTrue(covered, "quest " + qid + " single selectable "
					+ retail.selectable() + " must be declared as fixed reward");
				continue;
			}
			problems.add("quest " + qid + " selectable=" + actual
				+ " but retail=" + retail.selectable());
		}
		assertTrue(problems.isEmpty(), () -> "selectable mismatches: " + problems);
	}
}
