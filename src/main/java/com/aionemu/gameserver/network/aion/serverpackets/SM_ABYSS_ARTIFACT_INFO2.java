package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步欧比斯神器/要塞位置精简信息的服务端包（第二形态）。
 * Server packet synchronizing compact Abyss artifact/fortress location info to the client (variant 2).
 */
@AllArgsConstructor
public class SM_ABYSS_ARTIFACT_INFO2 extends AionServerPacket {

	private final Collection<SiegeLocation> locations;

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
