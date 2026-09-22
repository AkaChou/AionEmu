package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步玩家保护（新手/登入保护）剩余时间的服务端包。
 * login shield) time.
 * Created by wanke on 16/05/2017.
 */
@AllArgsConstructor
public class SM_PLAYER_PROTECTION extends AionServerPacket {

	private final int time;

	protected void writeImpl(AionConnection con) {
		writeD(time);
	}
}
