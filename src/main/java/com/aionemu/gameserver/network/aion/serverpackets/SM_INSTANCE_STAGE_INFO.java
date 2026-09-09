package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步副本阶段/事件进度信息的服务端包。
 * Server packet synchronizing instance stage/event progress info to the client.
 *
 * @author xTz
 */
@AllArgsConstructor
public class SM_INSTANCE_STAGE_INFO extends AionServerPacket {

	private final int type;
	private final int event;
	private final int unk;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(type);
		writeD(0);
		writeH(event);
		writeH(unk);
	}
}
