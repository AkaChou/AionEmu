package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 推送活动 YouTube 视频链接的服务端包。
 * Server packet that pushes an event YouTube video link.
 *
 * @author Rinzler (Encom)
 */
public class SM_YOUTUBE_VIDEO extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		String videoString = EventsConfig.EVENT_YOUTUBE_VIDEO;
		writeH(1);
		writeD((int) (System.currentTimeMillis() / 1000));
		writeS(videoString);
		writeB(new byte[8]);
		writeS("1532029853");
	}
}
