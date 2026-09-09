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
 * 向客户端同步欧比斯神器/要塞位置基础信息的服务端包。
 * Server packet synchronizing basic Abyss artifact/fortress location info to the client.
 */
@AllArgsConstructor
public class SM_ABYSS_ARTIFACT_INFO extends AionServerPacket {

	private final Collection<SiegeLocation> locations;

	@Override
	protected void writeImpl(AionConnection con) {
		List<SiegeLocation> validLocations = new ArrayList<>();
		for (SiegeLocation loc : locations) {
			if (((loc.getType() == SiegeType.ARTIFACT) || (loc.getType() == SiegeType.FORTRESS))
					&& (loc.getLocationId() >= 1011) && (loc.getLocationId() <= 10412))
				validLocations.add(loc);
		}
		writeH(validLocations.size());
		for (SiegeLocation loc : validLocations) {
			writeD(loc.getLocationId());
			writeD(0); // 未知 / unk
			writeD(0); // 未知 / unk
		}
	}
}
