package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 礼包/套餐信息通知服务端包。
 * pack info.
 */
@AllArgsConstructor
public class SM_PACKAGE_INFO_NOTIFY extends AionServerPacket {
	private final int count;
	private final int packId;
	private final int time;

	@Override
	protected void writeImpl(AionConnection con) {
		Player activePlayer = con.getActivePlayer();
		writeH(count);
		writeC(packId);
		writeD(time);
	}
}
