package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 回应战场联盟报名/取消报名结果的服务端包。
 * unregister results.
 * @author wanke
 */
@AllArgsConstructor
public class SM_BATTLEFIELD_UNION_REGISTER extends AionServerPacket {
	int requestId;
	boolean isRegister;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(requestId);
		writeC(isRegister ? 0 : 1);
	}
}
