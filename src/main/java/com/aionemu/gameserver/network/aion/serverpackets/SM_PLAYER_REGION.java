package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.world.zone.ZoneName;
import lombok.AllArgsConstructor;

/**
 * 同步玩家当前子区域（Zone）信息的服务端包。
 * Server packet that synchronizes the player's current sub-zone information.
 * @author LightNing
 */
@AllArgsConstructor
public class SM_PLAYER_REGION extends AionServerPacket {

	private final ZoneName subZone;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(con.getActivePlayer().getObjectId());
		writeC(0);
		writeC(0);
		writeC(0);
		writeD(subZone.name().hashCode());
	}
}
