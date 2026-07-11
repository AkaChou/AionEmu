package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 删除房屋内物件包：按物品对象 ID 移除房屋摆设。
 * Server packet that removes a house object/item by object id.
 */
public class SM_DELETE_HOUSE_OBJECT extends AionServerPacket {

	private int itemObjectId;

	public SM_DELETE_HOUSE_OBJECT(int itemObjectId) {
		this.itemObjectId = itemObjectId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(itemObjectId);
	}
}
