package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.ingameshop.IGItem;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 向客户端发送商城商品列表或销售排行的服务端包。
 * Server packet that sends an in-game shop item list or sales ranking to the client.
 */
public class SM_IN_GAME_SHOP_LIST extends AionServerPacket {
	private Player player;
	private int nrList;
	private int salesRanking;
	private IntObjectHashMap<List<IGItem>> allItems = new IntObjectHashMap<List<IGItem>>();

	/**
	 * 构造商城列表包。
	 * Creates an in-game shop list packet.
	 *
	 * target player
	 * list page number
	 * @param salesRanking 列表模式（1=普通列表，其他=销售排行） / list mode (1=normal list, otherwise sales ranking)
	 */
	public SM_IN_GAME_SHOP_LIST(Player player, int nrList, int salesRanking) {
		this.player = player;
		this.nrList = nrList;
		this.salesRanking = salesRanking;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		byte category = player.inGameShop.getCategory();
		byte subCategory = player.inGameShop.getSubCategory();
		if (salesRanking == 1) {
			Collection<IGItem> items = GameRuntimeServices.inGameShopEn().getItems(category);
			int size = 0;
			int tabSize = 9;
			int f = 0;
			for (IGItem a : items) {
				if (subCategory == 2 || a.getSubCategory() == subCategory) {
					if (size == tabSize) {
						tabSize += 9;
						f++;
					}
					List<IGItem> template = allItems.get(f);
					if (template == null) {
						template = new ArrayList<>();
						allItems.put(f, template);
					}
					template.add(a);
					size++;
				}
			}
			List<IGItem> inAllItems = allItems.get(nrList);
			writeD(salesRanking);
			writeD(nrList);
			writeD(size > 0 ? tabSize : 0);
			writeH(inAllItems == null ? 0 : inAllItems.size());
			if (inAllItems != null) {
				for (IGItem item : inAllItems) {
					writeD(item.getObjectId());
				}
			}
		} else {
			List<Integer> salesRankingItems = GameRuntimeServices.inGameShopEn().getTopSales(subCategory, category);
			writeD(salesRanking);
			writeD(nrList);
			writeD((GameRuntimeServices.inGameShopEn().getMaxList(subCategory, category) + 1) * 9);
			writeH(salesRankingItems.size());
			for (int id : salesRankingItems) {
				writeD(id);
			}
		}
	}
}
