package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 1192「贝尔特伦要塞的支援请求」的工作物品契约，并审计全部生产任务
 * 不得丢失 legacy {@code quest_work_items} 的迁移。
 * Locks quest 1192's work-item contract and audits that every production
 * definition preserves its legacy {@code quest_work_items} migration.
 * <p>权威来源是 {@code quest_data.xml} 的 {@code <quest_work_items>}。旧引擎
 * {@code QuestService.setFinishingState} 在完成时无条件清理这些物品；typed
 * 引擎只在 metadata 声明了 {@code <work-items>} 时执行同样的清理
 * （{@code QuestMutationPlanner.appendCompletionWorkItemCleanup}）。迁移时漏掉该
 * 声明、又没有任何显式 {@code remove-item} 覆盖该物品时，任务完成后道具必然残留在背包，
 * 1192 的 182200556 即为此形态。
 * The authority is {@code quest_data.xml}'s {@code <quest_work_items>}. The legacy
 * engine unconditionally cleared these items on completion; the typed engine only
 * mirrors that when the metadata declares {@code <work-items>}. A definition that
 * dropped the declaration and carries no explicit {@code remove-item} for the item
 * necessarily leaves it in the inventory, which is quest 1192's 182200556.</p>
 * <p>quest_data.xml 未声明 work item、但旧 handler 在 use-item 中消费的道理由
 * {@link QuestUseItemRewardCleanupGateTest} 覆盖；两道门禁共同覆盖两种消费语义来源。</p>
 * Items consumed by a legacy {@code use-item} handler without a quest_data work-item declaration
 * are covered by {@link QuestUseItemRewardCleanupGateTest}; the two gates cover both authorities.</p>
 */
class QuestWorkItemMigrationCoverageTest {
	private static final int QUEST_1192 = 1192;
	private static final int WORK_ITEM_1192 = 182200556;

	/**
	 * 证据冲突, 需人工裁定, 不纳入自动断言:旧 handler 完成时移除 186000085(收集物),
	 * quest_data.xml 声明 work item 为 182207122,而当前 XML 声明 6 个 152206* 分支图纸
	 * (sN->s7 时 give-item 发放)。三者互不相同,无法据此自动收敛。
	 * Conflicting evidence pending human adjudication; excluded from the automated assertion.
	 */
	private static final Set<Integer> EVIDENCE_CONFLICT_QUESTS = Set.of(4942);

	@Test
	void verteronReinforcementsDeclaresItsWorkItemAndTurnsItInAtLavirintos() {
		// P3 重锚（计划 §8.9）：1192 自 SimpleTalk 切换批起由 native 车道直驱，旧断言（typed metadata 的
		// {@code <work-items>} 声明 + QUEST_ACCEPT_1/SETPRO1 边）属 IR 形状，改锚原版表行的两条物品通道。
		// 原版事实不变：接取发工作物品、首步 Lavirintos(203701) 回收同一物品、第 2 步 Xenophon(203833)。
		// P3 re-anchor (plan §8.9): quest 1192 is native-lane driven since the SimpleTalk switch batch, so
		// the IR-shape assertions are replaced by the retail row's two item channels.
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals(203098, handler.acquireNpc(QUEST_1192), "原版 acquired_npc_name = Spatalos");
		assertEquals(203098, handler.rewardNpc(QUEST_1192), "原版 reward_npc_name = Spatalos");
		assertEquals(2, handler.relayCount(QUEST_1192), "原版行有 talk_npc1(Lavirintos)/talk_npc2(Xenophon)");
		// 接取发放工作物品：与 quest.xml 的 work item 声明同物（182200556）。
		// Accept grants the work item, the same item quest.xml declares (182200556).
		assertEquals(new SimpleTalkHandler.ItemStack(WORK_ITEM_1192, 1), handler.acceptGiveItem(QUEST_1192));
		// 首步交出：原版 remove_item1 = ITEM_DOC_QUEST_1192A 1，落在 Lavirintos 步上。
		// First-step hand-over: remove_item1 lands on the Lavirintos step.
		assertTrue(handler.relaysForNpc(203701).contains(new SimpleTalkHandler.RelayStep(QUEST_1192, 1, 203701)),
			"1192 的首步必须挂在 Lavirintos(203701)");
		assertEquals(new SimpleTalkHandler.ItemStack(WORK_ITEM_1192, 1), handler.stepRemoveItem(QUEST_1192, 1));
		assertNull(handler.stepRemoveItem(QUEST_1192, 2), "第 2 步不得重复回收工作物品");
	}

	@Test
	void everyExecutableWorkItemQuestIsCleanedUpOnCompletion() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		Map<Integer, List<Integer>> legacyWorkItems = legacyWorkItems();

		List<String> omitted = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> entry : legacyWorkItems.entrySet()) {
			int questId = entry.getKey();
			var executable = catalog.findExecutable(questId);
			if (executable.isEmpty() || EVIDENCE_CONFLICT_QUESTS.contains(questId)) {
				continue;
			}
			// legacy 的顺序差异不构成契约差异, 按集合比较。
			List<Integer> declared = executable.orElseThrow().definition().metadata()
				.questWorkItems().stream().map(QuestItemRequirement::itemId).toList();
			List<Integer> missing = entry.getValue().stream()
				.filter(itemId -> !declared.contains(itemId)).toList();
			if (!missing.isEmpty()) {
				omitted.add(questId + " legacy=" + entry.getValue()
					+ " declared=" + declared + " missing=" + missing);
			}
		}

		assertEquals(List.of(), omitted,
			"the following quests declare <work-items> that omit legacy quest_work_items;"
				+ " completion then leaves those items in the inventory");
	}

	@Test
	void everyItemEnteringRewardIsHandedInOrDeclaredAsAWorkItem() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> defects = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> entry : legacyWorkItems().entrySet()) {
			int questId = entry.getKey();
			var executable = catalog.findExecutable(questId);
			if (executable.isEmpty()) {
				continue;
			}
			QuestDefinition definition = executable.orElseThrow().definition();
			if (!definition.metadata().questWorkItems().isEmpty()) {
				// 引擎在完成/放弃时清理声明的 work items, 不依赖单条路由。
				continue;
			}
			// 未声明时, 每一条真正进入 REWARD 的路由都必须自己交出物品:
			// 1192 的 182200556 正是漏在这一条上。
			Map<String, QuestStatus> statuses = definition.nodes().stream().collect(Collectors
				.toMap(QuestNode::label, node -> node.projection().status(), (left, right) -> left));
			for (QuestTransition transition : definition.transitions()) {
				if (statuses.get(transition.targetNode()) != QuestStatus.REWARD
						|| statuses.get(transition.sourceNode()) == QuestStatus.REWARD) {
					continue;
				}
				List<Integer> notHandedIn = entry.getValue().stream()
					.filter(itemId -> transition.actions().stream().noneMatch(action ->
						action instanceof QuestAction.RemoveItem remove && remove.itemId() == itemId))
					.toList();
				if (!notHandedIn.isEmpty()) {
					defects.add(questId + " " + transition.sourceNode() + "->REWARD keeps " + notHandedIn);
				}
			}
		}
		assertEquals(List.of(), defects,
			"these quests neither declare <work-items> nor hand the item in on every REWARD entry;"
				+ " the completing player keeps the item");
	}

	private static Map<Integer, List<Integer>> legacyWorkItems() {
		Map<Integer, List<Integer>> result = new LinkedHashMap<>();
		for (QuestCatalogEntry entry : ProductionQuestDefinitions.catalog().entries()) {
			List<QuestItemRequirement> items = entry.metadata().questWorkItems();
			if (!items.isEmpty()) {
				result.put(entry.id(), items.stream().map(QuestItemRequirement::itemId).toList());
			}
		}
		return result;
	}

	private static QuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definition(questId).definition();
	}
}
