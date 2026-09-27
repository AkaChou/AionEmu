package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动期 {@link QuestInteractionObjectValidator} 的全量生产门禁（覆盖 NPC AI 解析面）。
 * <p>
 * 该合同在生产启动时执行（{@code QuestEngine.prepareProductionDefinitions}），但此前没有测试覆盖，
 * 导致 quest 14120（SimpleCollectItem 退役后缺 {@code talk_npc1} 步骤）在 T3 全绿的情况下让服务端起不来。
 * 本门禁对**全部可执行定义**逐个跑校验器并把失败聚合上报（NPC AI 走共享的
 * {@link QuestInteractionObjectTestData}，与生产 {@code DataManager.NPC_DATA} 同口径），
 * 并锁定 14120/14150 的中间 NPC 步骤形状。
 * <p>
 * Whole-production gate for the startup interaction-object contract; it also locks the retail
 * {@code talk_npc1} step of 14120 (the quest whose missing step blocked server startup).
 */
class QuestInteractionObjectContractGateTest {

	private static final List<Integer> TALK_STEP_QUESTS = List.of(14120, 14150);

	@Test
	void everyProductionDefinitionSatisfiesTheStartupInteractionObjectContract() throws Exception {
		IntFunction<String> aiByTemplate = QuestInteractionObjectTestData.npcAiResolver(getClass().getClassLoader());
		Map<Integer, String> failures = new TreeMap<>();
		int checked = 0;
		for (QuestCatalogEntry entry : ProductionQuestDefinitions.catalog().entries()) {
			if (entry.executable().isEmpty()) {
				continue;
			}
			checked++;
			CompiledQuestDefinition definition = entry.executable().get();
			try {
				QuestInteractionObjectValidator.validateDefinition(definition, aiByTemplate);
			} catch (IllegalStateException e) {
				failures.put(entry.id(), e.getMessage());
			}
		}
		int total = checked;
		assertTrue(total > 5000, () -> "生产可执行定义数量异常：" + total);
		assertEquals(Map.of(), failures, () -> "启动期交互对象合同失败 "
			+ failures.size() + " 例：" + failures);
	}

	/**
	 * 14120/14150：真端 {@code talk_npc1} 步骤必须存在——先与中间 NPC 对话推进到采集行，
	 * 采集对象的 {@code ACTION_ITEM_USE} 才能落在掉落生效步（{@code collect_progress}=1）上。
	 * P0-2 规范形：推进 = 中间 NPC 的 QUEST_SELECT 一步直达（select2 页链与末按钮 SETPRO1 删除）。
	 * The retail talk step must exist so the object-use route lands on the drop's collecting step.
	 * Canonical since P0-2: the advancement is the mid NPC's QUEST_SELECT in one step (the select2
	 * page chain and its terminal SETPRO1 button are gone).
	 */
	@Test
	void collectItemTalkStepsLandOnTheirCollectingStep() {
		Map<Integer, Integer> talkNpcs = Map.of(14120, 730020, 14150, 204582);
		for (int questId : TALK_STEP_QUESTS) {
			CompiledQuestDefinition compiled = ProductionQuestDefinitions.definition(questId);
			int talkNpc = talkNpcs.get(questId);
			boolean hasTalkStep = compiled.definition().transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == talkNpc && talk.dialogId() != null
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					&& "started".equals(transition.sourceNode()) && "v1".equals(transition.targetNode()));
			assertTrue(hasTalkStep, () -> "quest " + questId + " 必须由 talk_npc1=" + talkNpc
				+ " 的 QUEST_SELECT 一步推进到 v1");
			for (QuestTransition transition : compiled.definition().transitions()) {
				if (!(transition.event() instanceof QuestEvent.CanAct(int templateId, String actionType))
						|| !"ACTION_ITEM_USE".equals(actionType)) {
					continue;
				}
				QuestNode source = compiled.definition().nodes().stream()
					.filter(node -> Objects.equals(node.label(), transition.sourceNode()))
					.findFirst().orElseThrow();
				assertEquals(1, source.projection().variables().get("var0"),
					() -> "quest " + questId + " 的 ACTION_ITEM_USE 必须落在采集行 var0=1（模板 "
						+ templateId + "）");
			}
		}
	}
}
