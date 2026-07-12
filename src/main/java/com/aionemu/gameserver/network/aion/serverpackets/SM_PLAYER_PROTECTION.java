package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步玩家保护（新手/登入保护）剩余时间的服务端包。
 * login shield) time.
 *
 * Created by wanke on 16/05/2017.
 */
public class SM_PLAYER_PROTECTION extends AionServerPacket {

	private int time;

	/**
	 * @param time 保护剩余时间（秒或协议约定单位） / remaining protection duration
	 */
	public SM_PLAYER_PROTECTION(int time) {
		this.time = time;
	}

	protected void writeImpl(AionConnection con) {
		writeD(time);
	}
}
