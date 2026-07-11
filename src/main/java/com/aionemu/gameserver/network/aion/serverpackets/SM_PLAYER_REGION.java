package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 同步玩家当前子区域（Zone）信息的服务端包。
 * Server packet that synchronizes the player's current sub-zone information.
 *
 * @author LightNing
 */
public class SM_PLAYER_REGION extends AionServerPacket {

	private final ZoneName subZone;

	/**
	 * @param subZone 当前子区域名 / current sub-zone name
	 */
	public SM_PLAYER_REGION(ZoneName subZone) {
		this.subZone = subZone;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(con.getActivePlayer().getObjectId());
		writeC(0);
		writeC(0);
		writeC(0);
		writeD(subZone.name().hashCode());
	}
}
