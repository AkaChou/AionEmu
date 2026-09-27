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
	void kaligaWeaponExchangeQuestsConsumeKaligaKey() throws Exception {
		int[] kaligaQuests = {
			18618, 18619, 18620, 18621, 18622, 18623, 18624, 18625, 18626, 18627,
			18643, 18644, 18645, 18648,
			28618, 28619, 28620, 28621, 28622, 28623, 28624, 28625, 28626, 28627,
			28643, 28644, 28645, 28648
		};

		for (int questId : kaligaQuests) {
			QuestDefinition definition = load(questId).definition();
			List<QuestTransition> turnIns = definition.transitions().stream()
				.filter(t -> "started".equals(t.sourceNode()) && "reward".equals(t.targetNode()))
				.toList();
			assertFalse(turnIns.isEmpty(), "quest " + questId + " must have started -> reward transition");
			for (QuestTransition turnIn : turnIns) {
				if (turnIn.priority() != null && turnIn.priority() == 0) {
					assertTrue(turnIn.actions().contains(new QuestAction.RemoveItem(185000102, 1)),
						"quest " + questId + " must remove Kaliga key 185000102 on turn in");
				}
			}
		}
	}

	@Test
	void turnInItemConsumptionContractsAreLocked() throws Exception {
		// 11216: 德拉坎的研究 4份报告交付扣除
		assertTransitionRemovesItems(11216, "v1", "reward", Set.of(182206827, 182206828, 182206829, 182206830));

		// 3092: 观察幼龙 7个毒囊交付扣除。p0c11 真端接管后行轴改为规范 K 轴（采集行为 s1），
		// 交付仍在 s1 -> reward（dialogId 39/20002 两条，均带 CHECK 门）；遗留 XML 的节点名 step1 已不存在。
		// 3092 turn-in: after the p0c11 retail adoption the row axis is the canonical K axis (collect row s1),
		// so the hand-in stays on s1 -> reward (dialogIds 39/20002, both gated by the CHECK pair); the legacy
		// XML node name step1 is gone.
		assertTransitionRemovesItems(3092, "s1", "reward", Set.of(182208066));

		// 29064: 建筑之牙 证物交付扣除。QE-051 行阶梯（客户端两行）后交付落在 started -> s1（行 1 报告行），
		// 领奖行是 reward（var0=1）。
		// 29064 turn-in: after the QE-051 two-row ladder the hand-in lands on started -> s1 and reward owns row 1.
		assertTransitionRemovesItems(29064, "s1", "reward", Set.of(182213239));

		// 15606 & 15608 & 15613: 埃斯特拉任务收集物扣除与错扣纠正
		assertTransitionRemovesItems(15606, "s4", "reward", Set.of(182215997));
		// 15608 续片 20 由真端驱动接管（EA→采集→EA→首领）：扣除点从遗留的 reward 自环移到采集交付步
		// s1→s2（真端 quest.xml 的 check_item1_1 = quest_15608a 1 与客户端任务书第 2 行"交给
		// Canella"同判据；采集步不是末步，故落点是下一行 s2 而不是 reward）。
		// 15608 became retail-driven in slice 20: the removal moved from the legacy reward self loop
		// to the collect hand-in step s1 -> s2 (the hand-in is not the final step, so it lands on the
		// next row).
		assertTransitionRemovesItems(15608, "s1", "s2", Set.of(182215998));
		assertTransitionRemovesItems(15613, "s5", "reward", Set.of(182215999));

		// 25601 & 25605: 诺斯斯拉远征队信息与物品扣除纠正
		assertTransitionRemovesItems(25601, "s1", "s2", Set.of(182216000));
		assertTransitionRemovesItems(25605, "s1", "s2", Set.of(182216004));

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
		// 10010 & 20010: 永恒之塔主线 4 项收集物扣除
		assertTransitionRemovesItems(10010, "s1", "s2", Set.of(182216171, 182216172, 182216173, 182216174));
		assertTransitionRemovesItems(20010, "s1", "s2", Set.of(182216180, 182216181, 182216182, 182216183));

		// 14025: 进军计划书扣除
		assertTransitionRemovesItems(14025, "s1", "s2", Set.of(182215323));

		// 14046: 记忆材料扣除
		assertTransitionRemovesItems(14046, "s2", "s3", Set.of(167000323, 152000309, 182215353));

		// 14051: 调查物扣除
		assertTransitionRemovesItems(14051, "s2", "s3", Set.of(182215337, 182215338));

		// 15400 & 25400: 军团援助物资扣除
		assertTransitionRemovesItems(15400, "s3", "s4", Set.of(182215897, 182215898, 182215899));
		assertTransitionRemovesItems(25400, "s3", "s4", Set.of(182215900, 182215901, 182215902));

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

		// 1362 & 1367: 旁路交付与领奖交付分支道具扣除
		assertTransitionRemovesItems(1362, "started", "reward", Set.of(182201328, 182201329));
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
