package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送游戏守护校验数据的服务端包。
 * Server packet that sends GameGuard check data to the client.
 *
 * @author Alcapwnd
 */
public class SM_GAMEGUARD extends AionServerPacket {

	private int size;

	/**
	 * @param size 校验数据缓冲区大小 / Size of the check data buffer
	 */
	public SM_GAMEGUARD(int size) {
		this.size = size;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(size);
		writeB(new byte[size]);
	}
}
