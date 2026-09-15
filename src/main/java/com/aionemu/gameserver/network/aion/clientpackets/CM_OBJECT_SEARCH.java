package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SHOW_NPC_ON_MAP;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.WorldType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

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
	private static final int QUEST_COVERT_COMMUNIQUES = 10520;
	private static final int CLIENT_ASTERA_NPC = 800327;
	private static final int MOVE_TO_ASTERA_STAGE = 4;
	private static final int ELYOS_SANCTUARY_SENSOR = 206485;
	private static final int ASMODIAN_SANCTUARY_SENSOR = 206486;
	private static final int QUEST_RISK_FOR_THE_OBELISK = 10031;
	private static final int CLIENT_DREDGION_EREMITIA = 221524;
	private static final int QUEST_EREMITIA = 798600;
	private static final int EREMITIA_SEARCH_STAGE = 1;
	private static final int QUEST_DRAWLING_BALAUR = 14043;
	private static final int QUEST_VENTUS = 278532;
	private static final int CLIENT_DF6_BANTUS = 241198;
	private static final int CLIENT_DF6_BANTUS_REPLACEMENT = 241418;
	private static final int VENTUS_INTRO_STAGE = 0;
	private static final int VENTUS_REWARD_STAGE = 6;
	private static final int VENTUS_EXTENDED_REWARD_STAGE = 8;

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
		int searchNpcId = resolveSearchNpcId(player, npcId);
		SearchTarget target = resolveQuestSensorTarget(searchNpcId);
		if (target == null) {
			target = findSearchTarget(player, searchNpcId, preferredWorldId, gm);
		}
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
		if (searchResult == null && target.npc() != null) {
			SpawnSpotTemplate spot = new SpawnSpotTemplate(target.npc().getX(), target.npc().getY(),
					target.npc().getZ(),
							(byte) 0, 0, null, null);
			searchResult = new SpawnSearchResult(target.npc().getWorldId(), spot);
		}
		if (searchResult == null) {
			return;
		}
		sendPacket(new SM_SHOW_NPC_ON_MAP(searchNpcId, searchResult.getWorldId(), searchResult.getSpot().getX(),
				searchResult.getSpot().getY(), searchResult.getSpot().getZ()));
	}

	/**
	 * 将客户端可能因同名冲突提交的错误 NPC ID 解析为实际目标。
	 * Resolves NPC IDs that may collide on the client to their authoritative targets.
	 *
	 * @param player 搜索玩家 / searching player
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	static int resolveSearchNpcId(Player player, int requestedNpcId) {
		int searchNpcId = resolveAsteraSearchNpcId(player, requestedNpcId);
		searchNpcId = resolveQuestSearchNpcId(player, searchNpcId);
		searchNpcId = resolveEremitiaSearchNpcId(player, searchNpcId);
		return resolveVentusSearchNpcId(player, searchNpcId);
	}

	/**
	 * 将 10520“移动到阿斯泰拉”阶段的客户端同名 NPC 请求解析为任务感知目标。
	 * Resolves the client NPC-name collision during quest 10520's "Move to Astera" stage to its quest sensor.
	 *
	 * <p>Aion 5.8 客户端会将该区域链接提交为 NPC 800327；该 ID 实际属于厄夏勒的魔族 NPC
	 * Astella。只有任务状态明确处于 var0=4 时才改写为 206485，避免影响其他任务对 800327
	 * 的合法搜索。</p>
	 * <p>The Aion 5.8 client submits this zone link as NPC 800327, which belongs to the Asmodian NPC Astella
	 * in Enshar. The alias is rewritten to 206485 only while the quest is explicitly at var0=4, preserving
	 * legitimate searches for NPC 800327 from other quests.</p>
	 *
	 * @param player 搜索玩家 / searching player
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	private static int resolveAsteraSearchNpcId(Player player, int requestedNpcId) {
		if (player == null || requestedNpcId != CLIENT_ASTERA_NPC) {
			return requestedNpcId;
		}
		QuestState state = player.getQuestStateList().getQuestState(QUEST_COVERT_COMMUNIQUES);
		return resolveAsteraSearchNpcId(requestedNpcId, state);
	}

	static int resolveAsteraSearchNpcId(int requestedNpcId, QuestState state) {
		if (requestedNpcId != CLIENT_ASTERA_NPC || state == null
				|| state.getQuestId() != QUEST_COVERT_COMMUNIQUES || state.getStatus() != QuestStatus.START
				|| state.getQuestVarById(0) != MOVE_TO_ASTERA_STAGE) {
			return requestedNpcId;
		}
		return ELYOS_SANCTUARY_SENSOR;
	}

	/**
	 * 将 14047 的旧客户端 NPC 别名解析为实例中的任务 NPC。
	 * Resolves the legacy client NPC alias for quest 14047 to the instance quest NPC.
	 *
	 * <p>204652 与任务专用的 802051 使用相同的“阿凯斯泰斯”名称。客户端寻找请求只携带
	 * NPC ID，
	 * 因此 GM 在 14047 的阿凯斯泰斯阶段会错误地命中普通模板；只有任务状态明确指向 802051
	 * 时才做别名解析，避免影响其他任务使用 204652。</p>
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

	/**
	 * 将 10031 第一阶段的客户端同名 NPC 请求解析为任务目标。
	 * Resolves the client NPC-name collision during quest 10031's first stage to the quest target.
	 *
	 * <p>副本守卫 221524 与任务 NPC 798600 都使用“艾尔米提亚”名称，客户端寻路请求只携带
	 * NPC ID。只有任务状态明确处于 var0=1 时才改写，避免影响其他任务和阶段。</p>
	 * <p>Instance guard 221524 and quest NPC 798600 share the Eremitia name, while the client search
	 * request carries only an NPC ID. The alias is applied only at quest var0=1, preserving all other
	 * quests and stages.</p>
	 *
	 * @param player 搜索玩家 / searching player
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	private static int resolveEremitiaSearchNpcId(Player player, int requestedNpcId) {
		if (player == null || requestedNpcId != CLIENT_DREDGION_EREMITIA) {
			return requestedNpcId;
		}
		QuestState state = player.getQuestStateList().getQuestState(QUEST_RISK_FOR_THE_OBELISK);
		return resolveEremitiaSearchNpcId(requestedNpcId, state);
	}

	/**
	 * 按给定任务状态解析艾尔米提亚搜索目标；空状态和不匹配状态保持原 ID。
	 * Resolves the Eremitia search target from the supplied quest state; null or non-matching states
	 * preserve the original ID.
	 *
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @param state 10031 的当前任务状态，可为空 / current quest 10031 state, nullable
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	static int resolveEremitiaSearchNpcId(int requestedNpcId, QuestState state) {
		if (requestedNpcId != CLIENT_DREDGION_EREMITIA || state == null
				|| state.getQuestId() != QUEST_RISK_FOR_THE_OBELISK || state.getStatus() != QuestStatus.START
				|| state.getQuestVarById(0) != EREMITIA_SEARCH_STAGE) {
			return requestedNpcId;
		}
		return QUEST_EREMITIA;
	}

	/**
	 * 将 14043“学习龙族语”中的班图斯同名搜索解析为任务 NPC。
	 * Resolves the Bantus name collision during quest 14043 "Drawling Balaur" to the quest NPC.
	 *
	 * <p>客户端在“和班图斯进行对话”阶段只提交按显示名解析出的 NPC ID，会把埃雷修兰塔的情报官
	 * 278532 解析成诺斯珀德 DF6 B2_24 的同名精英怪 241198；241418 是同一刷新点的昼夜替换怪，
	 * 现场寻找命中的正是它。只有任务状态明确处于寻找 278532 的阶段时才做别名解析，避免影响
	 * 其他任务和其他阶段。</p>
	 * <p>The client submits only the NPC ID it resolved from the shared display name during the
	 * "talk to Ventus" stages, which maps to DF6 B2_24 monster 241198 instead of Reshanta
	 * intelligence officer 278532; 241418 is the day/night replacement at the same spawn point and
	 * is the NPC the live search reached. The alias is applied only while the quest state explicitly
	 * targets 278532, preserving every other quest and stage.</p>
	 *
	 * @param player 搜索玩家 / searching player
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	private static int resolveVentusSearchNpcId(Player player, int requestedNpcId) {
		if (isDf6BantusSlotTemplate(requestedNpcId) && player != null) {
			QuestState state = player.getQuestStateList().getQuestState(QUEST_DRAWLING_BALAUR);
			return resolveVentusSearchNpcId(requestedNpcId, state);
		}
		return requestedNpcId;
	}

	/**
	 * 按给定任务状态解析班图斯搜索目标；空状态和不匹配状态保持原 ID。
	 * Resolves a Bantus search target from the supplied quest state; null or non-matching states preserve the original ID.
	 *
	 * @param requestedNpcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @param state 14043 的当前任务状态，可为空 / current quest 14043 state, nullable
	 * @return 实际用于搜索的 NPC ID / NPC ID used for the search
	 */
	static int resolveVentusSearchNpcId(int requestedNpcId, QuestState state) {
		if (!isDf6BantusSlotTemplate(requestedNpcId) || state == null
				|| state.getQuestId() != QUEST_DRAWLING_BALAUR) {
			return requestedNpcId;
		}
		int stage = state.getQuestVarById(0);
		boolean introStage = state.getStatus() == QuestStatus.START && stage == VENTUS_INTRO_STAGE;
		boolean rewardStage = state.getStatus() == QuestStatus.REWARD
				&& (stage == VENTUS_REWARD_STAGE || stage == VENTUS_EXTENDED_REWARD_STAGE);
		return introStage || rewardStage ? QUEST_VENTUS : requestedNpcId;
	}

	/**
	 * 判断模板是否属于 DF6 B2_24 的班图斯刷新位；昼夜两个模板共用一个刷新点。
	 * Checks whether a template belongs to the DF6 B2_24 Bantus slot; its day and night templates share one spawn point.
	 *
	 * @param npcId 客户端请求的 NPC ID / NPC ID requested by the client
	 * @return 是否属于该同名刷新位 / whether the template belongs to that shared slot
	 */
	private static boolean isDf6BantusSlotTemplate(int npcId) {
		return npcId == CLIENT_DF6_BANTUS || npcId == CLIENT_DF6_BANTUS_REPLACEMENT;
	}

	/**
	 * 将无静态刷新点的任务感知目标解析到其权威区域坐标。
	 * Resolves quest sensor targets without static spawns to their authoritative zone coordinates.
	 *
	 * @param requestedNpcId 客户端请求的感知 NPC ID / sensory NPC ID requested by the client
	 * @return 固定任务区域目标；不是已知感知目标时返回 null /
	 * fixed quest-zone target, or null when not recognized
	 */
	static SearchTarget resolveQuestSensorTarget(int requestedNpcId) {
		return switch (requestedNpcId) {
			case ELYOS_SANCTUARY_SENSOR -> fixedSearchTarget(210100000, 1456.6283f, 1299.3306f, 336.49023f);
			case ASMODIAN_SANCTUARY_SENSOR -> fixedSearchTarget(220110000, 1757.3667f, 2008.911f, 196.59653f);
			default -> null;
		};
	}

	private static SearchTarget fixedSearchTarget(int worldId, float x, float y, float z) {
		SpawnSpotTemplate spot = new SpawnSpotTemplate(x, y, z, (byte) 0, 0, null, null);
		return new SearchTarget(new SpawnSearchResult(worldId, spot), null);
	}

	private static SearchTarget findSearchTarget(Player player, int npcId, int preferredWorldId, boolean gm) {
		List<SpawnSearchResult> locations = DataManager.SPAWNS_DATA2
				.getSpawnLocationsByNpcId(preferredWorldId, npcId);
		if (player != null && !gm) {
			locations = filterLocationsByRace(locations, player.getRace());
		}
		boolean allowStaticFallback = gm || (player != null && !locations.isEmpty());
		return selectSearchTarget(npcId, locations, GameWorldBootstrapServices.world().getNpcs(),
				allowStaticFallback);
	}

	static List<SpawnSearchResult> filterLocationsByRace(List<SpawnSearchResult> locations, Race race) {
		if (race == null || locations == null || locations.isEmpty() || DataManager.WORLD_MAPS_DATA == null) {
			return locations;
		}
		return filterLocationsByRace(locations, race, worldId -> {
			WorldMapTemplate template = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
			return template != null ? template.getWorldType() : WorldType.NONE;
		});
	}

	static List<SpawnSearchResult> filterLocationsByRace(List<SpawnSearchResult> locations, Race race,
			IntFunction<WorldType> worldTypeProvider) {
		if (race == null || locations == null || locations.isEmpty() || worldTypeProvider == null) {
			return locations;
		}
		List<SpawnSearchResult> filtered = new ArrayList<>();
		for (SpawnSearchResult loc : locations) {
			WorldType type = worldTypeProvider.apply(loc.getWorldId());
			if (race == Race.ELYOS && type == WorldType.ASMODAE) {
				continue;
			}
			if (race == Race.ASMODIANS && type == WorldType.ELYSEA) {
				continue;
			}
			filtered.add(loc);
		}
		return filtered.isEmpty() ? locations : filtered;
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

	record SearchTarget(SpawnSearchResult location, Npc npc) {
	}
}
