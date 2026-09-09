package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 强制同步玩家坐标与朝向的服务端包。
 * Server packet that force-syncs the player's coordinates and heading.
 *
 * @author cura
 */
@AllArgsConstructor
public class SM_PLAYER_MOVE extends AionServerPacket {

	private final float x;
	private final float y;
	private final float z;
	private final byte heading;

	@Override
	protected void writeImpl(AionConnection con) {
		writeF(x);
		writeF(y);
		writeF(z);
		writeC(heading);
	}
}
