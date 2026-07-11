package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 允许进入游戏世界的服务端包，响应 {@code CM_MAY_LOGIN_INTO_GAME}。
 * Server packet granting permission to enter the game world; answers {@code CM_MAY_LOGIN_INTO_GAME}.
 *
 * @author -Nemesiss-
 */
public class SM_MAY_LOGIN_INTO_GAME extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0);
	}
}
