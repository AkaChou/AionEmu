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
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.pet.PetBonusAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "petBonusattr" })
@XmlRootElement(name = "pet_bonusattrs")
public class PetBuffData {

	@XmlElement(name = "pet_bonusattr")
	protected List<PetBonusAttr> petBonusattr;

	@XmlTransient
	private IntObjectHashMap<PetBonusAttr> templates = new IntObjectHashMap<PetBonusAttr>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PetBonusAttr template : petBonusattr) {
			templates.put(template.getBuffId(), template);
			templates.put(template.getFoodCount(), template);
		}
		petBonusattr.clear();
		petBonusattr = null;
	}

	public int size() {
		return templates.size();
	}

	public PetBonusAttr getPetBonusattr(int buffId) {
		return templates.get(buffId);
	}

	public PetBonusAttr getFoodCount(int count) {
		return templates.get(count);
	}
}