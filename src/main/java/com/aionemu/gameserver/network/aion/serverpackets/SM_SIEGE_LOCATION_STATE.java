package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步攻城地点状态变化。
 * Server packet synchronizing a siege location state change to the client.
 */
@AllArgsConstructor
public class SM_SIEGE_LOCATION_STATE extends AionServerPacket {

	private final int locationId;
	private final int state;

	/**
	 * 使用给定参数构造 SM_SIEGE_LOCATION_STATE 包。
	 * Creates a SM_SIEGE_LOCATION_STATE packet with the given parameters.
	 * @param location location id
	 */
	public SM_SIEGE_LOCATION_STATE(SiegeLocation location) {
		locationId = location.getLocationId();
		state = (location.isVulnerable() ? 1 : 0);
	}

	protected void writeImpl(AionConnection con) {
		writeD(locationId);
		writeC(state);
	}
}
