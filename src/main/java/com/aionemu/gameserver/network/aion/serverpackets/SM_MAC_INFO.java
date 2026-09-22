package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 客户端 MAC/硬件信息同步的服务端包。
 * Server packet that carries client MAC/hardware information.
 * @author Ranastic
 */
@AllArgsConstructor
public class SM_MAC_INFO extends AionServerPacket {

	private final String macAddress;
	private final String hardName;
	private final int localIP;

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(macAddress);
		writeS(hardName);
		writeD(localIP);
	}
}
