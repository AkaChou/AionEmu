package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SHOW_NPC_ON_MAP;
import com.aionemu.gameserver.services.teleport.TeleportService2;

import java.util.Collection;
import java.util.List;

/**
 * 在地图上搜索 NPC 出生点的客户端包。
 * Client packet for searching an NPC spawn location on the map.
 */
public class CM_OBJECT_SEARCH extends AionClientPacket {

	private int npcId;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_OBJECT_SEARCH(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);

	}

	@Override
	protected void readImpl() {
		this.npcId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		boolean gm = player != null && player.isGM();
		int preferredWorldId = player == null ? 0 : player.getWorldId();
		SearchTarget target = findSearchTarget(npcId, preferredWorldId, gm);
		if (target == null) {
			return;
		}
		if (gm) {
			if (target.npc() != null) {
				TeleportService2.teleportToNpc(player, target.npc());
			} else {
				TeleportService2.teleportToNpc(player, target.location());
			}
			return;
		}
		SpawnSearchResult searchResult = target.location();
		if (searchResult == null) {
			return;
		}
		sendPacket(new SM_SHOW_NPC_ON_MAP(npcId, searchResult.getWorldId(), searchResult.getSpot().getX(),
				searchResult.getSpot().getY(), searchResult.getSpot().getZ()));
	}

	private static SearchTarget findSearchTarget(int npcId, int preferredWorldId, boolean allowStaticFallback) {
		List<SpawnSearchResult> locations = DataManager.SPAWNS_DATA2
				.getSpawnLocationsByNpcId(preferredWorldId, npcId);
		return selectSearchTarget(npcId, locations, GameWorldBootstrapServices.world().getNpcs(),
				allowStaticFallback);
	}

	static SearchTarget selectSearchTarget(int npcId, List<SpawnSearchResult> locations,
			Collection<? extends Npc> npcs,
			boolean allowStaticFallback) {
		for (SpawnSearchResult location : locations) {
			Npc npc = findLivingNpcAtLocation(npcs, npcId, location);
			if (npc != null) {
				return new SearchTarget(location, npc);
			}
		}
		if (!allowStaticFallback) {
			return null;
		}
		for (Npc npc : npcs) {
			if (isLivingNpc(npc, npcId)) {
				return new SearchTarget(null, npc);
			}
		}
		return locations.isEmpty() ? null : new SearchTarget(locations.getFirst(), null);
	}

	private static Npc findLivingNpcAtLocation(Collection<? extends Npc> npcs, int npcId,
			SpawnSearchResult location) {
		for (Npc npc : npcs) {
			if (!isLivingNpc(npc, npcId)
				|| npc.getWorldId() != location.getWorldId() || npc.getSpawn() == null
				|| !sameLocation(npc, location.getSpot())) {
				continue;
			}
			return npc;
		}
		return null;
	}

	private static boolean isLivingNpc(Npc npc, int npcId) {
		return npc != null && npc.getNpcId() == npcId && npc.isSpawned()
				&& (npc.getLifeStats() == null || !npc.getLifeStats().isAlreadyDead());
	}

	private static boolean sameLocation(Npc npc, SpawnSpotTemplate spot) {
		return sameCoordinates(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ(), spot)
				|| (npc.getPosition() != null && sameCoordinates(npc.getX(), npc.getY(), npc.getZ(), spot));
	}

	private static boolean sameCoordinates(float x, float y, float z, SpawnSpotTemplate spot) {
		return Math.abs(x - spot.getX()) < 0.01f && Math.abs(y - spot.getY()) < 0.01f
			&& Math.abs(z - spot.getZ()) < 0.01f;
	}

	static record SearchTarget(SpawnSearchResult location, Npc npc) {
	}
}
