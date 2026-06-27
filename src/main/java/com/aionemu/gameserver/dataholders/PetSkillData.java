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

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.petskill.PetSkillTemplate;

import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.commons.utils.collections.IntIntHashMap;
import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * @author ATracer
 */
@XmlRootElement(name = "pet_skill_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class PetSkillData {

	@XmlElement(name = "pet_skill")
	private List<PetSkillTemplate> petSkills;

	/** A map containing all npc skill templates */
	private IntObjectHashMap<IntIntHashMap> petSkillData = new IntObjectHashMap<IntIntHashMap>();

	private IntObjectHashMap<IntArrayList> petSkillsMap = new IntObjectHashMap<IntArrayList>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PetSkillTemplate petSkill : petSkills) {
			IntIntHashMap orderSkillMap = petSkillData.get(petSkill.getOrderSkill());
			if (orderSkillMap == null) {
				orderSkillMap = new IntIntHashMap();
				petSkillData.put(petSkill.getOrderSkill(), orderSkillMap);
			}
			orderSkillMap.put(petSkill.getPetId(), petSkill.getSkillId());

			IntArrayList skillList = petSkillsMap.get(petSkill.getPetId());
			if (skillList == null) {
				skillList = new IntArrayList();
				petSkillsMap.put(petSkill.getPetId(), skillList);
			}
			skillList.add(petSkill.getSkillId());
		}
	}

	public int size() {
		return petSkillData.size();
	}

	public int getPetOrderSkill(int orderSkill, int petNpcId) {
		return petSkillData.get(orderSkill).get(petNpcId);
	}

	public boolean petHasSkill(int petNpcId, int skillId) {
		return petSkillsMap.get(petNpcId).contains(skillId);
	}
}