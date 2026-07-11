package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 强制同步玩家坐标与朝向的服务端包。
 * Server packet that force-syncs the player's coordinates and heading.
 *
 * @author cura
 */
public class SM_PLAYER_MOVE extends AionServerPacket {

	private float x;
	private float y;
	private float z;
	private byte heading;

	/**
	 * @param x 目标 X 坐标 / destination X
	 * @param y 目标 Y 坐标 / destination Y
	 * @param z 目标 Z 坐标 / destination Z
	 * 朝向 / heading
	 */
	public SM_PLAYER_MOVE(float x, float y, float z, byte heading) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeF(x);
		writeF(y);
		writeF(z);
		writeC(heading);
	}
}
