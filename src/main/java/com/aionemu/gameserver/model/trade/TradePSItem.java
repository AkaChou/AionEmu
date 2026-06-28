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

import lombok.Getter;
import lombok.Setter;

/**
 * @author Simple
 */
public class TradePSItem extends TradeItem {

	@Getter
	@Setter
	private int itemObjId;
	@Getter
	@Setter
	private long price;

	/**
	 * @param itemId
	 * @param count
	 */
	public TradePSItem(int itemObjId, int itemId, long count, long price) {
		super(itemId, count);
		this.setPrice(price);
		this.setItemObjId(itemObjId);
	}

}
