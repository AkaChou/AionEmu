package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 昵称可用性检查响应服务端包。
 * Server packet that responds to {@code CM_CHECK_NICKNAME} with nickname availability.
 * <p>
 * 响应值示例：0x00=可用，0x0A=不可用（还有更多状态码）。
 * Response values e.g.: 0x00=ok, 0x0A=not ok (and more status codes).
 * @author -Nemesiss-
 */
@AllArgsConstructor
public class SM_NICKNAME_CHECK_RESPONSE extends AionServerPacket {

	/** 响应状态值 / response status value */
	private final int value;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
        writeC(value);
	}
}
