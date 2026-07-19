package com.aionemu.gameserver.services.instance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.dao.DynamicInstancesDAO;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.DynamicInstanceMember;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapInstanceFactory;

public final class DynamicInstanceManager {
	private static final Map<Long, WorldMapInstance> instances = new ConcurrentHashMap<>();
	private static volatile boolean restored;

	private DynamicInstanceManager() {
	}

	public static synchronized DynamicInstance attachNew(WorldMapInstance instance, int creationId, byte ownerType,
			int ownerId, byte difficulty) {
		Row definition = definition(instance.getMapId(), creationId);
		long now = System.currentTimeMillis();
		InstanceRuntimeState state = new InstanceRuntimeState();
		DynamicInstance dynamic = new DynamicInstance(0, instance.getMapId(), creationId,
				InstanceLimitService.clientCooldownId(instance.getMapId()), instance.getInstanceId(), ownerType, ownerId,
				difficulty, DynamicInstance.ACTIVE, (byte) (definition == null ? 0 : definition.intValue("spawn_page", 0)), now, 0, 0, 0, 1,
				state.encode(), now);
		long uid = dao().create(dynamic);
		dynamic.setInstanceUid(uid);
		attach(instance, dynamic, state);
		return dynamic;
	}

	public static synchronized void restore() {
		if (restored) {
			return;
		}
		DynamicInstancesDAO dao = dao();
		for (Map.Entry<Integer, Integer> entry : dao.loadMaxRuntimeInstanceIds().entrySet()) {
			WorldMap map = GameWorldBootstrapServices.world().getWorldMap(entry.getKey());
			if (map != null) {
				map.reserveInstanceId(entry.getValue());
			}
		}
		long now = System.currentTimeMillis();
		for (DynamicInstance dynamic : dao.loadRecoverable(now)) {
			restore(dynamic, dao.loadMembers(dynamic.getInstanceUid()), now);
		}
		restored = true;
	}

	public static WorldMapInstance findReentryInstance(int playerId, int worldId) {
		Long uid = dao().findReentryInstanceUid(playerId, worldId, System.currentTimeMillis());
		return uid == null ? null : instances.get(uid);
	}

	public static WorldMapInstance find(long instanceUid) {
		return instances.get(instanceUid);
	}

	public static void reserveMember(WorldMapInstance instance, Player player, int teamId, byte side) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null) {
			return;
		}
		dao().saveMember(new DynamicInstanceMember(dynamic.getInstanceUid(), player.getObjectId(), teamId, side, true,
				0, 0, 0, player.getWorldId(), "", (byte) 0));
		instance.register(player.getObjectId());
	}

	public static void markEntered(WorldMapInstance instance, Player player) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null) {
			return;
		}
		instance.register(player.getObjectId());
		dao().markMemberJoined(dynamic.getInstanceUid(), player.getObjectId(), System.currentTimeMillis());
		markActive(instance);
	}

	public static boolean hasJoined(WorldMapInstance instance, int playerId) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		return dynamic != null && dao().hasJoined(dynamic.getInstanceUid(), playerId);
	}

	public static int memberCount(WorldMapInstance instance) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		return dynamic == null ? instance.playersCount() : dao().countMembers(dynamic.getInstanceUid());
	}

	public static void removeReservedMember(WorldMapInstance instance, int playerId) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic != null) {
			dao().removeReservedMember(dynamic.getInstanceUid(), playerId);
			instance.unregister(playerId);
		}
	}

	public static void revokeMember(WorldMapInstance instance, int playerId) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic != null) {
			dao().revokeMember(dynamic.getInstanceUid(), playerId);
			instance.unregister(playerId);
		}
	}

	public static void markLeft(WorldMapInstance instance, Player player, long reentryMillis) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null) {
			return;
		}
		long now = System.currentTimeMillis();
		long reentryUntil = dynamic.getDestroyAt() > now ? dynamic.getDestroyAt() : now + reentryMillis;
		dao().markMemberLeft(dynamic.getInstanceUid(), player.getObjectId(), now, reentryUntil);
	}

	public static void markActive(WorldMapInstance instance) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null || dynamic.getStatus() == DynamicInstance.DESTROYED) {
			return;
		}
		dynamic.setStatus(DynamicInstance.ACTIVE);
		dynamic.setEmptyUntil(0);
		dynamic.setDestroyAt(0);
		update(dynamic);
	}

	public static long markEmpty(WorldMapInstance instance, long delayMillis) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		long now = System.currentTimeMillis();
		if (dynamic == null) {
			return now + delayMillis;
		}
		long deadline = dynamic.getStatus() == DynamicInstance.EMPTY && dynamic.getDestroyAt() > now
				? dynamic.getDestroyAt() : now + delayMillis;
		dynamic.setStatus(DynamicInstance.EMPTY);
		dynamic.setEmptyUntil(deadline);
		dynamic.setDestroyAt(deadline);
		update(dynamic);
		return deadline;
	}

	public static void archive(WorldMapInstance instance) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null) {
			return;
		}
		dynamic.setStatus(DynamicInstance.DESTROYED);
		dynamic.setEmptyUntil(0);
		dynamic.setDestroyAt(System.currentTimeMillis());
		update(dynamic);
		instances.remove(dynamic.getInstanceUid());
	}

	public static void persistState(WorldMapInstance instance) {
		DynamicInstance dynamic = instance.getDynamicInstance();
		if (dynamic == null) {
			return;
		}
		dynamic.setStateJson(instance.getRuntimeState().encode());
		update(dynamic);
	}

	public static int defaultCreationId(int worldId, boolean personal) {
		List<Row> definitions = DataManager.RETAIL_INSTANCE_DATA.definitionsForWorld(worldId);
		if (definitions.isEmpty()) {
			Row coverage = DataManager.RETAIL_INSTANCE_DATA.coverage(worldId);
			if (coverage != null && "special".equals(coverage.value("classification"))) {
				return 0;
			}
			throw new IllegalStateException("Missing retail instance creation for world " + worldId);
		}
		return definitions.stream()
				.filter(row -> !personal || row.value("type").contains("PRIVATE"))
				.findFirst().orElse(definitions.getFirst()).requiredInt("id");
	}

	public static int selectCreationId(int worldId, Player player, boolean personal) {
		List<Row> definitions = DataManager.RETAIL_INSTANCE_DATA.definitionsForWorld(worldId);
		if (definitions.isEmpty()) {
			return defaultCreationId(worldId, personal);
		}
		List<Row> candidates = definitions.stream()
				.filter(row -> !personal || row.value("type").contains("PRIVATE"))
				.filter(row -> personal || !row.value("type").contains("PRIVATE"))
				.toList();
		if (candidates.isEmpty()) {
			candidates = definitions;
		}
		String suffix = player.getRace().getRaceId() == 0 ? "_L" : "_D";
		Row raceDefinition = candidates.stream().filter(row -> row.value("name").endsWith(suffix)).findFirst().orElse(null);
		if (raceDefinition != null) {
			return raceDefinition.requiredInt("id");
		}
		return candidates.stream()
				.filter(row -> !row.value("name").endsWith("_SP"))
				.filter(row -> !row.value("type").contains("MATCH"))
				.findFirst().orElse(candidates.getFirst()).requiredInt("id");
	}

	private static void restore(DynamicInstance dynamic, List<DynamicInstanceMember> members, long now) {
		Row definition = definition(dynamic.getWorldId(), dynamic.getCreationId());
		if ((definition == null ? 0 : definition.intValue("spawn_page", 0)) != dynamic.getSpawnPage()) {
			throw new IllegalStateException("Dynamic instance spawn page drift for uid " + dynamic.getInstanceUid());
		}
		WorldMap map = GameWorldBootstrapServices.world().getWorldMap(dynamic.getWorldId());
		if (map == null || !map.isInstanceType()) {
			throw new IllegalStateException("Cannot restore unknown instance world " + dynamic.getWorldId());
		}
		if (map.getAvailableInstanceIds().contains(dynamic.getRuntimeInstanceId())) {
			throw new IllegalStateException("Runtime instance id collision " + dynamic.getWorldId() + ":"
					+ dynamic.getRuntimeInstanceId());
		}
		int personalOwner = dynamic.getOwnerType() == DynamicInstance.OWNER_PLAYER ? dynamic.getOwnerId() : 0;
		WorldMapInstance instance = WorldMapInstanceFactory.createWorldMapInstance(map, dynamic.getRuntimeInstanceId(), personalOwner);
		attach(instance, dynamic, InstanceRuntimeState.decode(dynamic.getStateJson()));
		map.addInstance(dynamic.getRuntimeInstanceId(), instance);
		map.reserveInstanceId(dynamic.getRuntimeInstanceId());
		try {
			SpawnEngine.spawnInstance(dynamic.getWorldId(), dynamic.getRuntimeInstanceId(), dynamic.getSpawnPage(), personalOwner);
			for (DynamicInstanceMember member : members) {
				if (member.isPermitted() && (member.getLeftAt() == 0 || member.getReentryUntil() == 0
						|| member.getReentryUntil() >= now)) {
					instance.register(member.getPlayerId());
				}
			}
			GameEngineServices.instanceEngine().onInstanceCreate(instance);
			if (com.aionemu.gameserver.lifecycle.GameCoreGameplayServices.autoGroupService()
					instanceof com.aionemu.gameserver.services.RetailMatchmakingService matchmaking) {
				matchmaking.attachInstance(instance);
			}
			InstanceService.restoreDestroyTask(instance);
		} catch (RuntimeException | Error e) {
			map.removeWorldMapInstance(dynamic.getRuntimeInstanceId());
			instances.remove(dynamic.getInstanceUid());
			throw e;
		}
	}

	private static void attach(WorldMapInstance instance, DynamicInstance dynamic, InstanceRuntimeState state) {
		instance.setDynamicInstance(dynamic, state);
		state.onChange(() -> persistState(instance));
		instances.put(dynamic.getInstanceUid(), instance);
	}

	private static Row definition(int worldId, int creationId) {
		if (creationId == 0) {
			Row coverage = DataManager.RETAIL_INSTANCE_DATA.coverage(worldId);
			if (coverage != null && "special".equals(coverage.value("classification"))) {
				return null;
			}
		}
		Row definition = DataManager.RETAIL_INSTANCE_DATA.definition(creationId);
		if (definition == null || definition.requiredInt("world_id") != worldId) {
			throw new IllegalStateException("Invalid retail instance creation " + creationId + " for world " + worldId);
		}
		return definition;
	}

	private static void update(DynamicInstance dynamic) {
		dynamic.setUpdatedAt(System.currentTimeMillis());
		dao().update(dynamic);
	}

	private static DynamicInstancesDAO dao() {
		return DAOManager.getDAO(DynamicInstancesDAO.class);
	}
}
