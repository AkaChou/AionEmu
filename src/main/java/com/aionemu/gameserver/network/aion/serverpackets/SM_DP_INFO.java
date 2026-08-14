package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * DP（斗志点）信息包：同步玩家当前 DP 值。
 * DP (divine power) info packet: current DP of a player.
 *
 * @author Sweetkr
 */
public class SM_DP_INFO extends AionServerPacket {

	private int playerObjectId;
	private int currentDp;

	/**
	 * 构造 DP 信息包。
	 * Creates a DP info packet.
	 *
	 * @param playerObjectId 玩家对象 ID / player object id
	 * @param currentDp 当前 DP 值 / current DP value
	 */
	public SM_DP_INFO(int playerObjectId, int currentDp) {
		this.playerObjectId = playerObjectId;
		this.currentDp = currentDp;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjectId);
		writeH(currentDp);
	}
}
