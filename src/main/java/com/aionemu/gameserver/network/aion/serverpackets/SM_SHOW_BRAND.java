package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端显示队伍/联盟标记（Brand）。
 * Server packet displaying a party/alliance brand marker on the client.
 *
 * @author Sweetkr
 */
@AllArgsConstructor
public class SM_SHOW_BRAND extends AionServerPacket {

	private final int brandId;
	private final int targetObjectId;

	@Override
	protected void writeImpl(AionConnection con) {

		writeH(0x01);
		writeD(0x01); // 未知 / unk
		writeD(brandId);
		writeD(targetObjectId);
	}
}
