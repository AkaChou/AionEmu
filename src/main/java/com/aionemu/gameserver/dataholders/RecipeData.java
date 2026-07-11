package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 制作配方数据容器，按 ID 索引，并按阵营缓存自动学习配方。
 * Recipe data holder, indexing recipes by id and caching auto-learn recipes by race.
 *
 * @author ATracer, MrPoke, KID
 */
@XmlRootElement(name = "recipe_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecipeData {
	@XmlElement(name = "recipe_template")
	protected List<RecipeTemplate> list;
	private IntObjectHashMap<RecipeTemplate> recipeData;
	private List<RecipeTemplate> elyos, asmos, any;

	/**
	 * JAXB 反序列化完成后，构建 ID 索引与自动学习阵营列表。
	 * After JAXB unmarshalling, builds the id index and auto-learn race lists.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		recipeData = new IntObjectHashMap<RecipeTemplate>();
		elyos = new ArrayList<>();
		asmos = new ArrayList<>();
		any = new ArrayList<>();
		for (RecipeTemplate it : list) {
			recipeData.put(it.getId(), it);
			if (it.getAutoLearn() == 0) {
				continue;
			}
			switch (it.getRace()) {
			case ASMODIANS:
				asmos.add(it);
				break;
			case ELYOS:
				elyos.add(it);
				break;
			case PC_ALL:
				any.add(it);
				break;
			}
		}
		list = null;
	}

	/**
	 * 返回指定阵营、技能与技能等级上限下可自动学习的配方。
	 * Returns auto-learn recipes for the given race, skill and max skill level.
	 *
	 * 阵营 / race
	 * skill id
	 * @param maxLevel 技能等级上限 / max skill level
	 * @return 可自动学习配方列表 / auto-learn recipe list
	 */
	public List<RecipeTemplate> getAutolearnRecipes(Race race, int skillId, int maxLevel) {
		List<RecipeTemplate> list = new ArrayList<>();
		switch (race) {
		case ASMODIANS:
			for (RecipeTemplate recipe : asmos)
				if (recipe.getSkillid() == skillId && recipe.getSkillpoint() <= maxLevel) {
					list.add(recipe);
				}
			break;
		case ELYOS:
			for (RecipeTemplate recipe : elyos)
				if (recipe.getSkillid() == skillId && recipe.getSkillpoint() <= maxLevel) {
					list.add(recipe);
				}
			break;
		}

		for (RecipeTemplate recipe : any)
			if (recipe.getSkillid() == skillId && recipe.getSkillpoint() <= maxLevel) {
				list.add(recipe);
			}
		return list;
	}

	/**
	 * 按配方 ID 获取配方模板。
	 * Returns the recipe template for the given id.
	 *
	 * @param id 配方 ID / recipe id
	 * @return 配方模板，不存在则为 null / recipe template or null
	 */
	public RecipeTemplate getRecipeTemplateById(int id) {
		return recipeData.get(id);
	}

	/**
	 * 返回全部配方模板映射。
	 * Returns the full recipe template map.
	 *
	 * @return ID 到配方的映射 / map of id to recipe
	 */
	public IntObjectHashMap<RecipeTemplate> getRecipeTemplates() {
		return recipeData;
	}

	/**
	 * 返回已加载的配方数量。
	 * Returns the number of loaded recipes.
	 *
	 * recipe count
	 */
	public int size() {
		return recipeData.size();
	}
}
