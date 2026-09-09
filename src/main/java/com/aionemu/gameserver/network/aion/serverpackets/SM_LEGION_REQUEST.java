package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 回复客户端军团加入申请处理结果的服务端包。
 * Server packet replying with the result of a legion join-request decision.
 */
@AllArgsConstructor
public class SM_LEGION_REQUEST extends AionServerPacket {
	private final int requesterId;
	private final boolean allowed;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(requesterId);
		writeC(allowed ? 1 : 0);
	}
}
