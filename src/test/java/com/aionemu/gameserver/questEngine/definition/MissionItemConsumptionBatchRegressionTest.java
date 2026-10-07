package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定主线使命与卡里佳武器兑换任务中的任务道具扣除契约。
 * Locks the item consumption contract across campaign missions and Kaliga weapon exchange quests.
 */
class MissionItemConsumptionBatchRegressionTest {

	@Test
	void rewardWindowPreviewDoesNotPrematurelyConsumeQuestItems() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		for (int questId : List.of(24012, 24051)) {
			CompiledQuestDefinition compiled = catalog.findExecutable(questId).orElseThrow();
			for (QuestTransition t : compiled.definition().transitions()) {
				if ("reward".equals(t.sourceNode()) && "reward".equals(t.targetNode())) {
					boolean removesSpecificItem = t.actions().stream()
						.anyMatch(a -> a instanceof QuestAction.RemoveItem rm && rm.count() > 0);
					assertFalse(removesSpecificItem,
						() -> "quest " + questId + " reward preview transition must not remove specific items");
				}
			}
		}
	}

	@Test
	void kaligaWeaponExchangeQuestsConsumeKaligaKey() {
		/* P0c-13 起卡里佳兑换族整体物理退役（SimpleTalk 真端表驱动，无 IR）：编译视图断言随退役停用
		 * （退役前口径 = started -> reward 优先级 0 的交付边扣卡里佳钥匙 185000102）——守卫 = 必须确属
		 * 退役（防名单陈旧静默缩水）；真端表行的钥匙扣除口径由 native 车道门承担。 */
		/* Since P0c-13 the whole Kaliga exchange family is physically retired to the SimpleTalk lane
		 * (no IR): the compile-view assertions retire with the XML (the former caliber removed the key
		 * 185000102 on the priority-0 started -> reward hand-in); the guard keeps the list honest and
		 * the native lane gates own the caliber. */
		int[] kaligaQuests = {
			18618, 18619, 18620, 18621, 18622, 18623, 18624, 18625, 18626, 18627,
			18643, 18644, 18645, 18648,
			28618, 28619, 28620, 28621, 28622, 28623, 28624, 28625, 28626, 28627,
			28643, 28644, 28645, 28648
		};

		for (int questId : kaligaQuests) {
			assertTrue(RetiredQuestIds.contains(questId),
				() -> "quest " + questId + " is retail-driven and must be a retired row");
		}
	}

	@Test
	void turnInItemConsumptionContractsAreLocked() throws Exception {
		// 11216: 德拉坎的研究 4份报告交付扣除
		assertTransitionRemovesItems(11216, "v1", "reward", Set.of(182206827, 182206828, 182206829, 182206830));

		// 3092 & 29064: 已物理退役（SimpleTalk 真端表驱动，无 IR）——编译视图断言随退役停用，守卫 = 必须
		// 确属退役（退役前口径：3092 s1 -> reward 扣 182208066、29064 s1 -> reward 扣 182213239）；
		// 表车道口径由其 native 门承担。
		// 3092 & 29064: physically retired to the SimpleTalk lane (no IR) — the compile-view assertions
		// retire with the XML; the guard keeps the rows honest.
		for (int questId : List.of(3092, 29064)) {
			assertTrue(RetiredQuestIds.contains(questId),
				() -> "quest " + questId + " is retail-driven and must be a retired row");
		}

		// 15606: 埃斯特拉任务收集物扣除（XML 仍在库，保留编译视图断言）。
		assertTransitionRemovesItems(15606, "s4", "reward", Set.of(182215997));
		// 15608 & 15613 & 25601 & 25605: 已物理退役（DataDriven 真端表驱动，无 IR）——编译视图断言随退役
		// 停用，守卫 = 必须确属退役（退役前口径：15608 s1 -> s2 扣 182215998、15613 s5 -> reward 扣 182215999、
		// 25601 s1 -> s2 扣 182216000、25605 s1 -> s2 扣 182216004）；表车道口径由其 native 门承担。
		// 15608/15613/25601/25605: physically retired to the DataDriven lane (no IR) — the compile-view
		// assertions retire with the XML; the guard keeps the rows honest.
		for (int questId : List.of(15608, 15613, 25601, 25605)) {
			assertTrue(RetiredQuestIds.contains(questId),
				() -> "quest " + questId + " is retail-driven and must be a retired row");
		}

		// 1573 & 1636: 毒囊与调查物错扣纠正
		assertTransitionRemovesItems(1573, "v1", "v2", Set.of(182201734));
		assertTransitionRemovesItems(1636, "v1", "v2", Set.of(182201786));

		// 2333: 调查材料交付扣除
		assertTransitionRemovesItems(2333, "started", "v1", Set.of(182204131));

		// 19000: 3种精华交付扣除
		assertTransitionRemovesItems(19000, "s1", "reward", Set.of(152003004, 152003005, 152003006));

		// 17540: 收集物交付扣除
		assertTransitionRemovesItems(17540, "s4", "reward", Set.of(182216159));

		// 18511: 工作物转换前置道具扣除
		assertTransitionRemovesItems(18511, "started", "started", Set.of(182212010));

		// 27540 & 28511: legacy collect_items are consumed when the turn-in check succeeds.
		// 27540、28511：legacy collect_items 交付检查成功时扣除。
		assertTransitionRemovesItems(27540, "s4", "reward", Set.of(182216160));
		assertTransitionRemovesItems(28511, "started", "started", Set.of(182212022));
	}

	@Test
	void campaignMissionsConsumeRequiredCollectionItems() throws Exception {
		// 10010 & 20010: 永恒之塔主线 4 项收集物扣除——已物理退役（DataDriven 真端表驱动，无 IR），
		// 编译视图断言随退役停用，守卫 = 必须确属退役；表车道口径由 native 门承担。
		// 10010 & 20010: physically retired to the DataDriven lane (no IR); the guard keeps the rows honest.
		for (int questId : List.of(10010, 20010)) {
			assertTrue(RetiredQuestIds.contains(questId),
				() -> "quest " + questId + " is retail-driven and must be a retired row");
		}

		// 14025: 进军计划书扣除
		assertTransitionRemovesItems(14025, "s1", "s2", Set.of(182215323));

		// 14046: 记忆材料扣除
		assertTransitionRemovesItems(14046, "s2", "s3", Set.of(167000323, 152000309, 182215353));

		// 14051: 调查物扣除
		assertTransitionRemovesItems(14051, "s2", "s3", Set.of(182215337, 182215338));

		// 15400 & 25400: 军团援助物资扣除——已物理退役（DataDriven 真端表驱动，无 IR），同前守卫。
		// 15400 & 25400: physically retired to the DataDriven lane (no IR); the guard keeps the rows honest.
		for (int questId : List.of(15400, 25400)) {
			assertTrue(RetiredQuestIds.contains(questId),
				() -> "quest " + questId + " is retail-driven and must be a retired row");
		}

		// 20110: 紧急情报扣除
		assertTransitionRemovesItems(20110, "s1", "s2", Set.of(182216233, 182216234, 182216235, 182216236));

		// 20112: 支援任务道具扣除
		assertTransitionRemovesItems(20112, "s4", "reward", Set.of(182216244));

		// 20525: 调查材料扣除
		assertTransitionRemovesItems(20525, "s4", "s5", Set.of(182216081, 182216082, 182216083));

		// 20529: 结界石材料扣除。QE-051 折叠步骤链拆分后，交付行 s10（var0=10）与领奖行 reward（var0=11）分离，
		// 材料扣除落在 s9 -> s10。
		// 20529: the folded journal chain now separates the report row s10 from the reward row, so the item
		// removal sits on s9 -> s10.
		assertTransitionRemovesItems(20529, "s9", "s10", Set.of(182216090, 182216091, 182216092));

		// 24030: 命运决战证物扣除
		assertTransitionRemovesItems(24030, "s3", "s4", Set.of(182215391));

		// 2001 ~ 2006: 伊夏尔根新手使命收集物扣除
		assertTransitionRemovesItems(2001, "v1", "v2", Set.of(182203002));
		assertTransitionRemovesItems(2002, "s11", "s12", Set.of(182203003));
		assertTransitionRemovesItems(2003, "v1", "reward", Set.of(182203004));
		assertTransitionRemovesItems(2004, "v1", "v2", Set.of(182203005));
		assertTransitionRemovesItems(2005, "v1", "reward", Set.of(182203006));
		assertTransitionRemovesItems(2006, "v1", "reward", Set.of(182203008));

		// 1922 & 2947: 奥德提取装置领奖扣除
		assertTransitionRemovesItems(1922, "reward", "complete", Set.of(182206030));
		assertTransitionRemovesItems(2947, "s9", "complete", Set.of(182207037));

		// 1362: 旁路交付扣除——已物理退役（SimpleTalk 真端表驱动，无 IR），同前守卫（退役前口径
		// started -> reward 扣 182201328/182201329）。1367 仍在库，保留编译视图断言。
		// 1362: physically retired to the SimpleTalk lane (no IR); the guard keeps the row honest.
		assertTrue(RetiredQuestIds.contains(1362),
			"quest 1362 is retail-driven and must be a retired row");
		// 1367 由 94636797a 拆成三个带材料条件的交付分支（reward0/1/2），每个分支扣除整套收集物。
		// 94636797a split 1367 into three material-guarded delivery branches (reward0/1/2); each branch
		// consumes the full collection set, replacing the former unconditional started -> reward route.
		for (String rewardNode : List.of("reward0", "reward1", "reward2")) {
			assertTransitionRemovesItems(1367, "started", rewardNode, Set.of(182201331, 182201332, 182201333));
		}
	}

	private static void assertTransitionRemovesItems(int questId, String source, String target,
			Set<Integer> expectedRemovedItemIds) throws Exception {
		QuestDefinition definition = load(questId).definition();
		List<QuestTransition> transitions = definition.transitions().stream()
			.filter(t -> source.equals(t.sourceNode()) && target.equals(t.targetNode()))
			.toList();
		assertFalse(transitions.isEmpty(), "quest " + questId + " missing transition " + source + " -> " + target);

		Set<Integer> removedInTransitions = transitions.stream()
			.flatMap(t -> t.actions().stream())
			.filter(QuestAction.RemoveItem.class::isInstance)
			.map(a -> ((QuestAction.RemoveItem) a).itemId())
			.collect(Collectors.toSet());

		assertTrue(removedInTransitions.containsAll(expectedRemovedItemIds),
			"quest " + questId + " transition " + source + " -> " + target
				+ " must remove items " + expectedRemovedItemIds + ", but removed " + removedInTransitions);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
}
