package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端确认已选择的物品添加结果。
 * Server packet confirming the result of adding a selected item on the client.
 */
@AllArgsConstructor
public class SM_SELECT_ITEM_ADD extends AionServerPacket {
	private final int uniqueItemId;
	private final int type;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(uniqueItemId);
		writeD(0x00);
		writeC(type);
	}
}
