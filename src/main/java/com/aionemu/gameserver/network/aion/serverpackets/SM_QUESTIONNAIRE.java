package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端分片发送问卷/调查 HTML 内容（最多 255 片）。
 * Server packet that sends survey HTML to the client in chunks (up to 255).
 *
 * @author lhw and Kaipo
 */
public class SM_QUESTIONNAIRE extends AionServerPacket {

	private int messageId;
	private byte chunk;
	private byte count;
	private String html;

	/**
	 * 使用给定参数构造 SM_QUESTIONNAIRE 包。
	 * Creates a SM_QUESTIONNAIRE packet with the given parameters.
	 *
	 * message id
	 * @param chunk 分片序号 / chunk index
	 * @param count 分片总数 / chunk count
	 * HTML content
	 */
	public SM_QUESTIONNAIRE(int messageId, byte chunk, byte count, String html) {
		this.messageId = messageId;
		this.chunk = chunk;
		this.count = count;
		this.html = html;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(messageId);
		writeC(chunk);
		writeC(count);
		writeH(html.length() * 2);
		writeS(html);
	}
}
