package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步欧比斯神器/要塞位置精简信息的服务端包（第二形态）。
 * Server packet synchronizing compact Abyss artifact/fortress location info to the client (variant 2).
 */
public class SM_ABYSS_ARTIFACT_INFO2 extends AionServerPacket {

	private Collection<SiegeLocation> locations;

	/**
	 * 使用围城位置集合构造精简神器信息包。
	 * Creates a compact artifact info packet from a collection of siege locations.
	 *
	 * @param collection 围城位置集合 / siege locations
	 */
	public SM_ABYSS_ARTIFACT_INFO2(Collection<SiegeLocation> collection) {
		this.locations = collection;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		List<SiegeLocation> validLocations = new ArrayList<>();
		for (SiegeLocation loc : locations) {
			if (((loc.getType() == SiegeType.ARTIFACT) || (loc.getType() == SiegeType.FORTRESS))
					&& (loc.getLocationId() >= 1011) && (loc.getLocationId() < 2000))
				validLocations.add(loc);
		}
		writeH(validLocations.size());
		for (SiegeLocation loc : validLocations) {
			writeD(loc.getLocationId());
			writeC(0);
		}
	}
}
