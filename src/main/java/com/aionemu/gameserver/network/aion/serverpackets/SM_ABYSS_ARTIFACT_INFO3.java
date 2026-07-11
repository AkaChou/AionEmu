package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.Collection;

import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.SiegeService;

/**
 * 向客户端同步欧比斯神器状态（含传送可用性）的服务端包。
 * Server packet synchronizing Abyss artifact status, including teleport availability, to the client.
 */
public class SM_ABYSS_ARTIFACT_INFO3 extends AionServerPacket {
	private boolean teleportStatus;
	private Collection<ArtifactLocation> locations;

	/**
	 * 使用神器位置集合构造状态包。
	 * Creates a status packet from a collection of artifact locations.
	 *
	 * @param collection 神器位置集合 / artifact locations
	 */
	public SM_ABYSS_ARTIFACT_INFO3(Collection<ArtifactLocation> collection) {
		this.locations = collection;
	}

	/**
	 * 按位置 ID 查询单个神器并构造状态包。
	 * Looks up a single artifact by location id and builds a status packet.
	 *
	 * @param loc 神器位置 ID / artifact location id
	 */
	public SM_ABYSS_ARTIFACT_INFO3(int loc) {
		locations = new ArrayList<ArtifactLocation>();
		locations.add(GameFeatureServices.siegeService().getArtifact(loc));
	}

	/**
	 * 按位置 ID 查询单个神器，并附带传送状态。
	 * Looks up a single artifact by location id and attaches teleport status.
	 *
	 * artifact location id
	 * @param teleportStatus 是否允许传送 / whether teleport is available
	 */
	public SM_ABYSS_ARTIFACT_INFO3(int locationId, boolean teleportStatus) {
		locations = new ArrayList<ArtifactLocation>();
		locations.add(GameFeatureServices.siegeService().getArtifact(locationId));
		this.teleportStatus = teleportStatus;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(locations.size());
		for (ArtifactLocation artifact : locations) {
			writeD(artifact.getLocationId() * 10 + 1);
			writeC(artifact.getStatus().getValue());
			writeD(0);
			writeC(teleportStatus ? 1 : 0);
		}
	}
}
