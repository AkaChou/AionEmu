package com.aionemu.gameserver.model.templates.spawns;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.event.EventTemplate;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;
import lombok.Getter;
import lombok.Setter;

/**
 * 刷新点模板（静态数据/XML）。
 * XML template.
 */

public class SpawnTemplate {
	/** 返回 x 坐标 / Returns the x */
	@Getter
	private float x;
	/** 返回 y 坐标 / Returns the y */
	@Getter
	private float y;
	/** 返回 z 坐标 / Returns the z */
	@Getter
	private float z;
	private final byte h;
	/** 返回实体 ID / Returns the entity id */
	@Getter
	@Setter
	private int entityId;
	@Getter
	private boolean resolveZ;
	@Getter
	@Setter
	private volatile float resolvedZ = Float.NaN;
	/** 返回随机行走 / Returns the random walk */
	@Getter
	@Setter
	private int randomWalk;
	/** 返回巡游者 ID / Returns the walker id */
	@Getter
	@Setter
	private String walkerId;
	private int walkerIdx;
	/** 获取飞行。 / Returns the fly. */
	@Getter
	@Setter
	private int fly;
	/** 返回锚点 / Returns the anchor */
	@Getter
	private String anchor;
	/** 是否已用 / Whether used*/
	@Getter
	private boolean isUsed;
	private final SpawnGroup2 spawnGroup;
	/** 获取活动模板。 / Returns the event template. */
	@Getter
	@Setter
	private EventTemplate eventTemplate;
	/** 返回模型 / Returns the model */
	@Getter
	private SpawnModel model;
	/** 获取状态。 / Returns the state. */
	@Getter
	private int state;
	private int astate;
	private int bstate;
	private int cstate;
	private int dstate;
	private int estate;
	private int istate;
	private int mstate;
	private int nstate;
	private int ostate;
	private int pstate;
	private int rstate;
	private int tstate;
	private int zstate;
	private int iustate;
	private int opstate;
	/** 返回 creator id / Returns the creator id */
	@Getter
	@Setter
	private int creatorId;
	private String alternateIdValues;
	private String selectprobValues;
	/** 返回大师名称 / Returns the master name */
	@Getter
	@Setter
	private String masterName = StringUtils.EMPTY;
	@Getter
	@Setter
	private String npcPartyId;
	@Getter
	@Setter
	private Creature master;
	private TemporarySpawn temporarySpawn;
	/** 返回可见对象。 / Returns the visible object. */
	@Getter
	@Setter
	private VisibleObject visibleObject;
	/** 返回 visible objects / Returns the visible objects */
	@Getter
	private List<VisibleObject> visibleObjects;

	public SpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		this.spawnGroup = spawnGroup;
		x = spot.getX();
		y = spot.getY();
		z = spot.getZ();
		h = spot.getHeading();
		entityId = spot.getEntityId();
		resolveZ = spot.isResolveZ();
		randomWalk = spot.getRandomWalk();
		walkerId = spot.getWalkerId();
		fly = spot.getFly();
		anchor = spot.getAnchor();
		walkerIdx = spot.getWalkerIndex();
		model = spot.getModel();
		state = spot.getState();
		astate = spot.getAState();
		bstate = spot.getBState();
		cstate = spot.getCState();
		dstate = spot.getDState();
		estate = spot.getEState();
		istate = spot.getIState();
		mstate = spot.getMState();
		nstate = spot.getNState();
		ostate = spot.getOState();
		pstate = spot.getPState();
		rstate = spot.getRState();
		tstate = spot.getTState();
		zstate = spot.getZState();
		iustate = spot.getIUState();
		opstate = spot.getOPState();
		alternateIdValues = spot.getAlternateIds();
		selectprobValues = spot.getSelectProbs();
		temporarySpawn = spot.getTemporarySpawn();
	}

	public SpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk, String walkerId,
			int entityId, int fly) {
		this.spawnGroup = spawnGroup;
		this.x = x;
		this.y = y;
		this.z = z;
		h = heading;
		this.randomWalk = randWalk;
		this.walkerId = walkerId;
		this.entityId = entityId;
		this.fly = fly;
		addTemplate();
	}

	private void addTemplate() {
		spawnGroup.addSpawnTemplate(this);
	}

	/** 设置 x 坐标 / Sets the x */
	public void setX(float x) {
		this.x = x;
		resolvedZ = Float.NaN;
	}

	/** 设置 y 坐标 / Sets the y */
	public void setY(float y) {
		this.y = y;
		resolvedZ = Float.NaN;
	}

	/** 设置 z 坐标 / Sets the z */
	public void setZ(float z) {
		this.z = z;
		resolvedZ = Float.NaN;
	}

	/** 返回 heading / Returns the heading */
	public byte getHeading() {
		return h;
	}

	public void setResolveZ(boolean resolveZ) {
		this.resolveZ = resolveZ;
		resolvedZ = Float.NaN;
	}

	/** 返回解析后的实际出生高度，尚未解析时回退到配置高度。 / Returns the resolved spawn height, or the configured height before resolution. */
	public float getEffectiveZ() {
		return Float.isFinite(resolvedZ) ? resolvedZ : z;
	}

	/** 返回静态 ID / Returns the static id */
	public int getStaticId() {
		return entityId;
	}

	/** 设置 static id / Sets the static id */
	public void setStaticId(int staticId) {
		this.entityId = staticId;
	}

	/** 是否可以飞行。 / Whether fly. */
	public boolean canFly() {
		return fly > 0;
	}

	/** 设置使用 / Sets the use*/
	public void setUse(boolean use) {
		isUsed = use;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return spawnGroup.getNpcId();
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return spawnGroup.getWorldId();
	}

	/** 更换模板 / Change Template*/
	public SpawnTemplate changeTemplate(int instanceId) {
		return spawnGroup.getRndTemplate(instanceId);
	}

	/** 返回重生时间 / Returns the respawn time */
	public int getRespawnTime() {
		return spawnGroup.getRespawnTime();
	}

	/** 设置重生时间 / Sets the respawn time */
	public void setRespawnTime(int respawnTime) {
		spawnGroup.setRespawnTime(respawnTime);
	}

	/** 返回临时刷新 / Returns the temporary spawn*/
	public TemporarySpawn getTemporarySpawn() {
		return temporarySpawn != null ? temporarySpawn : spawnGroup.geTemporarySpawn();
	}

	/** 获取处理器类型。 / Returns the handler type. */
	public SpawnHandlerType getHandlerType() {
		return spawnGroup.getHandlerType();
	}

	/**
	 * @return Whether random walk
	 */
	public boolean hasRandomWalk() {
		return randomWalk != 0;
	}

	/**
	 * @return Whether no respawn
	 */
	public boolean isNoRespawn() {
		return spawnGroup.getRespawnTime() == 0;
	}

	/**
	 * @return 是否使用对象池。 / Whether pool
	  */
	public boolean hasPool() {
		return spawnGroup.hasPool();
	}

	/** 返回巡游者索引 / Returns the walker index */
	public int getWalkerIndex() {
		return walkerIdx;
	}

	/**
	 * @return Whether temporary spawn
	 */
	public boolean isTemporarySpawn() {
		return spawnGroup.isTemporarySpawn();
	}

	/** 是否为活动刷新点。 / Whether event spawn. */
	public boolean isEventSpawn() {
		return eventTemplate != null;
	}

	/** 返回状态 / Returns the a state */
	public int getAState() {
		return astate;
	}

	/** 返回 b 状态 / Returns the b state */
	public int getBState() {
		return bstate;
	}

	/** 返回 c 状态 / Returns the c state */
	public int getCState() {
		return cstate;
	}

	/** 返回 d 状态 / Returns the d state */
	public int getDState() {
		return dstate;
	}

	/** 返回 e 状态 / Returns the e state */
	public int getEState() {
		return estate;
	}

	/** 返回 i 状态 / Returns the i state */
	public int getIState() {
		return istate;
	}

	/** 返回 m 状态 / Returns the m state */
	public int getMState() {
		return mstate;
	}

	/** 返回 n 状态 / Returns the n state */
	public int getNState() {
		return nstate;
	}

	/** 返回 o 状态 / Returns the o state */
	public int getOState() {
		return ostate;
	}

	/** 返回 p 状态 / Returns the p state */
	public int getPState() {
		return pstate;
	}

	/** 返回 r 状态 / Returns the r state */
	public int getRState() {
		return rstate;
	}

	/** 返回 t 状态 / Returns the t state */
	public int getTState() {
		return tstate;
	}

	/** 返回 z 状态 / Returns the z state */
	public int getZState() {
		return zstate;
	}

	/** 返回 IU 状态 / Returns the iu state */
	public int getIUState() {
		return iustate;
	}

	/** 返回 OP 状态 / Returns the op state */
	public int getOPState() {
		return opstate;
	}

	/** 返回备用 ID / Returns the alternate ids */
	public int[] getAlternateIds() {
		int[] alternateIds;
		if (alternateIdValues!=null){
			String[] values = alternateIdValues.split(",");
			alternateIds = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				alternateIds[i] = Integer.parseInt(values[i]);
			}
		}
		else
		{
			alternateIds = new int[1];
		}
		return alternateIds;
	}

	/** 返回选择概率 / Returns the select probs */
	public int[] getSelectProbs() {
		int[] selectProbs;
		if (selectprobValues!=null){
			String[] values = selectprobValues.split(",");
			selectProbs = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				selectProbs[i] = Integer.parseInt(values[i]);
			}
		}
		else
		{
			selectProbs = new int[1];
		}
		return selectProbs;
	}

	/** 该模板在指定实例中是否有已刷出的对象。 / Whether this template has an object spawned in the given instance. */
	public boolean isInWorld(int instanceId) {
		if (visibleObject != null && visibleObject.isSpawned() && visibleObject.getInstanceId() == instanceId) {
			return true;
		}
		List<VisibleObject> objects = visibleObjects;
		if (objects != null) {
			for (VisibleObject object : objects) {
				if (object.isSpawned() && object.getInstanceId() == instanceId) {
					return true;
				}
			}
		}
		return false;
	}

	/** 添加可见对象。 / Adds visible object. */
	public void addVisibleObject(VisibleObject visibleObject) {
		if (this.visibleObjects == null) {
			this.visibleObjects = new ArrayList<>();
		}
		this.visibleObjects.add(visibleObject);
	}
}
