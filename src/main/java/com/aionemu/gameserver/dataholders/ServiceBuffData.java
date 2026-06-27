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

import com.aionemu.gameserver.model.templates.bonus_service.BonusServiceAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * @author Ranastic (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "serviceBonusattr" })
@XmlRootElement(name = "service_bonusattrs")
public class ServiceBuffData {
	@XmlElement(name = "service_bonusattr")
	protected List<BonusServiceAttr> serviceBonusattr;

	@XmlTransient
	private IntObjectHashMap<BonusServiceAttr> templates = new IntObjectHashMap<BonusServiceAttr>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (BonusServiceAttr template : serviceBonusattr) {
			templates.put(template.getBuffId(), template);
		}
		serviceBonusattr.clear();
		serviceBonusattr = null;
	}

	public int size() {
		return templates.size();
	}

	public BonusServiceAttr getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}
}