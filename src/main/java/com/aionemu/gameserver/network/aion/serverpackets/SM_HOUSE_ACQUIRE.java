package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端玩家获得或失去房屋的服务端包。
 * Server packet that notifies the client of a player acquiring or losing a house.
 */
public class SM_HOUSE_ACQUIRE extends AionServerPacket {

	private int playerId;
	private int address;
	private boolean acquire;

	/**
	 * 构造房屋获得/失去通知包。
	 * Creates a house acquire/release notification packet.
	 *
	 * player id
	 * house address id
	 * @param acquire 是否获得（true=获得，false=失去） / whether acquired (true=acquire, false=release)
	 */
	public SM_HOUSE_ACQUIRE(int playerId, int address, boolean acquire) {
		this.playerId = playerId;
		this.address = address;
		this.acquire = acquire;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerId);
		writeD(address);
		writeD(acquire ? 1 : 0);
	}
}
