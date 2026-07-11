package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.Collection;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.SiegeService;

/**
 * 向客户端同步要塞护盾特效状态。
 * Server packet synchronizing fortress shield effect state to the client.
 */
public class SM_SHIELD_EFFECT extends AionServerPacket {
	private Collection<SiegeLocation> locations;

	/**
	 * 使用给定参数构造 SM_SHIELD_EFFECT 包。
	 * Creates a SM_SHIELD_EFFECT packet with the given parameters.
	 *
	 * @param locations 攻城地点集合 / siege locations
	 */
	public SM_SHIELD_EFFECT(Collection<SiegeLocation> locations) {
		this.locations = locations;
	}

	/**
	 * 使用给定参数构造 SM_SHIELD_EFFECT 包。
	 * Creates a SM_SHIELD_EFFECT packet with the given parameters.
	 *
	 * location id
	 */
	public SM_SHIELD_EFFECT(int location) {
		this.locations = new ArrayList<SiegeLocation>();
		this.locations.add(GameFeatureServices.siegeService().getSiegeLocation(location));
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(locations.size());
		for (SiegeLocation loc : locations) {
			writeD(loc.getLocationId());
			writeC(loc.isUnderShield() ? 1 : 0);
		}
	}
}
