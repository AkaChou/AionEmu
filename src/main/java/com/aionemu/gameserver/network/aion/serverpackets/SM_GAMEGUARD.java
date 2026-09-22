package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端发送游戏守护校验数据的服务端包。
 * Server packet that sends GameGuard check data to the client.
 * @author Alcapwnd
 */
@AllArgsConstructor
public class SM_GAMEGUARD extends AionServerPacket {

	private final int size;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(size);
		writeB(new byte[size]);
	}
}
