package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 宏创建/删除操作结果的服务端包。
 * Server packet for macro create/delete operation results.
 *
 * @author xavier
 */
@AllArgsConstructor
public class SM_MACRO_RESULT extends AionServerPacket {

	public static SM_MACRO_RESULT SM_MACRO_CREATED = new SM_MACRO_RESULT(0x00);
	public static SM_MACRO_RESULT SM_MACRO_DELETED = new SM_MACRO_RESULT(0x01);
	private final int code;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(code);
	}
}
