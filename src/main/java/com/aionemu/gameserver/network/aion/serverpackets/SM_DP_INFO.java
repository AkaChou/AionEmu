package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * DP（斗志点）信息包：同步玩家当前 DP 值。
 * DP (divine power) info packet: current DP of a player.
 *
 * @author Sweetkr
 */
@AllArgsConstructor
public class SM_DP_INFO extends AionServerPacket {

	private final int playerObjectId;
	private final int currentDp;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjectId);
		writeH(currentDp);
	}
}
