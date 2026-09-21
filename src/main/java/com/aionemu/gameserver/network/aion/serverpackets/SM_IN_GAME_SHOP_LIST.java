package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.ingameshop.IGItem;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import lombok.RequiredArgsConstructor;

/**
 * 向客户端发送商城商品列表或销售排行的服务端包。
 * Server packet that sends an in-game shop item list or sales ranking to the client.
 */
@RequiredArgsConstructor
public class SM_IN_GAME_SHOP_LIST extends AionServerPacket {
	private final Player player;
	private final int nrList;
	private final int salesRanking;
	private final IntObjectHashMap<List<IGItem>> allItems = new IntObjectHashMap<List<IGItem>>();

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
