package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.model.Race;

import jakarta.xml.bind.JAXBContext;

/**
 * 验证制作技能引用来自正式 recipe 数据且自动学习闭包按阵营稳定解析。
 * Verifies craft-skill references come from formal recipe data and auto-learn closure resolves deterministically by race.
 */
class QuestGraphCraftSkillReferenceCatalogTest {

	/**
	 * 加载正式 recipe bundle，验证当前七类制作技能和阵营自动学习闭包。
	 * Loads the formal recipe bundle and verifies the seven current craft skills and race-specific auto-learn closure.
	 */
	@Test
	void formalRecipesCloseCurrentCraftSkillsAndStableAutolearnPlans() throws Exception {
		RecipeData recipes = load(new File("src/main/resources/aion/data/static_data/recipe/recipe_templates.xml"));
		QuestGraphCraftSkillReferenceCatalog catalog = QuestGraphCraftSkillReferenceCatalog.build(recipes);

		assertTrue(catalog.craftSkillIds().containsAll(Set.of(40001, 40002, 40003, 40004, 40007, 40008, 40010)));
		assertFalse(catalog.craftSkillIds().contains(9832));
		List<Integer> elyos = catalog.autolearnRecipeIds(Race.ELYOS, 40002, 400);
		List<Integer> asmodians = catalog.autolearnRecipeIds(Race.ASMODIANS, 40002, 400);
		assertFalse(elyos.isEmpty());
		assertFalse(asmodians.isEmpty());
		assertEquals(elyos.stream().sorted().toList(), elyos);
		assertEquals(asmodians.stream().sorted().toList(), asmodians);
		assertTrue(catalog.autolearnRecipeIds(Race.ELYOS, 40002, 500).containsAll(elyos));
		List<Integer> skillIds = new ArrayList<>(catalog.craftSkillIds());
		assertEquals(skillIds.stream().sorted().toList(), skillIds);
		assertThrows(UnsupportedOperationException.class, () -> catalog.craftSkillIds().add(49999));
	}

	/**
	 * 验证 recipe plan 只由 RecipeTemplate.skillid、阵营、技能点和 autolearn 标记导出。
	 * Verifies recipe plans derive only from RecipeTemplate.skillid, race, skill point, and the autolearn flag.
	 */
	@Test
	void derivesRaceSpecificStablePlanFromRecipeTemplates() throws Exception {
		String source = "<recipe_templates>"
			+ "<recipe_template id=\"30\" skillid=\"40002\" skillpoint=\"401\" autolearn=\"1\" race=\"PC_ALL\"/>"
			+ "<recipe_template id=\"20\" skillid=\"40002\" skillpoint=\"400\" autolearn=\"1\" race=\"PC_ALL\"/>"
			+ "<recipe_template id=\"10\" skillid=\"40002\" skillpoint=\"1\" autolearn=\"1\" race=\"ELYOS\"/>"
			+ "<recipe_template id=\"11\" skillid=\"40002\" skillpoint=\"1\" autolearn=\"1\" race=\"ASMODIANS\"/>"
			+ "<recipe_template id=\"12\" skillid=\"40002\" skillpoint=\"1\" autolearn=\"0\" race=\"ELYOS\"/>"
			+ "<recipe_template id=\"13\" skillid=\"40003\" skillpoint=\"1\" autolearn=\"1\" race=\"ELYOS\"/>"
			+ "</recipe_templates>";
		QuestGraphCraftSkillReferenceCatalog catalog = QuestGraphCraftSkillReferenceCatalog.build(load(source));

		assertEquals(List.of(10, 20), catalog.autolearnRecipeIds(Race.ELYOS, 40002, 400));
		assertEquals(List.of(11, 20), catalog.autolearnRecipeIds(Race.ASMODIANS, 40002, 400));
		assertEquals(List.of(10, 20, 30), catalog.autolearnRecipeIds(Race.ELYOS, 40002, 500));
	}

	/**
	 * 拒绝未知技能、非玩家阵营和非法目标等级查询。
	 * Rejects unknown skills, non-player races, and invalid target-level queries.
	 */
	@Test
	void rejectsInvalidAutolearnQueries() throws Exception {
		QuestGraphCraftSkillReferenceCatalog catalog = QuestGraphCraftSkillReferenceCatalog.build(
			load(new File("src/main/resources/aion/data/static_data/recipe/recipe_templates.xml")));

		assertThrows(IllegalArgumentException.class, () -> catalog.autolearnRecipeIds(Race.PC_ALL, 40002, 400));
		assertThrows(IllegalArgumentException.class, () -> catalog.autolearnRecipeIds(Race.ELYOS, 99999, 400));
		assertThrows(IllegalArgumentException.class, () -> catalog.autolearnRecipeIds(Race.ELYOS, 40002, 0));
	}

	/**
	 * 全量校验拒绝非法 recipe ID、skill ID、skillpoint 和 race。
	 * Full validation rejects invalid recipe ids, skill ids, skill points, and races.
	 */
	@Test
	void rejectsInvalidFormalRecipeFields() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> QuestGraphCraftSkillReferenceCatalog.build(load(xml(0, 40002, 1, "ELYOS"))));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphCraftSkillReferenceCatalog.build(load(xml(1, 0, 1, "ELYOS"))));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphCraftSkillReferenceCatalog.build(load(xml(1, 40002, -1, "ELYOS"))));
		assertThrows(Exception.class, () -> QuestGraphCraftSkillReferenceCatalog.build(load(xml(1, 40002, 1, null))));
	}

	/** 加载正式文件。 / Loads a formal recipe file. */
	private static RecipeData load(File file) throws Exception {
		return (RecipeData) JAXBContext.newInstance(RecipeData.class).createUnmarshaller().unmarshal(file);
	}

	/** 加载聚焦 XML。 / Loads focused recipe XML. */
	private static RecipeData load(String xml) throws Exception {
		return (RecipeData) JAXBContext.newInstance(RecipeData.class).createUnmarshaller().unmarshal(new StringReader(xml));
	}

	/** 构造仅含一条 recipe 的聚焦 XML。 / Builds focused XML containing one recipe. */
	private static String xml(int id, int skillId, int skillPoint, String race) {
		String raceAttribute = race == null ? "" : " race=\"" + race + "\"";
		return "<recipe_templates><recipe_template id=\"" + id + "\" skillid=\"" + skillId
			+ "\" skillpoint=\"" + skillPoint + "\" autolearn=\"1\"" + raceAttribute + "/></recipe_templates>";
	}
}
