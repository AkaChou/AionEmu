package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步玩家姿态（格挡、飞行、滑翔、跳跃、静止物体等）的服务端包。
 * Server packet that synchronizes a player's stance (block, flight, glide, jump, stationary object, etc.).
 *
 * @author prix
 */
@AllArgsConstructor
public class SM_PLAYER_STANCE extends AionServerPacket {

	private final Player player;
	private final int state;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());
		writeC(state);
	}
}
