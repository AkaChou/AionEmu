package com.aionemu.gameserver.instance.handlers;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.StageType;
import com.aionemu.gameserver.model.instance.InstanceObjectRegistry;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.instance.InstanceDeadlineScheduler;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 通用副本处理器基类：提供空默认实现与常用刷怪/消息工具方法。
 * General instance-handler base: no-op defaults plus common spawn/message helpers.
 *
 * @author ATracer
 */
public class GeneralInstanceHandler implements InstanceHandler {

	/** 创建时间戳（毫秒） / creation timestamp (ms) */
	protected final long creationTime;
	/** 绑定的世界地图实例 / bound world-map instance */
	protected WorldMapInstance instance;
	/** 副本实例 ID / instance id */
	protected int instanceId;
	/** 地图 ID / map id */
	protected Integer mapId;

	/**
	 * 构造处理器并记录创建时间。
	 * Construct the handler and record creation time.
	 */
	public GeneralInstanceHandler() {
		creationTime = System.currentTimeMillis();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		this.instance = instance;
		this.instanceId = instance.getInstanceId();
		this.mapId = instance.getMapId();
		restoreDoors();
	}

	protected final InstanceRuntimeState runtimeState() {
		return instance.getRuntimeState();
	}

	protected final void scheduleDeadline(String key, long deadline, Runnable action) {
		InstanceDeadlineScheduler.schedule(instance, getClass().getName() + '.' + key, deadline, action);
	}

	protected final void cancelDeadline(String key) {
		InstanceDeadlineScheduler.cancel(instance, getClass().getName() + '.' + key);
	}

	protected final void setDoorState(int entityId, boolean open) {
		StaticDoor door = instance.getDoors().get(entityId);
		if (door == null) {
			throw new IllegalStateException("Missing instance door entity " + entityId + " in world " + mapId);
		}
		runtimeState().put("door." + entityId, open);
		door.setOpen(open);
	}

	protected final VisibleObject resolveObject(String stableKey) {
		return InstanceObjectRegistry.resolve(instance, stableKey);
	}

	protected final void bindObject(String businessKey, VisibleObject object) {
		InstanceObjectRegistry.bind(instance, businessKey, object);
	}

	private void restoreDoors() {
		for (var entry : runtimeState().snapshot("door.").entrySet()) {
			int entityId = Integer.parseInt(entry.getKey().substring("door.".length()));
			StaticDoor door = instance.getDoors().get(entityId);
			if (door == null) {
				throw new IllegalStateException("Cannot restore instance door entity " + entityId + " in world " + mapId);
			}
			door.setOpen(Boolean.parseBoolean(entry.getValue()));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onInstanceDestroy() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPlayerLogin(Player player) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPlayerLogOut(Player player) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onEnterInstance(Player player) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLeaveInstance(Player player) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onOpenDoor(Player player, int door) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onEnterZone(Player player, ZoneInstance zone) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLeaveZone(Player player, ZoneInstance zone) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPlayMovieEnd(Player player, int movieId) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onReviveEvent(Player player) {
		return false;
	}

	/**
	 * 在当前副本刷出一次性 NPC。
	 * Spawn a one-shot NPC in the current instance.
	 *
	 * NPC 模板 ID / NPC template id
	 * @param x X 坐标 / X coordinate
	 * @param y Y 坐标 / Y coordinate
	 * @param z Z 坐标 / Z coordinate
	 * 朝向 / heading
	 * @return 生成的可见对象 / spawned visible object
	 */
	protected VisibleObject spawn(int npcId, float x, float y, float z, byte heading) {
		SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(mapId, npcId, x, y, z, heading);
		return SpawnEngine.spawnObject(template, instanceId);
	}

	protected VisibleObject spawnPartyMember(String partyId, int npcId, float x, float y, float z, byte heading) {
		if (partyId == null || partyId.isBlank()) {
			throw new IllegalArgumentException("NPC party id must not be blank");
		}
		SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(mapId, npcId, x, y, z, heading);
		template.setNpcPartyId(partyId);
		return SpawnEngine.spawnObject(template, instanceId);
	}

	/**
	 * 在当前副本刷出绑定实体 ID 的一次性 NPC。
	 * Spawn a one-shot NPC bound to an entity id in the current instance.
	 *
	 * NPC 模板 ID / NPC template id
	 * @param x X 坐标 / X coordinate
	 * @param y Y 坐标 / Y coordinate
	 * @param z Z 坐标 / Z coordinate
	 * 朝向 / heading
	 * entity id
	 * @return 生成的可见对象 / spawned visible object
	 */
	protected VisibleObject spawn(int npcId, float x, float y, float z, byte heading, int entityId) {
		SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(mapId, npcId, x, y, z, heading);
		template.setEntityId(entityId);
		return SpawnEngine.spawnObject(template, instanceId);
	}

	/**
	 * 按 NPC 模板 ID 从当前实例查找 NPC。
	 * Look up an NPC by template id in the current instance.
	 *
	 * NPC 模板 ID / NPC template id
	 *
	 * @param npcId
	 * @return NPC；不存在则为 {@code null} / NPC, or {@code null}
	 */
	protected Npc getNpc(final int npcId) {
		return instance.getNpc(npcId);
	}

	/**
	 * 向副本内发送喊话/系统消息（立即）。
	 * Send a shout/system message inside the instance (immediately).
	 *
	 * message id
	 * @param Obj 关联对象 ID / related object id
	 * whether shout
	 * color
	 */
	protected void sendMsg(int msg, int Obj, boolean isShout, int color) {
		sendMsg(msg, Obj, isShout, color, 0);
	}

	/**
	 * 向副本内发送喊话/系统消息（可延迟）。
	 * Send a shout/system message inside the instance (optionally delayed).
	 *
	 * message id
	 * @param Obj 关联对象 ID / related object id
	 * whether shout
	 * color
	 * @param time 延迟毫秒 / delay in ms
	 */
	protected void sendMsg(int msg, int Obj, boolean isShout, int color, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(instance, msg, Obj, isShout, color, time);
	}

	/**
	 * 向副本内发送默认样式系统消息。
	 * Send a default-style system message inside the instance.
	 *
	 * message id
	 */
	protected void sendMsg(int msg) {
		sendMsg(msg, 0, false, 25);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onExitInstance(Player player) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doReward(Player player) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStopTraining(Player player) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDie(Npc npc) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onChangeStage(StageType type) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StageType getStage() {
		return StageType.DEFAULT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDropRegistered(Npc npc) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onGather(Player player, Gatherable gatherable) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPassFlyingRing(Player player, String flyingRing) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
	}

	@Override
	public boolean supportsRetailNpcScore(int npcId, int scoreApplyType) {
		return false;
	}

	@Override
	public boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int points) {
		return false;
	}
}
