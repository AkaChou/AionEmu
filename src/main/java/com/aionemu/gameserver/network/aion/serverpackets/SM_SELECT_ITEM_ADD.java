package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端确认已选择的物品添加结果。
 * Server packet confirming the result of adding a selected item on the client.
 */
public class SM_SELECT_ITEM_ADD extends AionServerPacket {
	private int uniqueItemId;
	private int type;

	/**
	 * 使用给定参数构造 SM_SELECT_ITEM_ADD 包。
	 * Creates a SM_SELECT_ITEM_ADD packet with the given parameters.
	 *
	 * unique item id
	 * type
	 */
	public SM_SELECT_ITEM_ADD(int uniqueItemId, int type) {
		this.uniqueItemId = uniqueItemId;
		this.type = type;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(uniqueItemId);
		writeD(0x00);
		writeC(type);
	}
}
