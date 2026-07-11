package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步玩家姿态（格挡、飞行、滑翔、跳跃、静止物体等）的服务端包。
 * Server packet that synchronizes a player's stance (block, flight, glide, jump, stationary object, etc.).
 *
 * @author prix
 */
public class SM_PLAYER_STANCE extends AionServerPacket {

	private Player player;
	private int state;

	/**
	 * target player
	 * @param state 姿态状态：0=关闭，1=格挡/飞行/滑翔/跳跃等，2=静止物体 / stance: 0=off, 1=block/flight/glide/jump, 2=stationary object
	 */
	public SM_PLAYER_STANCE(Player player, int state) {
		this.player = player;
		this.state = state; // 0 = off, 1 = block, flight, glide, jump, etc.
		// 2 = 静止物体 / 2 = stationary object
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());
		writeC(state);
	}
}
