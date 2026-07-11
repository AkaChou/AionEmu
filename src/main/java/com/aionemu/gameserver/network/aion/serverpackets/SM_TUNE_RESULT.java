package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 鉴定结果服务端包，向客户端发送鉴定后的物品信息。
 * Server packet that sends the item tuning (identification) result to the client.
 */
public class SM_TUNE_RESULT extends AionServerPacket {
	private final Player player;
	private final Item item;
	private final int tuningScrollId;

	/**
	 * 玩家 / player
	 * item
	 * tuning scroll id
	 */
	public SM_TUNE_RESULT(Player player, Item item, int tuningScrollId) {
		this.player = player;
		this.item = item;
		this.tuningScrollId = tuningScrollId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(item.getObjectId());
		writeD(tuningScrollId);
		writeC(item.getBonusNumber());
		ItemInfoBlob.newBlobEntry(ItemBlobType.MANA_SOCKETS, player, item).writeThisBlob(getBuf());
		writeC(0);
		writeC(0);
	}
}
