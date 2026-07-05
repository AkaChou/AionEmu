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

import java.util.EnumMap;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.templates.item.ItemSkillEnhance;

/**
 * Created by wanke on 01/03/2017.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "item_skill_enhances")
public class ItemSkillEnhanceData {
	@XmlElement(name = "item_skill_enhance", required = true)
	protected List<ItemSkillEnhance> skillEnhances;

	@XmlTransient
	protected IntObjectHashMap<ItemSkillEnhance> enhanceSkillsById = new IntObjectHashMap<ItemSkillEnhance>();

	@XmlTransient
	protected IntObjectHashMap<EnumMap<PlayerClass, ItemSkillEnhance>> enhanceSkillsByIdAndClass = new IntObjectHashMap<EnumMap<PlayerClass, ItemSkillEnhance>>();

	public ItemSkillEnhance getSkillEnhance(int id) {
		return enhanceSkillsById.get(id);
	}

	public ItemSkillEnhance getSkillEnhance(int id, PlayerClass playerClass) {
		EnumMap<PlayerClass, ItemSkillEnhance> enhanceSkillsByClass = enhanceSkillsByIdAndClass.get(id);
		if (enhanceSkillsByClass == null) {
			return null;
		}
		ItemSkillEnhance enhance = playerClass == null ? null : enhanceSkillsByClass.get(playerClass);
		if (enhance != null) {
			return enhance;
		}
		return enhanceSkillsByClass.get(PlayerClass.ALL);
	}

	void afterUnmarshal(Unmarshaller u, Object parent) {
		enhanceSkillsById.clear();
		enhanceSkillsByIdAndClass.clear();
		for (ItemSkillEnhance enhance : skillEnhances) {
			enhanceSkillsById.put(enhance.getId(), enhance);
			enhanceSkillsByIdAndClass.computeIfAbsent(enhance.getId(), id -> new EnumMap<PlayerClass, ItemSkillEnhance>(PlayerClass.class))
					.put(enhance.getClassId(), enhance);
		}
		skillEnhances.clear();
		skillEnhances = null;
	}

	public int size() {
		return enhanceSkillsById.size();
	}
}
