package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 防止“任务发放道具 -> 使用道具 -> 进入 REWARD”丢失任务侧扣除。
 * Prevents a quest-granted item from surviving a use-item route into REWARD.
 *
 * <p>旧的 work-item 审计只以 {@code quest_data.xml} 的 {@code <quest_work_items>} 为宇宙；
 * 10100/20100 这类由 {@code give-item} 发放、旧 handler 用
 * {@code useQuestItem(..., true)} 消耗、但 quest_data 未声明 work item 的道具会漏掉。
 * 本门禁直接检查编译后 IR 的 item lifecycle，不依赖 quest_data 声明。</p>
 * The old work-item audit only covered quests declared in
 * {@code quest_data.xml}; this gate checks the compiled item lifecycle directly.</p>
 *
 * <p>边界：只检查 {@code give-item} 发放并由 {@code use-item} 进入 REWARD 的道具；
 * 普通收集物和掉落物的交付扣除仍由 collect-item/collect-turn-in 门禁覆盖。</p>
 * Boundary: this gate only covers quest-granted items used to enter REWARD; collect-item and drop
 * turn-in removal remain covered by the collection gates.</p>
 */
class QuestUseItemRewardCleanupGateTest {
	/**
	 * 18738/28738 的 164000342 由 {@code skilluse} 模板链处理，旧 handler 也不在
	 * use 时调用 removeQuestItem；这是有证据的非任务侧扣除例外。
	 * 18738/28738 intentionally leave 164000342 to their skill-use lifecycle.
	 */
	private static final Set<String> EVIDENCE_BACKED_NON_CONSUMERS = Set.of(
		"18738:164000342",
		"28738:164000342");

	@Test
	void everyQuestGrantedUseItemRewardRouteCleansUpItsItem() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> defects = new ArrayList<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			Map<String, QuestStatus> statuses = definition.nodes().stream()
				.collect(Collectors.toMap(QuestNode::label, node -> node.projection().status()));
			Set<Integer> grantedItems = definition.transitions().stream()
				.flatMap(transition -> transition.actions().stream())
				.filter(QuestAction.GiveItem.class::isInstance)
				.map(action -> ((QuestAction.GiveItem) action).itemId())
				.collect(Collectors.toSet());
			Set<Integer> workItems = definition.metadata().questWorkItems().stream()
				.map(QuestItemRequirement::itemId)
				.collect(Collectors.toSet());

			for (QuestTransition transition : definition.transitions()) {
				if (!(transition.event() instanceof QuestEvent.UseItem use)) {
					continue;
				}
				int itemId = use.itemId();
				if (!grantedItems.contains(itemId)
						|| statuses.get(transition.targetNode()) != QuestStatus.REWARD
						|| statuses.get(transition.sourceNode()) == QuestStatus.REWARD) {
					continue;
				}
				boolean routeRemoval = transition.actions().stream().anyMatch(action ->
					action instanceof QuestAction.RemoveItem remove && remove.itemId() == itemId);
				boolean workItemCleanup = workItems.contains(itemId);
				boolean completionCleanup = completionRoutesCleanup(definition, statuses, itemId);
				if (!routeRemoval && !workItemCleanup && !completionCleanup
						&& !EVIDENCE_BACKED_NON_CONSUMERS.contains(compiled.id() + ":" + itemId)) {
					defects.add(compiled.id() + " " + transition.sourceNode() + "->"
						+ transition.targetNode() + " item=" + itemId);
				}
			}
		}
		assertEquals(List.of(), defects,
			"quest-granted use-item routes into REWARD must remove the item, declare it as a work item,"
				+ " or remove it on every reward->complete route; otherwise the completing player keeps it");
	}

	private static boolean completionRoutesCleanup(QuestDefinition definition,
			Map<String, QuestStatus> statuses, int itemId) {
		List<QuestTransition> completionRoutes = definition.transitions().stream()
			.filter(transition -> statuses.get(transition.sourceNode()) == QuestStatus.REWARD)
			.filter(transition -> statuses.get(transition.targetNode()) == QuestStatus.COMPLETE)
			.toList();
		return !completionRoutes.isEmpty() && completionRoutes.stream().allMatch(transition ->
			transition.actions().stream().anyMatch(action ->
				action instanceof QuestAction.RemoveItem remove && remove.itemId() == itemId));
	}
}
