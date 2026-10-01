package com.aionemu.gameserver.questEngine.retail;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestOwnerResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader;

/**
 * P6 步骤 1 的行集/可行性冻结门（计划 §7 批门第 1 条，**本步零切换**）：CombineTask 表行逐行独立复算
 * （不复用 handler / 编译器判定），把「表行 → 退役 → NATIVE_READY」三段冻结下来：
 * <ul>
 *   <li>装载：{@code Quest_CombineTask.xml} 574 行（装载器逐行 = 真端表逐行）；</li>
 *   <li>退役集（{@code retail-xml-retention.tsv} owner=RETAIL_TABLE）；</li>
 *   <li>NATIVE_READY = 退役 ∧ 非 XML-only ∧ 接取 NPC 名唯一 ×N ∧ 技能符号可解 ∧ 技能点与真端元数据一致
 *       ∧ 产物单槽且符号可解 ∧ 分量非空且全部可解 ∧ {@code (skill, product)} 配方唯一 ∧ 真端元数据
 *       产物/分量一致；</li>
 *   <li>不可路由行按稳定原因集合冻结（新增或消失都必须显式改本类）。</li>
 * </ul>
 * 复算证据导出：{@code -Dp6.out=<path>}（TSV，逐行 verdict）。
 * <p>
 * Frozen row-set / feasibility gate for the P6 CombineTask family. The verdict is recomputed here from
 * the retail table plus the production indexes and the retail quest.xml metadata — never read from a
 * handler — and each unroutable row's reason set is frozen.
 */
class CombineTaskFamilyRowInventoryGateTest {

	private static final String RECIPE_TEMPLATES = "/aion/data/static_data/recipe/recipe_templates.xml";
	private static final int EXPECTED_ROWS = 574;

	@Test
	void rowInventoryAndReadySetAreFrozen() throws Exception {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		NativeQuestOwnerResolver owners = NativeQuestOwnerResolver.instance();
		NativeNpcNameResolver npcs = NativeNpcNameResolver.instance();
		RetailItemNameIndex items = RetailItemNameIndex.loadItemTemplates();
		RetailRecipeIndex recipes;
		try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(RECIPE_TEMPLATES),
				"missing " + RECIPE_TEMPLATES)) {
			recipes = RetailRecipeIndex.build(List.of(input));
		}
		RetailQuestDriver driver = RetailQuestDriver.ensureLoaded();

		List<String> lines = new ArrayList<>();
		Map<String, Integer> reasons = new TreeMap<>();
		TreeSet<String> failClosed = new TreeSet<>();
		int retired = 0;
		int ready = 0;
		for (NativeQuestTableLoader.CombineTaskRow row : loader.combineRows()) {
			int questId = row.questId();
			List<String> why = new ArrayList<>();
			boolean isRetired = RetiredQuestIds.contains(questId);
			boolean xmlOnly = owners.xmlOnlyIds().contains(questId);
			boolean npcOk = row.taskNpcNames().stream().allMatch(name -> unique(name, npcs));
			Integer skillId = RetailQuestMetadataCompiler.combineSkillId(row.combineSkill());
			boolean skillOk = skillId != null;
			Integer productId = itemResolves(row.product(), items) ? productIdOf(row.product(), items) : null;
			boolean productOk = productId != null;
			boolean componentsOk = row.components().stream().filter(Objects::nonNull)
				.allMatch(cell -> itemResolves(cell, items));
			if (componentsOk && row.components().stream().allMatch(Objects::isNull)) {
				componentsOk = false;
			}
			RetailQuestMetadataCompiler.Outcome metadata = metadata(driver, questId);
			boolean recipeOk = skillOk && productOk
				&& !recipes.resolveAll(skillId, productId).isEmpty()
				&& recipes.resolveAll(skillId, productId).size() == 1;
			boolean metadataOk = metadata != null && metadata.clean()
				&& skillPointMatches(row, metadata) && metadataProductMatches(row, productId, metadata)
				&& metadataComponentsMatch(row, items, metadata);
			if (!isRetired) why.add("NOT_RETIRED");
			if (xmlOnly) why.add("XML_ONLY");
			if (!npcOk) why.add("ACQUIRE_NPC_UNRESOLVED");
			if (!skillOk) why.add("COMBINE_SKILL_UNRESOLVED");
			if (!productOk) why.add("PRODUCT_UNRESOLVED");
			if (!componentsOk) why.add("COMPONENT_UNRESOLVED");
			if (!recipeOk) why.add("RECIPE_UNRESOLVED");
			if (!metadataOk) why.add("METADATA_MISMATCH");
			boolean isReady = why.isEmpty();
			if (isRetired) {
				retired++;
			}
			if (isReady) {
				ready++;
			}
			if (isRetired && !isReady) {
				failClosed.add(questId + ":" + String.join("+", why));
				for (String reason : why) {
					if (!"NOT_RETIRED".equals(reason)) {
						reasons.merge(reason, 1, Integer::sum);
					}
				}
			}
			lines.add(String.join("\t", String.valueOf(questId), row.combineSkill(),
				String.valueOf(row.skillPoint()), row.product(), String.valueOf(row.components().size()),
				row.taskNpcNames().size() + "", isReady ? "NATIVE_READY" : String.join("+", why)));
		}

		String out = System.getProperty("p6.out");
		if (out != null) {
			Files.write(Path.of(out), ("# P6 步骤 1 可行性复算（临时；NATIVE_READY 判据见 p6/P6-STEP1-REPORT.zh-CN.md）\n"
				+ "quest_id\tcombine_skill\tskill_point\tproduct\tcomponent_slots\ttask_npcs\tverdict\n"
				+ String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
		}

		org.junit.jupiter.api.Assertions.assertEquals(EXPECTED_ROWS, loader.combineSize(),
			"真端 Quest_CombineTask.xml 行数必须冻结");
		org.junit.jupiter.api.Assertions.assertEquals(EXPECTED_ROWS, retired,
			"该族 retention owner=RETAIL_TABLE 行数必须冻结（真端表逐行 ⇔ 退役行）");
		org.junit.jupiter.api.Assertions.assertEquals(EXPECTED_ROWS, ready,
			"NATIVE_READY 行数必须冻结：全族 NPC/技能/技能点/产物/分量/配方/元数据七轴全通，零 fail-closed 残余");
		org.junit.jupiter.api.Assertions.assertEquals(Map.of(), reasons,
			"不可路由原因直方图必须保持为空（出现任何原因都必须显式登记）");
		org.junit.jupiter.api.Assertions.assertEquals(new TreeSet<String>(), failClosed,
			"该族不得出现 fail-closed 行");
		System.out.println("P6_PROBE rows=" + lines.size() + " retired=" + retired + " ready=" + ready
			+ " reasons=" + reasons + " failClosed=" + failClosed);
	}


	private static boolean unique(String name, NativeNpcNameResolver npcs) {
		if (name == null || name.isBlank()) {
			return false;
		}
		return npcs.resolve(name).resolution() == NativeNpcNameResolver.Resolution.UNIQUE;
	}

	private static boolean itemResolves(String cell, RetailItemNameIndex items) {
		return symbol(cell, items) != null;
	}

	private static Integer productIdOf(String cell, RetailItemNameIndex items) {
		return symbol(cell, items);
	}

	/** 单元 = 「符号 [数量]」；符号解析走两通道（先原名，未命中再去 {@code ITEM_} 前缀）。 /
	 * A cell is "symbol [count]"; the symbol resolves through the two-channel rule (raw name first, then
	 * the {@code ITEM_}-stripped name). */
	private static Integer symbol(String cell, RetailItemNameIndex items) {
		if (cell == null || cell.isBlank()) {
			return null;
		}
		String name = cell.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
		Integer direct = items.resolve(name);
		if (direct != null) {
			return direct;
		}
		return name.startsWith("item_") ? items.resolve(name.substring("item_".length())) : null;
	}

	private static int cellCount(String cell) {
		String[] parts = cell.trim().split("\\s+");
		return parts.length < 2 ? 1 : Integer.parseInt(parts[1]);
	}

	private static RetailQuestMetadataCompiler.Outcome metadata(RetailQuestDriver driver, int questId) {
		try {
			return driver.retailMetadataOf(questId).orElse(null);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static boolean skillPointMatches(NativeQuestTableLoader.CombineTaskRow row,
			RetailQuestMetadataCompiler.Outcome metadata) {
		Integer declared = metadata.metadata().combineSkillPoint();
		return (declared == null ? 0 : declared) == row.skillPoint();
	}

	private static boolean metadataProductMatches(NativeQuestTableLoader.CombineTaskRow row, Integer productId,
			RetailQuestMetadataCompiler.Outcome metadata) {
		List<QuestItemRequirement> declared = metadata.metadata().itemRequirements();
		return declared.size() == 1 && productId != null && declared.getFirst().itemId() == productId
			&& declared.getFirst().count() == cellCount(row.product());
	}

	private static boolean metadataComponentsMatch(NativeQuestTableLoader.CombineTaskRow row,
			RetailItemNameIndex items, RetailQuestMetadataCompiler.Outcome metadata) {
		List<QuestItemRequirement> declared = new ArrayList<>();
		for (String cell : row.components()) {
			if (cell == null) {
				continue;
			}
			declared.add(new QuestItemRequirement(symbol(cell, items), cellCount(cell)));
		}
		return metadata.metadata().questWorkItems().equals(declared);
	}
}
