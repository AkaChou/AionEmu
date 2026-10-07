package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		// 下限随 owner 迁移下移：SimpleHunt(939)/SimpleSerialHunt(16)/SimpleTalk(3152)/SimpleCollectItem(177)
		// 四族切到 native 后退出 typed 目录，可执行定义数从 P2 的 5000+ 降到 P3 的 3044、P4 的 2867，
		// P5 再切 SimpleUseItem/SimpleItemPlay 两族后为 2757；P6 切 CombineTask(574) 后为 2183；
		// P7 步 f 切 DataDriven(1467) 后 typed 目录只剩 XML 保留行，实测 714——下限 700 仍能拦住
		// 「目录塌成空壳」，native 行的启动合同由各家族门与 DataDrivenNativeRuntimeGateTest 承担。
		// The floor follows the owner migration: the four families switched to the native lane left the
		// typed directory, taking the executable count from 5000+ (P2) to 3044 (P3) and 2867 (P4); the
		// P5 switch of SimpleUseItem/SimpleItemPlay brings it to 2757, and the P6 CombineTask switch (574)
		// to 2183; the P7 step-f DataDriven switch (1467) leaves only the XML-retained rows (observed 714)
		// — 700 still catches a collapse into an empty shell, and native startup contracts are held by
		// the family gates and DataDrivenNativeRuntimeGateTest.
		assertTrue(total > 700, () -> "生产可执行定义数量异常：" + total);
		assertEquals(Map.of(), failures, () -> "启动期交互对象合同失败 "
			+ failures.size() + " 例：" + failures);
	}

	/**
	 * 14120/14150：真端 {@code talk_npc1} 中继步必须在 native 车道上成立——中继链未走完时采集对象零推进，
	 * 与该中间 NPC 对话推进后对象才生效（真端 {@code collect_progress}=1 的直读语义）。
	 * <p>
	 * 该步曾因缺失让服务端起不来（typed 时代的 {@code ACTION_ITEM_USE} 掉落步合同）；P4 起两行由
	 * {@link SimpleCollectItemHandler} 原生直驱，合同改由 native 链路本身承担。
	 * The retail talk step must hold on the native lane: the collect object stays a no-op until the mid
	 * NPC has been talked to. The two rows were the ones whose missing step blocked server startup.
	 */
	@Test
	void collectItemTalkStepsGateTheirCollectingStepOnTheNativeLane() {
		Map<Integer, Integer> talkNpcs = Map.of(14120, 730020, 14150, 204582);
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		for (int questId : TALK_STEP_QUESTS) {
			List<Integer> relays = handler.relayNpcs(questId);
			assertFalse(relays.isEmpty(), () -> "quest " + questId + " 必须装载真端 talk_npc1 中继步");
			assertEquals(talkNpcs.get(questId), relays.getFirst(),
				() -> "quest " + questId + " 的 talk_npc1 必须解析为静态数据 id");
			assertTrue(handler.routes(questId), () -> "quest " + questId + " 必须由 native 车道路由");

			var player = NativeTalkFixture.player();
			NativeTalkFixture.start(player, questId);
			int objectNpc = handler.collectObjects(questId).getFirst();
			assertFalse(handler.onObjectUse(player, questId, objectNpc),
				() -> "quest " + questId + " 中继链未走完时采集对象不得推进");
			// 任务行打开只下发该步页、不推进；SETPRO1（真端 cabb10）才推进链条。
			// The row selection only opens the step page; SETPRO1 advances the chain.
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), questId, 26)),
				() -> "quest " + questId + " 任务行必须下发该步页");
			assertFalse(handler.onObjectUse(player, questId, objectNpc),
				() -> "quest " + questId + " 打开步页不得推进链条");
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), questId, 10000)),
				() -> "quest " + questId + " SETPRO1 必须推进链条");
			assertTrue(handler.onObjectUse(player, questId, objectNpc),
				() -> "quest " + questId + " 中继链走完后采集对象必须推进（采集行生效）");
		}
	}
}
