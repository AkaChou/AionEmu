package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 对 CM_QUIT 的应答，通知客户端退出或进入编辑模式结果。
 * Response to CM_QUIT notifying the client of quit or edit-mode outcome.
 *
 * @author -Nemesiss-
 */
public class SM_QUIT_RESPONSE extends AionServerPacket {

	private boolean edit_mode = false;

	/**
	 * 构造默认的 SM_QUIT_RESPONSE 包。
	 * Creates a default SM_QUIT_RESPONSE packet.
	 */
	public SM_QUIT_RESPONSE() {
	}

	/**
	 * 使用给定参数构造 SM_QUIT_RESPONSE 包。
	 * Creates a SM_QUIT_RESPONSE packet with the given parameters.
	 *
	 * @param edit_mode 是否编辑模式 / edit mode flag
	 */
	public SM_QUIT_RESPONSE(boolean edit_mode) {
		this.edit_mode = edit_mode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(edit_mode ? 2 : 1);// 1 normal, 2 plastic surgery/gender switch
		writeC(0x00);// 未知 / unk
		writeC(0xFF);// why sometime 0x2e?
		writeC(0xFF);
		writeC(0xFF);
		writeC(0xFF);
	}
}
