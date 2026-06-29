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

import com.aionemu.gameserver.model.templates.arcadeupgrade.ArcadeTab;
import com.aionemu.gameserver.model.templates.arcadeupgrade.ArcadeTabItem;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * Created by wanke on 17/02/2017.
 */
@XmlRootElement(name = "arcadelist")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArcadeUpgradeData {
	@XmlElement(name = "tab")
	private List<ArcadeTab> arcadeTabTemplate;
	@XmlTransient
	private IntObjectHashMap<List<ArcadeTabItem>> arcadeItemList = new IntObjectHashMap<>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		arcadeItemList.clear();
		for (ArcadeTab template : arcadeTabTemplate) {
			arcadeItemList.put(template.getId(), template.getArcadeTabItems());
		}
	}

	public int size() {
		return arcadeItemList.size();
	}

	public List<ArcadeTabItem> getArcadeTabById(int id) {
		return arcadeItemList.get(id);
	}

	public List<ArcadeTab> getArcadeTabs() {
		return arcadeTabTemplate;
	}
}
