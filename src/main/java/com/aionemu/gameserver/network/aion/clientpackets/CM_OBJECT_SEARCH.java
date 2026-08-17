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
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;

import java.util.Collection;
import java.util.List;

/**
 * 在地图上搜索 NPC 出生点的客户端包。
 * Client packet for searching an NPC spawn location on the map.
 */
public class CM_OBJECT_SEARCH extends AionClientPacket {

	private static final int QUEST_CHAINING_MEMORIES = 14047;
	private static final int QUEST_ACESTES = 802051;
	private static final int LEGACY_ACESTES = 204652;
	private static final int FIRST_ACESTES_STAGE = 3;
	private static final int REPORT_ACESTES_STAGE = 6;

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
		int searchNpcId = gm ? resolveQuestSearchNpcId(player, npcId) : npcId;
		SearchTarget target = findSearchTarget(searchNpcId, preferredWorldId, gm);
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
		sendPacket(new SM_SHOW_NPC_ON_MAP(searchNpcId, searchResult.getWorldId(), searchResult.getSpot().getX(),
				searchResult.getSpot().getY(), searchResult.getSpot().getZ()));
	}

	/**
	 * 将 14047 的旧客户端 NPC 别名解析为实例中的任务 NPC。
	 * Resolves the legacy client NPC alias for quest 14047 to the instance quest NPC.
	 *
	 * <p>204652 与任务专用的 802051 使用相同的“阿凯斯泰斯”名称。客户端寻找请求只携带
	 * NPC ID，因此 GM 在 14047 的阿凯斯泰斯阶段会错误地命中普通模板；只有任务状态明确
	 * 指向 802051 时才做别名解析，避免影响其他任务使用 204652。</p>
	 * <p>NPC 204652 shares the Acestes name with quest-only NPC 802051. Because the client search request
	 * carries only an NPC ID, a GM at the Acestes stages of quest 14047 otherwise reaches the ordinary
	 * template. The alias is applied only when the quest state explicitly targets 802051, preserving other
	 * quests that use 204652.</p>
	 *
	 * @param player 搜索玩家 / searching player
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	private static int resolveQuestSearchNpcId(Player player, int requestedNpcId) {
		if (player == null || requestedNpcId != LEGACY_ACESTES) {
			return requestedNpcId;
		}
		QuestState state = player.getQuestStateList().getQuestState(QUEST_CHAINING_MEMORIES);
		return resolveQuestSearchNpcId(requestedNpcId, state);
	}

	/**
	 * 按给定任务状态解析搜索目标；空状态和不匹配状态保持原 ID。
	 * Resolves a search target from the supplied quest state; null or non-matching states preserve the original ID.
	 *
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @param state 14047 的当前任务状态，可为空 / current quest 14047 state, nullable
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	static int resolveQuestSearchNpcId(int requestedNpcId, QuestState state) {
		if (requestedNpcId != LEGACY_ACESTES || state == null
				|| state.getQuestId() != QUEST_CHAINING_MEMORIES || state.getStatus() != QuestStatus.START) {
			return requestedNpcId;
		}
		int stage = state.getQuestVarById(0);
		return stage == FIRST_ACESTES_STAGE || stage == REPORT_ACESTES_STAGE
				? QUEST_ACESTES : requestedNpcId;
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
