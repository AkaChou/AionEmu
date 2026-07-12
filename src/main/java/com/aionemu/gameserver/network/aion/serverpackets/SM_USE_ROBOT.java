package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步玩家使用/切换机器人（Aethertech）外观的服务端包。
 * switching robot (Aethertech) appearance.
 *
 * @author Ranastic
 */
public class SM_USE_ROBOT extends AionServerPacket {

	private Player player;
	private int robotInfo;

	/**
	 * 玩家 / player
	 * @param robotInfo 机器人信息/ID / robot info / id
	 */
	public SM_USE_ROBOT(Player player, int robotInfo) {
		this.player = player;
		this.robotInfo = robotInfo;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());
		writeD(robotInfo);
	}
}
