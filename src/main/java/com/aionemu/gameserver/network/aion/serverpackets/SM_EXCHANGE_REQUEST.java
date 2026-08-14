package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 交易请求包：向目标展示发起交易的玩家名。
 * Exchange request packet: shows the requester name to the target.
 *
 * @author -Avol-
 */
public class SM_EXCHANGE_REQUEST extends AionServerPacket {

	private String receiver;

	/**
	 * 按发起者名称构造交易请求包。
	 * Creates an exchange request packet for the given requester name.
	 *
	 * @param receiver 发起交易玩家名 / name of the requesting player
	 */
	public SM_EXCHANGE_REQUEST(String receiver) {
		this.receiver = receiver;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(receiver);
	}
}
