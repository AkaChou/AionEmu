package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PrivateStore;
import com.aionemu.gameserver.model.trade.TradePSItem;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;

/**
 * 向客户端同步玩家个人商店的出售物品列表。
 * Server packet synchronizing a player's private-store sell list to the client.
 *
 * @author Simple
 */
public class SM_PRIVATE_STORE extends AionServerPacket {

	private Player player;
	/**
	 * Private store Information *
	 */
	private PrivateStore store;

	/**
	 * 使用给定参数构造 SM_PRIVATE_STORE 包。
	 * Creates a SM_PRIVATE_STORE packet with the given parameters.
	 *
	 * @param store 个人商店 / private store
	 * 玩家 / player
	 */
	public SM_PRIVATE_STORE(PrivateStore store, Player player) {
		this.player = player;
		this.store = store;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if (store != null) {
			Player storePlayer = store.getOwner();
			synchronized (storePlayer) {
				LinkedHashMap<Integer, TradePSItem> soldItems = store.getSoldItems();

				writeD(storePlayer.getObjectId());
				writeH(soldItems.size());
				for (Entry<Integer, TradePSItem> entry : soldItems.entrySet()) {
					int itemObjId = entry.getKey();
					Item item = storePlayer.getInventory().getItemByObjId(itemObjId);
					TradePSItem tradeItem = entry.getValue();
					long price = tradeItem.getPrice();
					writeD(itemObjId);
					writeD(item.getItemTemplate().getTemplateId());
					writeH((int) tradeItem.getCount());
					writeQ((int) price);

					ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
					itemInfoBlob.writeMe(getBuf());
				}
			}
		}
	}
}
