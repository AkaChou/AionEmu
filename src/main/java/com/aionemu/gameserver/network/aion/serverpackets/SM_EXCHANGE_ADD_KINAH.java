package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 交易添加基纳包：同步己方/对方放入的基纳数量。
 * Exchange add-kinah packet: kinah amount placed by self or other.
 * @author Avol
 */
@AllArgsConstructor
public class SM_EXCHANGE_ADD_KINAH extends AionServerPacket {

	private final long itemCount;
	private final int action;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action); // 0 -self 1-other
		writeD((int) itemCount); // itemId
		writeD(0); // 未知 / unk
	}
}
