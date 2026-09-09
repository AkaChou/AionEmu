package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步玩家 Toll（商城点券）余额的服务端包。
 * Server packet that syncs the player's Toll (cash shop currency) balance.
 *
 * @author xTz
 */
@AllArgsConstructor
public class SM_TOLL_INFO extends AionServerPacket {

	private final long tollCount;

	@Override
	protected void writeImpl(AionConnection con) {
		writeQ(tollCount);
	}
}
