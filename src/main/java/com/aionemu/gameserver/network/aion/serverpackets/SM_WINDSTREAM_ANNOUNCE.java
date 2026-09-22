package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 广播风道（Windstream）开启/关闭状态的服务端包。
 * Server packet that announces windstream open/close state.
 * @author LokiReborn
 */
@AllArgsConstructor
public class SM_WINDSTREAM_ANNOUNCE extends AionServerPacket {
	private final int bidirectional;
	private final int mapId;
	private final int streamId;
	private final int state;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(bidirectional);
		writeD(mapId);
		writeD(streamId);
		writeC(state);
	}
}
