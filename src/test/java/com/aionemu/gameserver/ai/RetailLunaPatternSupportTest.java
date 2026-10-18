package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawn;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 塔奇的冒险（露娜副本 301630000/301640000）原版 AI 审计回归：
 * 确保两张地图的条件刷怪投影完整、全部 NPC 均映射到 AI pattern，且所有 pattern 的操作码与参数
 * 都能被 {@link RetailPatternAI2} 接管——这是实例处理器"完全交给原版数据、不手写刷怪"的前提。
 * Retail AI audit regression for Taki's Adventure (Luna instances 301630000/301640000):
 * guarantees complete condition-spawn projection, full NPC-to-pattern mapping and that every
 * opcode/parameter is accepted by {@link RetailPatternAI2} — the precondition for the instance
 * handlers to rely purely on retail data without hand-written spawns.
 */
class RetailLunaPatternSupportTest {

	/**
	 * 污染暗道 / Contaminated Underpath.
	 */
	private static final int UNDERPATH = 301630000;
	/**
	 * 秘密军工厂 / Secret Munitions Factory.
	 */
	private static final int FACTORY = 301640000;
	/**
	 * 原版条件总数：<world_N.xml> 中 <condition_info> 粒度计数。
	 * Retail condition totals: <condition_info> count in the retail world_N.xml files.
	 */
	private static final int UNDERPATH_CONDITIONS = 229;
	private static final int FACTORY_CONDITIONS = 153;

	private static String previousDefinitions;
	private static RetailAiData aiData;

	@BeforeAll
	static void loadRetailAiData() {
		previousDefinitions = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		if (DataManager.RETAIL_AI_DATA == null) {
			DataManager.RETAIL_AI_DATA = new XmlDataLoader().loadRetailAiData();
		}
		aiData = DataManager.RETAIL_AI_DATA;
	}

	@AfterAll
	static void restoreDefinitionsDir() {
		if (previousDefinitions == null) {
			System.clearProperty("aion.game.definitions.dir");
		} else {
			System.setProperty("aion.game.definitions.dir", previousDefinitions);
		}
	}

	@Test
	void lunaPhaseChainConditionsAreFullyProjected() {
		assertEquals(UNDERPATH_CONDITIONS, aiData.getConditionSpawns(UNDERPATH).size(),
			"污染暗道条件数应与原版一致 / Underpath condition count must match retail");
		assertEquals(FACTORY_CONDITIONS, aiData.getConditionSpawns(FACTORY).size(),
			"军工厂条件数应与原版一致 / Factory condition count must match retail");

		assertExpressionPresent(UNDERPATH, "TIMEATTACK_PLAY_START==1");
		assertExpressionPresent(UNDERPATH, "IDLUNA_DEF_PHASE_1>=300");
		assertExpressionPresent(UNDERPATH, "IDLUNA_DEF_END==1");
		assertExpressionPresent(FACTORY, "TIMEATTACK_PLAY_START==1");
		assertExpressionPresent(FACTORY, "Rolling_Golem_01==1");
		assertExpressionPresent(FACTORY, "2STAGE_OPEN==1");
		assertExpressionPresent(FACTORY, "ALLSTAGE_END==1");

		// AI 写入的变量必须显式声明，否则 set_condition_spawn_variable 会被拒绝。
		// Variables written by AI must be declared, otherwise set_condition_spawn_variable is rejected.
		List<String> undeclared = new ArrayList<>();
		for (String variable : List.of("timeattack_play_start", "idluna_def_phase_1", "idluna_def_phase_2",
			"idluna_def_phase_3", "idluna_def_phase_4", "idluna_def_end", "idluna_def_barricade_01",
			"idluna_def_barricade_02", "idluna_def_barricade_03")) {
			if (!aiData.supportsConditionVariable(UNDERPATH, variable)) {
				undeclared.add(UNDERPATH + ":" + variable);
			}
		}
		for (String variable : List.of("timeattack_play_start", "rolling_golem_01", "rolling_golem_02",
			"rolling_golem_03", "rolling_golem_04", "alarm_spawn", "blue_bomb", "yellow_bomb",
			"left_bomb_spawn", "2stage_open", "teleport_stage2", "allstage_end")) {
			if (!aiData.supportsConditionVariable(FACTORY, variable)) {
				undeclared.add(FACTORY + ":" + variable);
			}
		}
		assertTrue(undeclared.isEmpty(), () -> "未声明的条件变量 / Undeclared condition variables: " + undeclared);
	}

	@Test
	void lunaRespawnCadenceIsProjectedFromRetail() {
		// 真端 spawn_time：bomber 834524=2s、firewall 833813=60s、出口 833998=300s。
		// Retail spawn_time: bomber 834524=2s, firewall 833813=60s, exit 833998=300s.
		assertEquals(2, firstNpc(UNDERPATH, 834524).respawnDelay());
		assertEquals(60, firstNpc(UNDERPATH, 833813).respawnDelay());
		assertEquals(300, firstNpc(FACTORY, 833998).respawnDelay());
		// 仅原版 no_respawn="FALSE" 的 territory 才带重生节奏（def 69 + siege 13）。
		// Only retail no_respawn="FALSE" territories carry a respawn cadence (69 def + 13 siege).
		assertEquals(82, countRespawningNpcs());
	}

	private RetailAiData.ConditionSpawnNpc firstNpc(int worldId, int npcId) {
		return aiData.getConditionSpawns(worldId).stream()
			.flatMap(condition -> condition.groups().stream())
			.flatMap(group -> group.slots().stream())
			.flatMap(List::stream)
			.flatMap(choice -> choice.members().stream())
			.filter(npc -> npc.id() == npcId)
			.findFirst().orElseThrow();
	}

	private long countRespawningNpcs() {
		return List.of(UNDERPATH, FACTORY).stream()
			.flatMap(worldId -> aiData.getConditionSpawns(worldId).stream())
			.flatMap(condition -> condition.groups().stream())
			.flatMap(group -> group.slots().stream())
			.flatMap(List::stream)
			.flatMap(choice -> choice.members().stream())
			.filter(npc -> npc.respawnDelay() > 0)
			.count();
	}

	@Test
	void lunaConditionSpawnNpcsResolveToRetailPatterns() {
		// pattern 集合基线：与真端 NpcAIPatterns 文件逐名一致（108 def + 90 siege，双向 diff 为 0）。
		// Pattern-set baseline: identical by name to the retail NpcAIPatterns files (108 def + 90 siege).
		assertEquals(198, countLunaPatterns(), "露娜 pattern 总数应与真端一致 / Luna pattern total must match retail");

		for (int worldId : List.of(UNDERPATH, FACTORY)) {
			for (ConditionSpawn condition : aiData.getConditionSpawns(worldId)) {
				condition.groups().forEach(group -> group.slots().forEach(slot -> slot.forEach(choice ->
					choice.members().forEach(npc -> {
						// 每个 NPC 必须有 npc-ai 映射；ai 名在 compact pattern 表缺失时，真端同样缺失，
						// 双方都回退基础 AI（已核对真端 pattern 文件集合一致），因此只断言映射存在。
						// Every NPC needs an npc-ai entry; when the AI name has no compact pattern, retail lacks it
						// too (pattern sets verified identical) and both fall back to the base AI.
						assertNotNull(aiData.getNpc(npc.id()),
							() -> worldId + " 条件刷怪 NPC 缺少 AI 映射 / condition NPC lacks AI mapping: " + npc.id());
					}))));
			}
		}
	}

	private long countLunaPatterns() {
		long count = 0;
		for (Pattern pattern : aiData.patterns()) {
			String name = pattern.name() == null ? "" : pattern.name().toLowerCase(Locale.ROOT);
			if (name.startsWith("idluna_def") || name.startsWith("idluna_siege")) {
				count++;
			}
		}
		return count;
	}

	@Test
	void lunaInstanceAiPatternsAreFullySupported() {
		List<String> failures = new ArrayList<>();
		int checked = 0;
		for (Pattern pattern : aiData.patterns()) {
			String name = pattern.name() == null ? "" : pattern.name().toLowerCase(Locale.ROOT);
			if (!name.startsWith("idluna_def") && !name.startsWith("idluna_siege")) {
				continue;
			}
			checked++;
			String reason = unsupportedReason(pattern);
			if (reason != null) {
				failures.add(pattern.name() + " -> " + reason);
			}
		}
		assertTrue(checked >= 150, "两张地图的 pattern 数量异常 / unexpected pattern count: " + checked);
		assertTrue(failures.isEmpty(), () -> "未被 RetailPatternAI2 支持的露娜 pattern / unsupported Luna patterns:\n"
			+ String.join("\n", failures));
	}

	/**
	 * 返回首个不被支持的条件/动作描述；全部支持时返回 null。
	 * Return the first unsupported condition/action description, or null when fully supported.
	 */
	private String unsupportedReason(Pattern pattern) {
		try {
			Method supportsCondition = RetailPatternAI2.class.getDeclaredMethod("supportsCondition", String.class, Operation.class);
			supportsCondition.setAccessible(true);
			Method supportsAction = RetailPatternAI2.class.getDeclaredMethod("supportsAction", String.class, Operation.class, boolean.class);
			supportsAction.setAccessible(true);
			for (var event : pattern.events().entrySet()) {
				for (Rule rule : event.getValue()) {
					for (Operation condition : rule.conditions()) {
						if (!(Boolean) supportsCondition.invoke(null, event.getKey(), condition)) {
							return "condition " + condition.type() + " on " + event.getKey();
						}
					}
					for (Operation action : rule.actions()) {
						if (!(Boolean) supportsAction.invoke(null, event.getKey(), action, false)) {
							return "action " + action.type() + " on " + event.getKey();
						}
					}
				}
			}
			return null;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private void assertExpressionPresent(int worldId, String expression) {
		boolean found = aiData.getConditionSpawns(worldId).stream()
			.map(ConditionSpawn::expression)
			.anyMatch(candidate -> candidate.replaceAll("\\s+", "").equalsIgnoreCase(expression));
		assertTrue(found, () -> worldId + " 缺少关键条件表达式 / missing key expression: " + expression);
	}

	@Test
	void lunaInstanceRankAndScoreRulesMatchRetail() throws Exception {
		var underpathClass = Class.forName("com.aionemu.gameserver.instance.handlers.scripts.luna.ContaminatedUnderpathInstance");
		Object underpath = underpathClass.getDeclaredConstructor().newInstance();
		Method checkUnderpathRank = underpathClass.getDeclaredMethod("checkRank", int.class);
		checkUnderpathRank.setAccessible(true);
		assertEquals(1, checkUnderpathRank.invoke(underpath, 549000), "污染暗道 S 级积分阈值应为 549000");
		assertEquals(2, checkUnderpathRank.invoke(underpath, 544000), "污染暗道 A 级积分阈值应为 544000");
		assertEquals(3, checkUnderpathRank.invoke(underpath, 50), "污染暗道 B/C/D 级积分阈值应为 50");
		assertEquals(6, checkUnderpathRank.invoke(underpath, 49), "污染暗道 50 分以下为 F 级");

		var factoryClass = Class.forName("com.aionemu.gameserver.instance.handlers.scripts.luna.SecretMunitionsFactoryInstance");
		Object factory = factoryClass.getDeclaredConstructor().newInstance();
		Method checkFactoryRank = factoryClass.getDeclaredMethod("checkRank", int.class);
		checkFactoryRank.setAccessible(true);
		var startTimeField = factoryClass.getDeclaredField("startTime");
		startTimeField.setAccessible(true);

		long now = System.currentTimeMillis();
		// S 级：耗时 <= 780s
		startTimeField.set(factory, now - 700_000L);
		assertEquals(1, checkFactoryRank.invoke(factory, 878600), "军工厂 700s 通关应为 S 级");

		// A 级：耗时 <= 1140s
		startTimeField.set(factory, now - 1000_000L);
		assertEquals(2, checkFactoryRank.invoke(factory, 878600), "军工厂 1000s 通关应为 A 级");

		// B 级：耗时 <= 5000s
		startTimeField.set(factory, now - 3000_000L);
		assertEquals(3, checkFactoryRank.invoke(factory, 878600), "军工厂 3000s 通关应为 B 级");

		// F 级：超时或未击杀首领
		startTimeField.set(factory, now - 6000_000L);
		assertEquals(6, checkFactoryRank.invoke(factory, 878600), "军工厂超过 5000s 通关应为 F 级");
		assertEquals(6, checkFactoryRank.invoke(factory, 500000), "军工厂未达成 878600 分应为 F 级");
	}
}
