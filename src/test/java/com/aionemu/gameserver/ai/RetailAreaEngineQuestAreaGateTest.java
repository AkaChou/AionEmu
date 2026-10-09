package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进区发放绑定权威门（§10.3-#26 闭环，P8 第六批）。
 * <p>
 * 原版绑定权威 = 258 个世界目录的 {@code world{,_M,_N}.xml} {@code <questscript_area>} 的
 * {@code <quest>} 子元素（`User::MoveNew` 入队 → tick 排水 → `User_AddAreaQuest(type 3)` 的
 * 绑定源）。全量对拍裁定：家族道 `_area_` 行 25 行 = **活 6**（SimpleTalk 12504/22504、
 * SimpleHunt 12505/12524/22524/39005，各自绑定原版区名）+ **死 19**（区名缺席或区在而无
 * {@code quest} 绑定 = 原版本征死边，沿 §10.3-#23 口径冻结）。ai-areas.xml 曾含 9 个幻影绑定
 * （P0c-4「ai-areas = 原版镜像」前提被推翻），2026-10-02 已按原版清空（区定义保留、绑定置空，
 * 与 {@code LDF5a_QuestArea_Q22505} 既有先例同形）。
 * <p>
 * 断言：①家族两车道 AREA 行在 ai-areas 的绑定 == 原版活集（双向）；②活行绑定区名逐行等于
 * 原版区名；③活行发放/触发面齐备（laneOf ∧ isSystemGranted——routes ∧ AREA，冻结行不进分发）。
 * Gate for the area-grant binding authority (§10.3-#26 closure): the retail world files' quest
 * script areas are the binding authority. Of the 25 family `_area_` rows exactly six are
 * retail-live and nineteen are retail-dead; ai-areas.xml carried nine phantom bindings that were
 * emptied against the retail truth. The lanes' AREA rows must bind exactly the live set, each
 * under its retail area name, with the grant face intact.
 */
class RetailAreaEngineQuestAreaGateTest {

	/** ai-areas.xml（生产 quest_area 绑定表，主代码 RetailAiData 同源）。 / The production quest-area table. */
	private static final Path AI_AREAS = Path.of("src/main/resources/aion/definitions/compact/ai/ai-areas.xml");

	/** 原版活绑定：quest → 原版区名（258 世界目录全量对拍，2026-10-02 冻结）。 / Retail-live bindings. */
	private static final Map<Integer, String> RETAIL_LIVE = Map.of(
		12504, "LDF5a_QuestArea_Q12504",
		12505, "LDF5a_QuestArea_Q12505",
		12524, "LDF5a_QuestArea_Q12524",
		22504, "LDF5a_QuestArea_Q22504",
		22524, "LDF5a_QuestArea_Q22524",
		39005, "InvadePortalDest_41_questArea_02");

	/** 原版死边：区名缺席或区在而无 quest 绑定（原版本征，禁止补绑定）。 / Retail-dead rows. */
	private static final Set<Integer> RETAIL_DEAD = Set.of(
		13523, 13524, 13525, 16972, 18003, 18033, 22505,
		23523, 23524, 23525, 26972, 28003, 28033,
		39007, 39009, 49005, 49007, 49009, 99000,
		// 驼峰哨兵 `_Area_` 四行（大小写不敏感同类别）：原版表原文即 `_Area_`，原版无区绑定。
		// The four camel-case `_Area_` rows (same category, case-insensitive): the retail table
		// itself writes `_Area_`, and retail binds no quest script area for them.
		13912, 13913, 23912, 23913);

	@Test
	void familyAreaRowsBindExactlyTheRetailLiveSet() throws Exception {
		String xml = java.nio.file.Files.readString(AI_AREAS);
		// ai-areas 全表绑定扫描（quests 属性；与 RetailSystemGrantDispatchTest 同一口径）。
		// Scan every quest_area binding (same shape as the grant-dispatch gate).
		Map<Integer, String> bound = new java.util.TreeMap<>();
		Matcher matcher = Pattern.compile("<quest_area\\b[^>]*\\bname=\"([^\"]+)\"[^>]*\\bquests=\"([^\"]*)\"")
			.matcher(xml);
		while (matcher.find()) {
			for (String token : matcher.group(2).split("[,\\s]+")) {
				if (token.matches("\\d+")) {
					bound.put(Integer.parseInt(token), matcher.group(1));
				}
			}
		}
		Set<Integer> familyRows = new TreeSet<>();
		Set<Integer> routedAreaRows = new TreeSet<>();
		collectAreaRows(familyRows, routedAreaRows);
		Set<Integer> expectedAll = new TreeSet<>(RETAIL_LIVE.keySet());
		expectedAll.addAll(RETAIL_DEAD);
		assertEquals(expectedAll, familyRows,
			"家族 _area_ 行全集漂移（取证基线 29 行（驼峰 `_Area_` 同类别）；missing=" + new TreeSet<>(diff(expectedAll, familyRows))
				+ " extra=" + new TreeSet<>(diff(familyRows, expectedAll)) + "）");
		Set<Integer> aiAreasFamilyBindings = new TreeSet<>(familyRows);
		aiAreasFamilyBindings.retainAll(bound.keySet());
		assertEquals(RETAIL_LIVE.keySet(), aiAreasFamilyBindings,
			"ai-areas 绑定必须恰好等于原版活集（活行必绑、死行必不绑）");
		for (Map.Entry<Integer, String> entry : RETAIL_LIVE.entrySet()) {
			assertEquals(entry.getValue(), bound.get(entry.getKey()),
				() -> "quest " + entry.getKey() + " 的绑定区名必须逐字等于原版 questscript_area");
		}
		// 触发面齐备：活行必须 routes ∧ grantKind==AREA（分发判据）；可路由面 ⊇ 活集即可——
		// 死边行不触发由**数据门**（ai-areas 绑定 == 活集）界定，不由路由面界定。
		// The trigger face: every live row must be a routed AREA row; the routed set may be larger —
		// the data gate (bindings == live set) is what bounds the dispatch, not the routing face.
		assertTrue(routedAreaRows.containsAll(RETAIL_LIVE.keySet()),
			() -> "原版活行缺路由/AREA 判据（进区无法发放）: " + diff(RETAIL_LIVE.keySet(), routedAreaRows));
	}

	/** 家族两车道的 AREA 行全集与可路由子集。 / All family AREA rows and their routed subset. */
	private static void collectAreaRows(Set<Integer> familyRows, Set<Integer> routedAreaRows) {
		for (var lane : new com.aionemu.gameserver.questEngine.tablelane.NativeSystemGrantLane[] {
				SimpleTalkHandler.instance(), SimpleHuntHandler.instance()}) {
			for (int questId : lane.ownedQuestIds()) {
				if (lane.grantKind(questId) == RetailGrantKind.AREA) {
					familyRows.add(questId);
					if (lane.routes(questId)) {
						routedAreaRows.add(questId);
					}
				}
			}
		}
	}

	private static <T> Set<T> diff(Set<T> left, Set<T> right) {
		Set<T> result = new TreeSet<>(left);
		result.removeAll(right);
		return result;
	}
}
