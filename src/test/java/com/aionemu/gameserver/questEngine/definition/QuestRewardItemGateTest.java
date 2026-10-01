package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.io.BufferedReader;
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

	/**
	 * 多档平铺单池形态：1687/2677 真端为三档 selectable（档 1 防具 4 件、
	 * 档 2/3 其余装备），生产以单池平铺+全 choice 表达（玩家可选范围一致）。
	 * 按 QE-026 重建三档 reward-groups 前作为已定性形态保留。
	 */
	private static final Set<Integer> MULTI_TIER_FLATTENED = Set.of(1687, 2677);

	/**
	 * 真端 reward_item_ext_1 以档 1 平铺表达的形态：18606/50029/51029 的延伸
	 * 奖励道具在档 1 容器发放（结算结果一致：玩家拿到该道具）。
	 */
	private static final Set<Integer> EXT_FLATTENED = Set.of(18606, 50029, 51029);

	private static Map<Integer, RetailItems> contract;
	private static Map<Integer, ProductionItems> production;

	record RetailItems(boolean fixedUnset, List<String> fixed, Set<Integer> selectable,
		Long extGold, List<String> extItems) {
	}

	/** 生产档位 1 容器声明 + 三种可选来源 + extended（最后一轮追加）声明。 */
	record ProductionItems(List<String> fixed, Set<Integer> selectable,
		Set<Integer> branchGranted, Long extGold, List<String> extItems) {
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
				assertEquals(4, cols.length, "contract row must have 4 columns: " + line);
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
				Long extGold = null;
				List<String> extItems = new ArrayList<>();
				if (!"-".equals(cols[3])) {
					String[] extParts = cols[3].split("\\|", -1);
					extGold = "-".equals(extParts[0]) ? null : Long.parseLong(extParts[0]);
					if (!"-".equals(extParts[1])) {
						for (String pair : extParts[1].split(";")) {
							extItems.add(pair);
						}
					}
				}
				contract.put(Integer.parseInt(cols[0]),
					new RetailItems(fixedUnset, fixed, selectable, extGold, extItems));
			}
		}
		assertFalse(contract.isEmpty(), "item contract must not be empty");

		production = new HashMap<>();
		for (Integer qid : contract.keySet()) {
			// P3 重锚（计划 §8.9）：已切到原生车道的行（SimpleTalk / SimpleHunt / SimpleSerialHunt）
			// 没有 typed 定义，道具轴直接取自真端表行；其余行仍按生产视图（真端合成定义）反推。
			// P3 re-anchor (plan §8.9): rows on the native lane carry no typed definition, so their item
			// axis comes from the retail row itself; the remaining rows keep the synthesized view.
			if (nativeOwned(qid)) {
				production.put(qid, parseNativeRowItems(qid));
			} else {
				production.put(qid, RetiredQuestIds.contains(qid) ? parseRetailProduction(qid) : parseProduction(qid));
			}
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

		// extended-rewards（最后一轮追加）：容器直属 reward 行
		Long extGold = null;
		List<String> extItems = new ArrayList<>();
		var extContainers = metadata.getElementsByTagName("extended-rewards");
		var extGroupContainers = metadata.getElementsByTagName("extended-reward-groups");
		if (extContainers.getLength() == 0 && extGroupContainers.getLength() > 0) {
			// 多档 extended 形态：组 1 即最后一轮追加奖励
			var extGroups = ((Element) extGroupContainers.item(0))
				.getElementsByTagName("group");
			if (extGroups.getLength() > 0) {
				extContainers = extGroups;
			}
		}
		if (extContainers.getLength() > 0) {
			Element extContainer = (Element) extContainers.item(0);
			for (Element child = firstElementChild(extContainer); child != null;
					child = getNextElement(child)) {
				if ("reward".equals(child.getTagName())) {
					String kind = child.getAttribute("kind");
					if ("GOLD".equals(kind)) {
						extGold = Long.parseLong(child.getAttribute("amount"));
					} else if ("ITEM".equals(kind) || "SELECTABLE_ITEM".equals(kind)) {
						extItems.add(child.getAttribute("id") + ":"
							+ child.getAttribute("amount"));
					}
				}
			}
		}
		extItems.sort(null);
		return new ProductionItems(fixed, allSelectable, new TreeSet<>(),
			extGold, extItems);
	}

	/** 已切到原生车道的行（SimpleTalk / SimpleHunt / SimpleSerialHunt）。 /
	 * Rows already switched to the native lane. */
	private static boolean nativeOwned(int questId) {
		return SimpleTalkHandler.instance().routes(questId)
			|| SimpleHuntHandler.instance().routes(questId)
			|| SimpleSerialHuntHandler.instance().routes(questId);
	}

	/**
	 * 原生车道的奖赏轴：真端 {@code quest.xml} 行就是唯一事实来源——固定道具取
	 * {@code reward_item1_N}（缺列即 RETAIL_UNSET 语义），可选取 {@code selectable_reward_item1_N}，
	 * extended 取 {@code reward_gold_ext / reward_item_ext_N / selectable_reward_item_ext_N}；
	 * 道具符号 → id 走生产物品名索引（与合同快照同一条 name_desc 通道）。合同表是客户端
	 * {@code Quest_unpacked/quest.xml} 的冻结快照，故本方法对 native 行构成「服务端表 → 客户端表」对拍。
	 * <p>
	 * Native reward axis: the retail {@code quest.xml} row is the single source of truth — fixed items
	 * from {@code reward_item1_N} (a missing column means RETAIL_UNSET), selectable items from
	 * {@code selectable_reward_item1_N}, extended from the {@code *_ext*} columns, with symbols mapped
	 * through the production item-name index. The contract TSV is a frozen client-side snapshot, so this
	 * compares the server table against the client table for native rows.
	 */
	private static ProductionItems parseNativeRowItems(int questId) throws Exception {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId)
			.orElseThrow(() -> new IllegalStateException("missing retail quest.xml row " + questId));
		RetailItemNameIndex itemIndex = itemIndex();
		List<String> fixed = new ArrayList<>();
		Set<Integer> selectable = new TreeSet<>();
		for (Map.Entry<String, List<String>> field : row.fields().entrySet()) {
			String tag = field.getKey();
			if (field.getValue().isEmpty()) {
				continue;
			}
			if (tag.startsWith("reward_item_ext_") || tag.startsWith("selectable_reward_item_ext_")
				|| tag.equals("reward_gold_ext") || tag.equals("reward_title_ext")) {
				continue;
			}
			boolean fixedSlot = tag.startsWith("reward_item1_");
			if (!fixedSlot && !tag.startsWith("selectable_reward_item1_")) {
				continue;
			}
			for (String value : field.getValue()) {
				Integer itemId = itemIndex.resolve(symbol(value));
				assertNotNull(itemId, "retail item symbol must resolve: quest " + questId + " " + tag + "=" + value);
				if (fixedSlot) {
					fixed.add(itemId + ":" + symbolCount(value));
				} else {
					selectable.add(itemId);
				}
			}
		}
		List<String> extItems = new ArrayList<>();
		for (Map.Entry<String, List<String>> field : row.fields().entrySet()) {
			String tag = field.getKey();
			if (!tag.startsWith("reward_item_ext_") && !tag.startsWith("selectable_reward_item_ext_")) {
				continue;
			}
			for (String value : field.getValue()) {
				Integer itemId = itemIndex.resolve(symbol(value));
				assertNotNull(itemId, "retail ext item symbol must resolve: quest " + questId + " " + tag + "=" + value);
				extItems.add(itemId + ":" + symbolCount(value));
			}
		}
		extItems.sort(null);
		Long extGold = row.text("reward_gold_ext").isBlank() ? null : Long.parseLong(row.text("reward_gold_ext").trim());
		return new ProductionItems(fixed, selectable, new TreeSet<>(), extGold, extItems);
	}

	/** 生产物品名索引（惰性缓存：合同 3992 行共享一份）。 / The production item-name index, cached. */
	private static synchronized RetailItemNameIndex itemIndex() throws Exception {
		if (ITEM_INDEX == null) {
			ITEM_INDEX = RetailItemNameIndex.loadItemTemplates();
		}
		return ITEM_INDEX;
	}

	private static RetailItemNameIndex ITEM_INDEX;

	/** 真端物品单元格的物品名（空格前段，允许名称本身含空格时取最后一段为数量）。 /
	 * The item name of a retail cell (the trailing numeric token is the count). */
	private static String symbol(String cell) {
		String trimmed = cell.trim();
		int lastSpace = trimmed.lastIndexOf(' ');
		if (lastSpace < 0) {
			return trimmed;
		}
		String tail = trimmed.substring(lastSpace + 1);
		return tail.chars().allMatch(Character::isDigit) ? trimmed.substring(0, lastSpace) : trimmed;
	}

	/** 真端物品单元格的数量（缺省 1）。 / The cell count (1 when absent). */
	private static int symbolCount(String cell) {
		String trimmed = cell.trim();
		int lastSpace = trimmed.lastIndexOf(' ');
		if (lastSpace < 0) {
			return 1;
		}
		String tail = trimmed.substring(lastSpace + 1);
		return tail.chars().allMatch(Character::isDigit) ? Integer.parseInt(tail) : 1;
	}

	/**
	 * 已退役任务的检查路径：定义取生产视图（真端合成），三个来源从 IR 反推——
	 * 固定道具 = 档位 1 的 ITEM 奖励；可选项 = 领奖确认分支相对固定奖励额外发放的道具；
	 * extended = 元数据的最后一轮追加奖励。
	 * Retired quests carry no XML; the item axes are re-derived from the synthesized definition.
	 */
	private static ProductionItems parseRetailProduction(int questId) {
		CompiledQuestDefinition compiled = ProductionQuestDefinitions.definition(questId);
		QuestMetadata metadata = compiled.definition().metadata();
		List<QuestReward> group = metadata.rewardGroups().isEmpty() ? List.of()
			: metadata.rewardGroups().get(0).rewards();
		List<String> fixed = new ArrayList<>();
		for (QuestReward reward : group) {
			if ("ITEM".equals(reward.kind())) {
				fixed.add(reward.id() + ":" + reward.amount());
			}
		}
		Set<Integer> selectable = new TreeSet<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.dialogId() == null) {
				continue;
			}
			if (talk.dialogId() < QuestDialogAction.SELECTED_QUEST_REWARD1.id()
				|| talk.dialogId() > QuestDialogAction.SELECTED_QUEST_NOREWARD.id()) {
				continue;
			}
			for (QuestAction action : transition.actions()) {
				// choice 确认路由上的可选项发放带 SELECTABLE_ITEM kind（P1b 合成口径），同样计入可选集合。
				// Choice confirm routes grant selectable items with SELECTABLE_ITEM kind (P1b synthesis).
				if (action instanceof QuestAction.GrantReward grant
					&& ("ITEM".equals(grant.kind()) || "SELECTABLE_ITEM".equals(grant.kind()))
					&& !fixed.contains(grant.id() + ":" + grant.amount())) {
					selectable.add(grant.id());
				}
			}
		}
		Long extGold = null;
		List<String> extItems = new ArrayList<>();
		for (QuestReward reward : metadata.extendedRewards()) {
			if ("GOLD".equals(reward.kind())) {
				extGold = reward.amount();
			} else if ("ITEM".equals(reward.kind()) || "SELECTABLE_ITEM".equals(reward.kind())) {
				extItems.add(reward.id() + ":" + reward.amount());
			}
		}
		extItems.sort(null);
		return new ProductionItems(fixed, selectable, new TreeSet<>(), extGold, extItems);
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
				|| BRANCH_TIER_EQUIVALENT.contains(entry.getKey())
				|| MULTI_TIER_FLATTENED.contains(entry.getKey())
				|| EXT_FLATTENED.contains(entry.getKey())) {
				// 已定性：双路线待取证 / 分支档位一致 / 多档平铺单池 / ext 平铺
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
				|| BRANCH_TIER_EQUIVALENT.contains(qid)
				|| MULTI_TIER_FLATTENED.contains(qid)
				|| EXT_FLATTENED.contains(qid)) {
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

	/**
	 * extended-rewards（真端 reward_gold_ext / reward_item_ext_1 /
	 * selectable_reward_item_ext_N，引擎在最后一轮重复完成时追加发放）
	 * 必须与真端一致；真端无 ext 字段（"-"）的任务不纳入比对。
	 * 2368 的 title_ext=dark_title27 无模板表映射数字 id，TITLE 声明豁免。
	 */
	@Test
	void extendedRewardsMatchTheRetailContract() {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<Integer, RetailItems> entry : contract.entrySet()) {
			if (entry.getValue().extGold() == null && entry.getValue().extItems().isEmpty()) {
				continue;
			}
			int qid = entry.getKey();
			ProductionItems actual = production.get(qid);
			List<String> actualItems = new ArrayList<>(actual.extItems());
			if (!actualItems.equals(entry.getValue().extItems())) {
				problems.add("quest " + qid + " extended items=" + actualItems
					+ " but retail=" + entry.getValue().extItems());
				continue;
			}
			Long actualGold = actual.extGold();
			Long retailGold = entry.getValue().extGold();
			if (!(actualGold == null ? retailGold == null
					: actualGold.equals(retailGold))) {
				problems.add("quest " + qid + " extended gold=" + actualGold
					+ " but retail=" + retailGold);
			}
		}
		assertTrue(problems.isEmpty(), () -> "extended reward mismatches: " + problems);
	}
}
