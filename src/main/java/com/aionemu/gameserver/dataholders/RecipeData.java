/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
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

import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * @author ATracer, MrPoke, KID
 */
@XmlRootElement(name = "recipe_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecipeData {
	@XmlElement(name = "recipe_template")
	protected List<RecipeTemplate> list;
	private TIntObjectHashMap<RecipeTemplate> recipeData;
	private List<RecipeTemplate> elyos, asmos, any;

	void afterUnmarshal(Unmarshaller u, Object parent) {
		recipeData = new TIntObjectHashMap<RecipeTemplate>();
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

	public RecipeTemplate getRecipeTemplateById(int id) {
		return recipeData.get(id);
	}

	public TIntObjectHashMap<RecipeTemplate> getRecipeTemplates() {
		return recipeData;
	}

	/**
	 * @return recipeData.size()
	 */
	public int size() {
		return recipeData.size();
	}
}
