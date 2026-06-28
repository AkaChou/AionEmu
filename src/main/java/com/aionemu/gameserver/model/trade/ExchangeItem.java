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
package com.aionemu.gameserver.model.trade;

import com.aionemu.gameserver.model.gameobjects.Item;
import lombok.Getter;
import lombok.Setter;

/**
 * @author ATracer
 */
public class ExchangeItem {

	@Getter
	private int itemObjId;
	@Getter
	private long itemCount;
	@Getter
	private int itemDesc;
	@Getter
	@Setter
	private Item item;

	/**
	 * Used when exchange item != original item
	 * 
	 * @param itemObjId
	 * @param itemCount
	 * @param item
	 */
	public ExchangeItem(int itemObjId, long itemCount, Item item) {
		this.itemObjId = itemObjId;
		this.itemCount = itemCount;
		this.item = item;
		this.itemDesc = item.getItemTemplate().getNameId();
	}

	/**
	 * @param countToAdd
	 */
	public void addCount(long countToAdd) {
		this.itemCount += countToAdd;
		this.item.setItemCount(itemCount);
	}

}
