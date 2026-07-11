package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 宏创建/删除操作结果的服务端包。
 * Server packet for macro create/delete operation results.
 *
 * @author xavier
 */
public class SM_MACRO_RESULT extends AionServerPacket {

	public static SM_MACRO_RESULT SM_MACRO_CREATED = new SM_MACRO_RESULT(0x00);
	public static SM_MACRO_RESULT SM_MACRO_DELETED = new SM_MACRO_RESULT(0x01);
	private int code;

	/**
	 * 构造宏操作结果包。
	 * Builds a macro operation result packet.
	 *
	 * @param code 结果码（0 创建成功 / 1 删除成功） / result code (0 created / 1 deleted)
	 */
	public SM_MACRO_RESULT(int code) {
		this.code = code;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(code);
	}
}
