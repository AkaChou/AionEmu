package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 广播风道（Windstream）开启/关闭状态的服务端包。
 * Server packet that announces windstream open/close state.
 *
 * @author LokiReborn
 */
public class SM_WINDSTREAM_ANNOUNCE extends AionServerPacket {
	private int bidirectional;
	private int mapId;
	private int streamId;
	private int state;

	/**
	 * whether bidirectional
	 * map id
	 * stream id
	 * state
	 */
	public SM_WINDSTREAM_ANNOUNCE(int bidirectional, int mapId, int streamId, int state) {
		this.bidirectional = bidirectional;
		this.mapId = mapId;
		this.streamId = streamId;
		this.state = state;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(bidirectional);
		writeD(mapId);
		writeD(streamId);
		writeC(state);
	}
}
