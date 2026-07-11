package com.aionemu.gameserver.model.trade;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.RepurchaseService;

/**
 * Repurchase 列表，用于交易相关逻辑。
 * Repurchase List for trade logic.
 *
 * @author xTz
 */
public class RepurchaseList {

	private final int sellerObjId;
	private List<Item> repurchases = new ArrayList<Item>();

	public RepurchaseList(int sellerObjId) {
		this.sellerObjId = sellerObjId;
	}

	/**
	 * @param player
	 * @param itemObjectId
	 * @param count
	 */
	public void addRepurchaseItem(Player player, int itemObjectId, long count) {
		Item item = GameFeatureServices.repurchaseService().getRepurchaseItem(player, itemObjectId);
		if (item != null) {
			repurchases.add(item);
		}
	}

	/**
	 * @return the tradeItems
	 */
	public List<Item> getRepurchaseItems() {
		return repurchases;
	}

	/** 大小 / size. */
	public int size() {
		return repurchases.size();
	}

	/** 返回 seller obj id / Returns the seller obj id */
	public final int getSellerObjId() {
		return sellerObjId;
	}
}
