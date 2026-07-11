package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.ingameshop.IGItem;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送商城单个商品详情的服务端包。
 * Server packet that sends a single in-game shop item detail to the client.
 */
public class SM_IN_GAME_SHOP_ITEM extends AionServerPacket {
	private IGItem item;

	/**
	 * 构造商城商品详情包。
	 * Creates an in-game shop item detail packet.
	 *
	 * requesting player
	 * @param objectItem 商城商品对象 ID / in-game shop item object id
	 */
	public SM_IN_GAME_SHOP_ITEM(Player player, int objectItem) {
		item = GameRuntimeServices.inGameShopEn().getIGItem(objectItem);
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(item.getObjectId());
		writeQ(item.getItemPrice());
		writeH(0);
		writeD(item.getItemId());
		writeQ(item.getItemCount());
		writeD(0);
		writeD(item.getGift());
		writeD(item.getItemType());
		writeD(0);
		writeC(0);
		writeH(0);
		writeS(item.getTitleDescription());
		writeS(item.getItemDescription());
	}
}
