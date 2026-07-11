package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 礼包/套餐信息通知服务端包。
 * pack info. / pack info.
 */
public class SM_PACKAGE_INFO_NOTIFY extends AionServerPacket {
	private int count;
	private int packId;
	private int time;

	/**
	 * 构造礼包信息通知包。
	 * Builds a package-info notify packet.
	 *
	 * package count
	 * pack id
	 * @param time 剩余/到期时间 / remaining or expire time
	 */
	public SM_PACKAGE_INFO_NOTIFY(int count, int packId, int time) {
		this.count = count;
		this.packId = packId;
		this.time = time;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player activePlayer = con.getActivePlayer();
		writeH(count);
		writeC(packId);
		writeD(time);
	}
}
