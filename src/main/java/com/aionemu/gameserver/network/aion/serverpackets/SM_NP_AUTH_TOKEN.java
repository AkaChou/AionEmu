package com.aionemu.gameserver.network.aion.serverpackets;

import java.nio.charset.StandardCharsets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 启动国服客户端的本地 STS 会员流程。
 * Starts the China client's local STS membership flow.
 */
public class SM_NP_AUTH_TOKEN extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(1);
		writeAsciiZ("local-aionemu");
		writeAsciiZ("83C1595C-2932-B33F-DA9B-F26A859BEAB8");
		writeAsciiZ("aion");
		writeH(0x1F6);
		writeC(2);
	}

	private void writeAsciiZ(String value) {
		writeB(value.getBytes(StandardCharsets.US_ASCII));
		writeC(0);
	}
}
