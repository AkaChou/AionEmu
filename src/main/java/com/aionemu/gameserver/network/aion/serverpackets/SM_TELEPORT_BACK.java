package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端执行“传送回退”的空载荷服务端包。
 * Empty-payload server packet that notifies the client to perform a teleport-back.
 *
 * @author FrozenKiller
 */
public class SM_TELEPORT_BACK extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		// 空数据包 / EmptyPacket
	}
}
