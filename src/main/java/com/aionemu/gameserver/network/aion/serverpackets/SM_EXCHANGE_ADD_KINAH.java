package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 交易添加基纳包：同步己方/对方放入的基纳数量。
 * Exchange add-kinah packet: kinah amount placed by self or other.
 *
 * @author Avol
 */
public class SM_EXCHANGE_ADD_KINAH extends AionServerPacket {

	private long itemCount;
	private int action;

	/**
	 * 构造交易基纳添加包。
	 * Creates an exchange add-kinah packet.
	 *
	 * @param itemCount 添加的基纳数量 / kinah amount to add
	 * @param action 0 为己方，1 为对方 / 0 for self, 1 for other
	 */
	public SM_EXCHANGE_ADD_KINAH(long itemCount, int action) {
		this.itemCount = itemCount;
		this.action = action;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action); // 0 -self 1-other
		writeD((int) itemCount); // itemId
		writeD(0); // 未知 / unk
	}
}
