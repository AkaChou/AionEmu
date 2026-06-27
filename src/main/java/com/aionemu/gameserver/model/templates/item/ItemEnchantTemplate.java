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
package com.aionemu.gameserver.model.templates.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemEnchantTemplate")
public class ItemEnchantTemplate {
	@XmlAttribute(name = "id")
	private int id;

	@XmlAttribute(name = "type")
	private EnchantType type;

	@XmlElement(name = "item_enchant", required = false)
	private List<ItemEnchantBonus> item_enchant;

	@XmlTransient
	private Map<Integer, List<StatFunction>> enchants = new HashMap<Integer, List<StatFunction>>();

	public List<StatFunction> getStats(int level) {
		if (this.enchants.containsKey(level)) {
			return this.enchants.get(level);
		}
		return null;
	}

	public List<ItemEnchantBonus> getItemEnchant() {
		return this.item_enchant;
	}

	public int getId() {
		return this.id;
	}

	public EnchantType getEnchantType() {
		return this.type;
	}
}
