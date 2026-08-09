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
		SpawnSearchResult searchResult = findSearchResult(npcId);
		if (searchResult != null) {
			sendPacket(new SM_SHOW_NPC_ON_MAP(npcId, searchResult.getWorldId(), searchResult.getSpot().getX(),
					searchResult.getSpot().getY(), searchResult.getSpot().getZ()));
			if (player.isGM()) {
				TeleportService2.teleportToNpc(player, searchResult);
			}
		}
	}

	private static SpawnSearchResult findSearchResult(int npcId) {
		List<SpawnSearchResult> locations = DataManager.SPAWNS_DATA2.getSpawnLocationsByNpcId(0, npcId);
		if (locations.isEmpty()) {
			return null;
		}
		Collection<Npc> npcs = GameWorldBootstrapServices.world().getNpcs();
		for (SpawnSearchResult location : locations) {
			if (hasNpcAtLocation(npcs, npcId, location, true)) {
				return location;
			}
			// 没有运行时对象时无法证明该刷怪点已死亡，保留静态点作为搜索回退。
			if (!hasNpcAtLocation(npcs, npcId, location, false)) {
				return location;
			}
		}
		return locations.getFirst();
	}

	private static boolean hasNpcAtLocation(Collection<Npc> npcs, int npcId, SpawnSearchResult location,
		boolean aliveOnly) {
		for (Npc npc : npcs) {
			if (npc == null || npc.getNpcId() != npcId || !npc.isSpawned()
				|| npc.getWorldId() != location.getWorldId() || npc.getSpawn() == null
				|| !sameLocation(npc, location.getSpot())) {
				continue;
			}
			boolean dead = npc.getLifeStats() != null && npc.getLifeStats().isAlreadyDead();
			if (!aliveOnly || !dead) {
				return true;
			}
		}
		return false;
	}

	private static boolean sameLocation(Npc npc, SpawnSpotTemplate spot) {
		return sameCoordinates(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ(), spot)
			|| sameCoordinates(npc.getX(), npc.getY(), npc.getZ(), spot);
	}

	private static boolean sameCoordinates(float x, float y, float z, SpawnSpotTemplate spot) {
		return Math.abs(x - spot.getX()) < 0.01f && Math.abs(y - spot.getY()) < 0.01f
			&& Math.abs(z - spot.getZ()) < 0.01f;
	}
}
