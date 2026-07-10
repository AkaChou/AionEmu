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
package com.aionemu.gameserver.model.drop;

import java.nio.ByteBuffer;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.Unmarshaller;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "drop")
public class Drop {
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;

	@XmlAttribute(name = "min_amount")
	protected int minAmount = 1;

	@XmlAttribute(name = "max_amount")
	protected int maxAmount;

	@XmlAttribute
	protected float chance = 100;

	@XmlAttribute(name = "no_reduce")
	protected Boolean noReduce = false;

	@XmlAttribute(name = "eachmember")
	protected boolean eachMember = false;
	@XmlAttribute(name = "each_member")
	protected Boolean aionServerEachMember;

	private ItemTemplate template;

	public Drop() {
	}

	public Drop(int itemId, int minAmount, int maxAmount, float chance, boolean noReduce, boolean eachMember) {
		this.itemId = itemId;
		this.minAmount = minAmount;
		this.maxAmount = maxAmount;
		this.chance = chance;
		this.noReduce = noReduce;
		this.eachMember = eachMember;
		template = DataManager.ITEM_DATA.getItemTemplate(itemId);
	}

	@SuppressWarnings("unused")
	private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		if (maxAmount == 0) {
			maxAmount = minAmount;
		}
	}

	public ItemTemplate getItemTemplate() {
		return template == null ? DataManager.ITEM_DATA.getItemTemplate(itemId) : template;
	}

	public int getItemId() {
		return itemId;
	}

	public int getMinAmount() {
		return minAmount;
	}

	public int getMaxAmount() {
		return maxAmount;
	}

	public float getChance() {
		return chance;
	}

	public boolean isNoReduction() {
		return noReduce;
	}

	public Boolean isEachMember() {
		return aionServerEachMember == null ? eachMember : aionServerEachMember;
	}

	public static Drop load(ByteBuffer buffer) {
		Drop drop = new Drop();
		drop.itemId = buffer.getInt();
		drop.chance = buffer.getFloat();
		drop.minAmount = buffer.getInt();
		drop.maxAmount = buffer.getInt();
		drop.noReduce = buffer.get() == 1 ? true : false;
		drop.eachMember = buffer.get() == 1 ? true : false;
		return drop;
	}

	public String toString() {
		return "Drop [itemId=" + itemId + ", minAmount=" + minAmount + ", maxAmount=" + maxAmount + ", chance=" + chance
				+ ", noReduce=" + noReduce + ", eachMember=" + eachMember + "]";
	}
}
