package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端分片发送问卷/调查 HTML 内容（最多 255 片）。
 * Server packet that sends survey HTML to the client in chunks (up to 255).
 *
 * @author lhw and Kaipo
 */
@AllArgsConstructor
public class SM_QUESTIONNAIRE extends AionServerPacket {

	private final int messageId;
	private final byte chunk;
	private final byte count;
	private final String html;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(messageId);
		writeC(chunk);
		writeC(count);
		writeH(html.length() * 2);
		writeS(html);
	}
}
