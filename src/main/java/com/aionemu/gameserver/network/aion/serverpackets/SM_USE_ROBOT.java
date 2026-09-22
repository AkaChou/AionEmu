package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步玩家使用/切换机器人（Aethertech）外观的服务端包。
 * switching robot (Aethertech) appearance.
 * @author Ranastic
 */
@AllArgsConstructor
public class SM_USE_ROBOT extends AionServerPacket {

	private final Player player;
	private final int robotInfo;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());
		writeD(robotInfo);
	}
}
