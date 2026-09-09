package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 进入世界检查响应包：向客户端回传进入世界的校验结果码。
 * Enter-world check response: result code for world-entry validation.
 *
 * @author -Nemesiss-
 */
@NoArgsConstructor
@AllArgsConstructor
public class SM_ENTER_WORLD_CHECK extends AionServerPacket {

	private byte msg = 0x00;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(msg);
		writeC(0x00);
		writeC(0x00);
	}
}
