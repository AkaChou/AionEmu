package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 回应战场联盟报名/取消报名结果的服务端包。
 * unregister results. / unregister results.
 *
 * @author wanke
 */
public class SM_BATTLEFIELD_UNION_REGISTER extends AionServerPacket {
	int requestId;
	boolean isRegister;

	/**
	 * request id
	 * true = register, false = unregister。 / true = register, false = unregister
	 */
	public SM_BATTLEFIELD_UNION_REGISTER(int requestId, boolean register) {
		this.requestId = requestId;
		this.isRegister = register;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(requestId);
		writeC(isRegister ? 0 : 1);
	}
}
