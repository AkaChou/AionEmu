package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务道具来源契约门禁：收集类事件必须监听本任务自己声明/发放的道具，且跨任务道具引用必须与真端角色一致。
 * Quest item source contract gate. A {@code collect-item} event decides when the client progress
 * refresh fires, so it may only watch an item the quest itself declares (metadata items /
 * inventory-items / work-items), drops, grants, or reports. A quest that watches a neighbouring
 * quest's item never refreshes progress and can leave the collection stage stalled.
 * <p>此外锁定 2026-09-17 修复的 20 个任务：7 个"引用邻居任务道具"的任务其交付条件必须使用本任务在真端
 * collect_item/check_item 中声明的道具（开发名见 item_template 的 name_desc），另有 7 个 COLLECT_ITEM 交付缺失
 * has-item 的任务必须重新校验并扣除自己的任务道具。</p>
 * It also pins the seven quests repaired on 2026-09-17 whose turn-in condition referenced the
 * neighbouring quest's item instead of the item retail declares for that quest.
 */
class QuestItemSourceContractGateTest {

	@Test
	void collectItemEventsOnlyWatchItemsTheQuestItselfTracks() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> violations = new ArrayList<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			Set<Integer> tracked = trackedItems(definition);
			for (QuestTransition transition : definition.transitions()) {
				if (transition.event() instanceof QuestEvent.CollectItem collect && !tracked.contains(collect.itemId())) {
					violations.add("quest " + definition.id() + " node " + transition.sourceNode() + " watches collect-item "
						+ collect.itemId() + " which the quest never declares, drops, grants or reports");
				}
			}
		}
		assertTrue(violations.isEmpty(),
			() -> "collect-item events must watch an item owned by the same quest: " + violations);
	}

	@Test
	void everyRewardEntryBranchVerifiesTheQuestsOwnCollectedItems() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		Set<String> knownGaps = loadKnownHandInGaps();
		List<String> violations = new ArrayList<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			Set<Integer> collectItems = droppedCollectItems(definition.metadata());
			if (collectItems.isEmpty()) {
				continue;
			}
			Set<String> rewardNodes = new LinkedHashSet<>();
			for (QuestNode node : definition.nodes()) {
				if (node.projection().status() == QuestStatus.REWARD) {
					rewardNodes.add(node.label());
				}
			}
			// 同一 (源节点, NPC) 的多条 SELECT_QUEST_REWARD 路由构成一个交付分支：gated 主路由 + 未集齐回落路由。
			// Routes sharing (source node, npc) form one hand-in branch: a gated primary route plus a fallback.
			Map<String, Set<Integer>> gatedItems = new LinkedHashMap<>();
			for (QuestTransition transition : definition.transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc talk) || talk.dialogId() == null
						|| talk.dialogId() != QuestDialogAction.SELECT_QUEST_REWARD.id()
						|| rewardNodes.contains(transition.sourceNode())) {
					continue;
				}
				Set<Integer> gated = gatedItems.computeIfAbsent(
					transition.sourceNode() + "@" + talk.npcId(), ignored -> new LinkedHashSet<>());
				for (QuestCondition condition : transition.conditions()) {
					if (condition instanceof QuestCondition.HasItem hasItem && hasItem.expected()) {
						gated.add(hasItem.itemId());
					}
				}
			}
			for (Map.Entry<String, Set<Integer>> branch : gatedItems.entrySet()) {
				if (!branch.getValue().containsAll(collectItems)
						&& !knownGaps.contains(definition.id() + "\t" + branch.getKey())) {
					violations.add("quest " + definition.id() + " branch " + branch.getKey()
						+ " reaches the reward node without verifying " + collectItems);
				}
			}
		}
		assertTrue(violations.isEmpty(),
			() -> "every reward-entry branch must verify the quest's own collected items: " + violations);
	}

    /**
	 * 既有无条件交付分支清单（逐条评审前不做全库豁免）。
	 * Recorded pre-existing ungated hand-in branches; reviewed one by one, never a quest-wide wildcard.
	 */
	private static Set<String> loadKnownHandInGaps() throws Exception {
		Set<String> known = new LinkedHashSet<>();
		try (InputStream input = QuestItemSourceContractGateTest.class
				.getResourceAsStream("/quest/quest-item-handin-baseline.tsv")) {
			assertNotNull(input, "missing /quest/quest-item-handin-baseline.tsv");
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isBlank() && !line.startsWith("#")) {
						known.add(line);
					}
				}
			}
		}
		return known;
	}

	private static Set<Integer> droppedCollectItems(QuestMetadata metadata) {
		Set<Integer> collectItems = new LinkedHashSet<>();
		for (QuestItemRequirement requirement : metadata.itemRequirements()) {
			boolean dropped = metadata.drops().stream()
				.anyMatch(drop -> drop.itemId() == requirement.itemId() && drop.chance() > 0);
			if (dropped) {
				collectItems.add(requirement.itemId());
			}
		}
		return collectItems;
	}

	@Test
	void repairedQuestsRequireTheirOwnRetailCollectItems() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		// 15010 真端 collect/check = quest_15010a 5 + quest_15010b 3（原写成 15011 的 quest_15011a 7）
		assertTurnInItemsOrRetired(catalog, 15010, Map.of(182215664, 5, 182215665, 3));
		// 15012 真端 collect/check = quest_15012a 5（原写成 15013 的 quest_15013a）
		assertTurnInItemsOrRetired(catalog, 15012, Map.of(182215667, 5));
		// 15043 真端 collect/check = quest_15043a 7（原写成 15044 的 quest_15044a 5）
		assertTurnInItemsOrRetired(catalog, 15043, Map.of(182215677, 7));
		// 15070 真端 collect/check = quest_15070a 10（原写成 15071 的 quest_15071a 1）
		assertTurnInItemsOrRetired(catalog, 15070, Map.of(182215682, 10));
		// 51021 真端 collect/check = quest_51017a 3（原写成 51018 的 quest_51018a）
		assertTurnInItemsOrRetired(catalog, 51021, Map.of(182215182, 3));
		// 1932/3547/14121/14201/24121/24152/24242：真端 COLLECT_ITEM 交付缺少 has-item，玩家可零进度领奖、
		// 掉落的任务道具永不被消耗；交付边（唯一进入 reward 的过渡）补回校验与扣除，数量取真端 collect_item 值
		assertTurnInItemsOrRetired(catalog, 1932, Map.of(182206008, 1));
		assertTurnInItemsOrRetired(catalog, 3547, Map.of(182215334, 10));
		assertTurnInItemsOrRetired(catalog, 14121, Map.of(182215479, 5));
		assertTurnInItemsOrRetired(catalog, 14201, Map.of(182215468, 1));
		assertTurnInItemsOrRetired(catalog, 24121, Map.of(182215470, 1));
		assertTurnInItemsOrRetired(catalog, 24152, Map.of(182215461, 1));
		assertTurnInItemsOrRetired(catalog, 24242, Map.of(182215583, 1));
		// 2232/2239/2289/3013/3088/4542：同一任务由多个 NPC 变体交付，17 条交付边此前完全没有条件
		// （零进度可领奖）；每个变体的交付边都必须校验并扣除自家任务道具
		assertTurnInItemsOrRetired(catalog, 2232, Map.of(182203224, 9));
		// 2239 还有一条本金交付边：真端 quest_work_item1 = quest_2239b（182203227）由 NPC 在行 1 交手，
		// 交付边必须校验并扣除它（QE-051 批次 43 的行阶梯）。
		// 2239 also owns a work-item delivery edge: retail quest_work_item1 = quest_2239b (182203227).
		assertTurnInItemsOrRetired(catalog, 2239, Map.of(182203228, 3, 182203227, 1));
		assertTurnInItemsOrRetired(catalog, 2289, Map.of(182203016, 1));
		assertTurnInItemsOrRetired(catalog, 3013, Map.of(182208008, 1));
		assertTurnInItemsOrRetired(catalog, 3088, Map.of(182208064, 1));
		assertTurnInItemsOrRetired(catalog, 4542, Map.of(182215329, 1));
		// 28836/28838：真端 SimpleTalk 行 item_check=1，quest.xml 的 collect_item/check_item 都是本任务道具，
		// 因此交付边必须校验并扣除「本任务道具 + 收集数量」。旧 XML 的 collect-item 事件形状随 XML 退役。
		// 28836/28838: the retail row marks item_check=1 and quest.xml declares the quest's own collect item,
		// so the report edge must require and remove that exact item and count.
		assertTurnInItemsOrRetired(catalog, 28836, Map.of(182213207, 50));
		assertTurnInItemsOrRetired(catalog, 28838, Map.of(182213208, 50));
		// 1870/2870/3217/4217/28739/28740：消除既有无条件交付分支，交付边必须校验并扣除自家收集物
		assertTurnInItemsOrRetired(catalog, 1870, Map.of(182215905, 4, 182215906, 4));
		assertTurnInItemsOrRetired(catalog, 2870, Map.of(182215907, 4, 182215908, 4));
		assertTurnInItemsOrRetired(catalog, 3217, Map.of(182209095, 3));
		assertTurnInItemsOrRetired(catalog, 4217, Map.of(182209110, 3));
		assertTurnInItemsOrRetired(catalog, 28739, Map.of(182215695, 5));
		assertTurnInItemsOrRetired(catalog, 28740, Map.of(182215696, 8));
		// 30756/15335/25335/19064：消除收集/制作道具与交付边的错配与漏洞，严格校验本任务真端道具
		assertTurnInItemsOrRetired(catalog, 30756, Map.of(182213266, 3));
		assertTurnInItemsOrRetired(catalog, 15335, Map.of(182215924, 1));
		assertTurnInItemsOrRetired(catalog, 25335, Map.of(182215926, 1));
		assertTurnInItemsOrRetired(catalog, 19064, Map.of(182213237, 1, 186000081, 1));
		assertTurnInItemsOrRetired(catalog, 29064, Map.of(182213239, 1, 186000085, 1));
		assertTurnInItemsOrRetired(catalog, 80291, Map.of(186000040, 5));
		assertTurnInItemsOrRetired(catalog, 80295, Map.of(186000040, 5));
		// 80955/80956：已由真端 DataDriven PVP 行接管（战场击杀 1/5 名敌对玩家），任务道具交付随
		// 旧 XML 一并退役；PVP 计数网格没有任何 has-item / 移除边，交付面必须为空。
		// 80955/80956: adopted by the retail DataDriven PVP rows (kill 1/5 enemy players in the
		// battlefield); the item turn-in retired with the old XML, and the PVP counter grid
		// carries no has-item or removal edges at all.
		assertTurnInItemsOrRetired(catalog, 80955, Map.of());
		assertTurnInItemsOrRetired(catalog, 80956, Map.of());
		assertTurnInItemsOrRetired(catalog, 50053, Map.of(186000432, 3, 162001062, 1));
		assertTurnInItemsOrRetired(catalog, 50054, Map.of(186000432, 3, 162001062, 2));
		assertTurnInItemsOrRetired(catalog, 1687, Map.of(186000035, 2, 186000036, 5));
		assertTurnInItemsOrRetired(catalog, 19010, Map.of(169405399, 1));
		assertTurnInItemsOrRetired(catalog, 19016, Map.of(169405400, 1));
		assertTurnInItemsOrRetired(catalog, 19022, Map.of(169405401, 1));
		assertTurnInItemsOrRetired(catalog, 19028, Map.of(169405403, 1));
		assertTurnInItemsOrRetired(catalog, 19034, Map.of(169405402, 1));
		assertTurnInItemsOrRetired(catalog, 4966, Map.of(182400001, 40000, 182207136, 1));
		assertTurnInItemsOrRetired(catalog, 4967, Map.of(186000091, 1, 182400001, 50000, 182207137, 1));
		assertTurnInItemsOrRetired(catalog, 4968, Map.of(186000092, 1, 182400001, 70000, 182207138, 1));
		assertTurnInItemsOrRetired(catalog, 4969, Map.of(186000093, 1, 182400001, 90000, 182207139, 1));
		assertTurnInItemsOrRetired(catalog, 15301, Map.of(182215829, 1, 182215830, 1, 182215831, 1));
		assertTurnInItemsOrRetired(catalog, 25301, Map.of(182215844, 1, 182215845, 1, 182215846, 1));
		assertTurnInItemsOrRetired(catalog, 15302, Map.of(182215832, 70));
		assertTurnInItemsOrRetired(catalog, 25302, Map.of(182215847, 70));
		assertTurnInItemsOrRetired(catalog, 15303, Map.of(152003017, 150, 182215833, 40, 182215883, 50, 182215884, 50));
		assertTurnInItemsOrRetired(catalog, 25303, Map.of(152003018, 150, 182215848, 40, 182215890, 50, 182215891, 50));
		assertTurnInItemsOrRetired(catalog, 15305, Map.of(152003019, 30, 182215887, 80));
		assertTurnInItemsOrRetired(catalog, 25305, Map.of(152003019, 30, 182215894, 80));
	}

	/**
	 * 任务自己声明、发放或汇报的道具集合。
	 * Items the quest itself declares, grants, drops or reports.
	 */
	private static Set<Integer> trackedItems(QuestDefinition definition) {
		QuestMetadata metadata = definition.metadata();
		Set<Integer> tracked = new LinkedHashSet<>();
		metadata.itemRequirements().forEach(requirement -> tracked.add(requirement.itemId()));
		metadata.inventoryItems().forEach(requirement -> tracked.add(requirement.itemId()));
		metadata.questWorkItems().forEach(requirement -> tracked.add(requirement.itemId()));
		metadata.drops().stream().filter(drop -> drop.chance() > 0).forEach(drop -> tracked.add(drop.itemId()));
		trackItemRewards(tracked, metadata.rewards());
		trackItemRewards(tracked, metadata.extendedRewards());
		trackRewardGroups(tracked, metadata.rewardGroups());
		trackRewardGroups(tracked, metadata.extendedRewardGroups());
		for (QuestTransition transition : definition.transitions()) {
			for (QuestCondition condition : transition.conditions()) {
				if (condition instanceof QuestCondition.HasItem hasItem) {
					tracked.add(hasItem.itemId());
				}
			}
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.GiveItem giveItem) {
					tracked.add(giveItem.itemId());
				}
			}
		}
		return tracked;
	}

	private static void trackRewardGroups(Set<Integer> tracked, List<QuestRewardGroup> groups) {
		groups.forEach(group -> trackItemRewards(tracked, group.rewards()));
	}

	private static void trackItemRewards(Set<Integer> tracked, List<QuestReward> rewards) {
		for (QuestReward reward : rewards) {
			String kind = reward.kind() == null ? "" : reward.kind().toUpperCase();
			if ("ITEM".equals(kind) || "ITEM_SET".equals(kind)) {
				tracked.add(reward.id());
			}
		}
	}

	/**
	 * 断言交付条件只要求给定道具，且移除动作不触碰其它道具。
	 * Asserts the turn-in conditions require exactly the given items and removals touch nothing else.
	 */
	/**
	 * 退役行（真端表驱动，无 typed 定义）跳过并要求确属退役（防名单陈旧静默缩水）——其交付门由
	 * native 车道读取的真端表列承担（lane 门覆盖）；XML 保留行仍逐条断言交付物品。
	 * Retired rows carry no typed definition: skip but require real retirement (list-rot guard); their
	 * turn-in gate is owned by the native lane reading the retail columns. XML rows still assert.
	 */
	private static void assertTurnInItemsOrRetired(QuestCatalog catalog, int questId, Map<Integer, Integer> expected) {
		if (catalog.findExecutable(questId).isEmpty()) {
			assertTrue(RetiredQuestIds.contains(questId), () -> "quest " + questId + " missing and not retired");
			return;
		}
		assertTurnInItems(catalog, questId, expected);
	}

	private static void assertTurnInItems(QuestCatalog catalog, int questId, Map<Integer, Integer> expected) {
		QuestDefinition definition = definition(catalog, questId);
		Map<Integer, Set<Integer>> required = new LinkedHashMap<>();
		Set<Integer> removed = new TreeSet<>();
		for (QuestTransition transition : definition.transitions()) {
			for (QuestCondition condition : transition.conditions()) {
				if (condition instanceof QuestCondition.HasItem hasItem && hasItem.expected()) {
					required.computeIfAbsent(hasItem.itemId(), ignored -> new TreeSet<>()).add(hasItem.count());
				}
			}
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.RemoveItem removeItem) {
					removed.add(removeItem.itemId());
				}
			}
		}
		assertEquals(expected.keySet(), required.keySet(),
			() -> "quest " + questId + " must require exactly the retail collect items");
		expected.forEach((itemId, count) -> assertEquals(Set.of(count), required.get(itemId),
			() -> "quest " + questId + " must require " + count + " of item " + itemId));
		assertTrue(expected.keySet().containsAll(removed),
			() -> "quest " + questId + " may only remove the items it requires: " + removed);
	}

	private static void assertCollectEvent(QuestCatalog catalog, int questId, int itemId, int count) {
		QuestDefinition definition = definition(catalog, questId);
		boolean matched = definition.transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.CollectItem collect
				&& collect.itemId() == itemId && collect.count() == count);
		assertTrue(matched, () -> "quest " + questId + " must watch collect-item " + itemId + " count " + count);
	}

	private static QuestDefinition definition(QuestCatalog catalog, int questId) {
		// 已退役行无 typed 定义——调用方先经 assertTurnInItemsOrRetired 跳过；此处只服务 XML 保留行。
		// Retired rows carry no typed definition and are skipped by assertTurnInItemsOrRetired; only
		// XML-retained rows reach this lookup.
		return catalog.findExecutable(questId)
			.map(CompiledQuestDefinition::definition)
			.orElseThrow(() -> new AssertionError("quest " + questId + " has no executable definition"));
	}
}
