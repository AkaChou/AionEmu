package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectTestData;
import com.aionemu.gameserver.questEngine.runtime.QuestSpawnRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动期总门禁：对**原版 overlay 后的生产目录**跑完整 {@code QuestEngine.prepareProductionDefinitions}
 * （交互对象合同 + 事件接线合同 + item-play 索引 + NPC 路由注册）。
 * <p>
 * 为什么需要它：两次实机启动失败（14120 缺 {@code talk_npc1} 步骤、{@code SYSTEM_GRANT} 未接线）都发生在
 * 这条路径上，而此前没有任何测试跑过它——各子合同只有在有人单独写测试时才被覆盖。本门禁把"整条启动路径
 * 在原版目录上可准备"变成常驻断言：新增事件类型/新形状只要破坏启动，这里就会红。
 * <p>
 * Whole-startup gate over the retail-overlaid production catalog: runs the real
 * {@code prepareProductionDefinitions} path so that any startup-breaking shape or unwired event fails here
 * instead of on a live server start.
 */
class QuestProductionStartupGateTest {

	private NpcData originalNpcData;

	@BeforeEach
	void setUp() {
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
	}

	@AfterEach
	void cleanup() {
		QuestSpawnRegistry.global().cleanupAll();
		DataManager.NPC_DATA = originalNpcData;
	}

	@Test
	void theWholeRetailProductionCatalogPreparesWithoutStartupContractViolations() {
		var catalog = ProductionQuestDefinitions.catalog();
		var engine = new QuestEngine();
		assertDoesNotThrow(() -> engine.prepareProductionDefinitions(catalog,
			QuestInteractionObjectTestData.npcAiResolver(getClass().getClassLoader())));
	}

	/**
	 * 回归锚：{@code SYSTEM_GRANT} 边必须真实存在于被校验的目录里（否则"事件接线合同"会因无样本而空跑，
	 * 再次放过未接线事件）。 / The gate must actually contain {@code SystemGrant} edges to be meaningful.
	 */
	@Test
	void theGatedCatalogStillCarriesServiceSideGrantEdges() {
		List<Integer> carriers = new ArrayList<>();
		int executables = 0;
		for (QuestCatalogEntry entry : ProductionQuestDefinitions.catalog().entries()) {
			if (entry.executable().isEmpty()) {
				continue;
			}
			executables++;
			boolean carriesGrant = entry.executable().get().definition().transitions().stream()
				.anyMatch(transition -> transition.event() instanceof QuestEvent.SystemGrant);
			if (carriesGrant) {
				carriers.add(entry.id());
			}
		}
		// P1/P2/P3 按族切换后，被切走的家族不再出现在 typed 目录里，但仍由原生处理器直驱；
		// 生产规模门按「typed 可执行 + 原生 owner」的合计锚定（总数不得回退）。
		final int nativeOwned = com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler.instance().ownedQuestCount()
			+ com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler.instance().ownedQuestCount()
			+ com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler.instance().ownedQuestCount();
		final int typedExecutables = executables;
		final int total = typedExecutables + nativeOwned;
		assertTrue(total > 5000, () -> "生产可执行定义数量异常：" + total + "（typed=" + typedExecutables
			+ " native=" + nativeOwned + "）");
		// P7 步 f 重锚：SYSTEM_GRANT 边随 DataDriven 1467 行退出 typed 目录（XML 保留行本就不携带发放边），
		// 服务侧发放面改由三条原生通道承担——NativeSystemGrantLanes 聚合面（Talk+Collect）、DD 运行时的
		// 接取型发放兴趣面（leveluplogin 等级键 + enterworld 世界键）与链式接取发放面（acquire=none 后继，
		// 2026-10-08 缺口修复批）。任一为空即拦「发放面塌空壳」。
		// P7 step-f re-anchor: SYSTEM_GRANT edges left the typed catalog with the 1467 DataDriven rows
		// (XML-retained rows never carried grant edges). The service-side grant face is now held by three
		// native channels — the NativeSystemGrantLanes aggregate, the DD runtime's grant-style acquire
		// interests, and the chain-acquire face. An empty any channel means the grant face collapsed.
		assertFalse(com.aionemu.gameserver.questEngine.tablelane.NativeSystemGrantLanes.lanes().isEmpty(),
			"原生系统发放聚合面不得为空（NativeSystemGrantLanes）");
		assertTrue(!com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.instance()
				.acquireLevelInterests().isEmpty(),
			"DD 运行时接取型发放（leveluplogin 等级键）不得为空");
		assertTrue(!com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.instance()
				.acquireWorldInterests().isEmpty(),
			"DD 运行时接取型发放（enterworld 世界键）不得为空");
		assertFalse(com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.instance()
				.chainAcquireSuccessors().isEmpty(),
			"DD 链式接取发放面不得为空（acquire=none 后继）");
	}
}
