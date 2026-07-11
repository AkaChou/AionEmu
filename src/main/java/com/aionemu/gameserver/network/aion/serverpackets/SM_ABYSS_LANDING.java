package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.AbyssLandingService;

/**
 * 向客户端同步欧比斯着陆点各评分项（任务、要塞、神器等）的服务端包。
 * Server packet synchronizing Abyss landing location score categories (quest, fortress, artifact, etc.) to the client.
 *
 * @author Ranastic & Lightning (Encom)
 */
public class SM_ABYSS_LANDING extends AionServerPacket {
	private Map<Integer, LandingLocation> locations;

	/**
	 * 从 {@link AbyssLandingService} 拉取全部着陆点并构造同步包。
	 * Loads all landing locations from {@link AbyssLandingService} and builds the sync packet.
	 */
	public SM_ABYSS_LANDING() {
		locations = AbyssLandingService.getLandingLocations();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		for (LandingLocation loc : locations.values()) {
			writeD(loc.getQuestPoints()); // Quest Completion.
			writeD(loc.getSiegePoints()); // Fortress Occupation.
			writeD(loc.getArtifactPoints()); // Artifact Occupation.
			writeD(loc.getBasePoints()); // Base Occupation.
			writeD(loc.getFacilityPoints()); // Facility Control.
			writeD(loc.getMonumentsPoints()); // Monument Control.
			writeD(loc.getCommanderPoints()); // Commander Defense.
		}
	}
}
