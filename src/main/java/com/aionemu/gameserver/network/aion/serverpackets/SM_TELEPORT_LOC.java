package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端传送到指定地图/实例坐标的服务端包。
 * Server packet that teleports the client to a map/instance location.
 */
public class SM_TELEPORT_LOC extends AionServerPacket {
	private int portAnimation;
	private int mapId;
	private int instanceId;
	private float x;
	private float y;
	private float z;
	private byte heading;
	private boolean isInstance;

	/**
	 * @param isInstance    是否为实例 / whether the destination is an instance
	 * instance id
	 * map id
	 * @param x             X 坐标 / x coordinate
	 * @param y             Y 坐标 / y coordinate
	 * @param z             Z 坐标 / z coordinate
	 * 朝向 / heading
	 * @param portAnimation 传送动画（字段保留，当前未写出） / port animation (stored, currently not written)
	 */
	public SM_TELEPORT_LOC(boolean isInstance, int instanceId, int mapId, float x, float y, float z, byte heading,
			int portAnimation) {
		this.isInstance = isInstance;
		this.instanceId = instanceId;
		this.mapId = mapId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
		this.portAnimation = portAnimation;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0x03);
		writeD(mapId); // 4.3
		writeD(isInstance ? instanceId : mapId);
		writeF(x);
		writeF(y);
		writeF(z);
		writeC(heading);
	}
}
