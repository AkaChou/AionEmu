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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import lombok.Getter;

/**
 * @author ATracer
 */
@Getter
public class Exchange {

	private Player activeplayer;
	private Player targetPlayer;

	private boolean confirmed;
	private boolean locked;

	private long kinahCount;

	private Map<Integer, ExchangeItem> items = new HashMap<Integer, ExchangeItem>();
	private List<Item> itemsToUpdate = new ArrayList<Item>();

	public Exchange(Player activeplayer, Player targetPlayer) {
		super();
		this.activeplayer = activeplayer;
		this.targetPlayer = targetPlayer;
	}

	public void confirm() {
		confirmed = true;
	}

	public void lock() {
		this.locked = true;
	}

	/**
	 * @param parentItemObjId
	 */
	public void addItem(int parentItemObjId, ExchangeItem exchangeItem) {
		this.items.put(parentItemObjId, exchangeItem);
	}

	/**
	 * @param countToAdd
	 */
	public void addKinah(long countToAdd) {
		this.kinahCount += countToAdd;
	}

	public boolean isExchangeListFull() {
		return items.size() > 18;
	}

	/**
	 * @param item
	 */
	public void addItemToUpdate(Item item) {
		itemsToUpdate.add(item);
	}
}
