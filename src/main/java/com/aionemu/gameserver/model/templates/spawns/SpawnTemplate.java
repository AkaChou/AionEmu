package com.aionemu.gameserver.model.templates.spawns;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.event.EventTemplate;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;

/**
 * 刷新点模板（静态数据/XML）。
 * XML template.
 */

public class SpawnTemplate {
	private float x;
	private float y;
	private float z;
	private byte h;
	private int entityId;
	private int randomWalk;
	private String walkerId;
	private int walkerIdx;
	private int fly;
	private String anchor;
	private boolean isUsed;
	private SpawnGroup2 spawnGroup;
	private EventTemplate eventTemplate;
	private SpawnModel model;
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
	private int creatorId;
	private String alternateIdValues;
	private String selectprobValues;
	private String masterName = StringUtils.EMPTY;
	private TemporarySpawn temporarySpawn;
	private VisibleObject visibleObject;
	private List<VisibleObject> visibleObjects;

	public SpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		this.spawnGroup = spawnGroup;
		x = spot.getX();
		y = spot.getY();
		z = spot.getZ();
		h = spot.getHeading();
		entityId = spot.getEntityId();
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

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 设置 x / Sets the x */
	public void setX(float x) {
		this.x = x;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 设置 y / Sets the y */
	public void setY(float y) {
		this.y = y;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 设置 z / Sets the z */
	public void setZ(float z) {
		this.z = z;
	}

	/** 返回 heading / Returns the heading */
	public byte getHeading() {
		return h;
	}

	/** 返回 entity id / Returns the entity id */
	public int getEntityId() {
		return entityId;
	}

	/** 设置 entity id / Sets the entity id */
	public void setEntityId(int entityId) {
		this.entityId = entityId;
	}

	/** 返回静态 ID / Returns the static id */
	public int getStaticId() {
		return entityId;
	}

	/** 设置 static id / Sets the static id */
	public void setStaticId(int staticId) {
		this.entityId = staticId;
	}

	/** 返回 random walk / Returns the random walk */
	public int getRandomWalk() {
		return randomWalk;
	}

	/** 设置 random walk / Sets the random walk */
	public void setRandomWalk(int randomWalk) {
		this.randomWalk = randomWalk;
	}

	/** 获取飞行。 / Returns the fly. */
	public int getFly() {
		return fly;
	}

	/** 是否可以飞行。 / Whether fly. */
	public boolean canFly() {
		return fly > 0;
	}

	/** 设置使用 / Sets the use*/
	public void setUse(boolean use) {
		isUsed = use;
	}

	/** 是否已用 / Whether used*/
	public boolean isUsed() {
		return isUsed;
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

	/** 返回 respawn time / Returns the respawn time */
	public int getRespawnTime() {
		return spawnGroup.getRespawnTime();
	}

	/** 设置 respawn time / Sets the respawn time */
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

	/** 返回 anchor / Returns the anchor */
	public String getAnchor() {
		return anchor;
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

	/** 返回 walker id / Returns the walker id */
	public String getWalkerId() {
		return walkerId;
	}

	/** 设置 walker id / Sets the walker id */
	public void setWalkerId(String walkerId) {
		this.walkerId = walkerId;
	}

	/** 返回 walker index / Returns the walker index */
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

	/** 获取活动模板。 / Returns the event template. */
	public EventTemplate getEventTemplate() {
		return eventTemplate;
	}

	/** 设置活动模板。 / Sets the event template. */
	public void setEventTemplate(EventTemplate eventTemplate) {
		this.eventTemplate = eventTemplate;
	}

	/** 返回 model / Returns the model */
	public SpawnModel getModel() {
		return model;
	}

	/** 获取状态。 / Returns the state. */
	public int getState() {
		return state;
	}

	/** 返回状态 / Returns the a state */
	public int getAState() {
		return astate;
	}

	/** 返回 b state / Returns the b state */
	public int getBState() {
		return bstate;
	}

	/** 返回 c state / Returns the c state */
	public int getCState() {
		return cstate;
	}

	/** 返回 d state / Returns the d state */
	public int getDState() {
		return dstate;
	}

	/** 返回 e state / Returns the e state */
	public int getEState() {
		return estate;
	}

	/** 返回 i state / Returns the i state */
	public int getIState() {
		return istate;
	}

	/** 返回 m state / Returns the m state */
	public int getMState() {
		return mstate;
	}

	/** 返回 n state / Returns the n state */
	public int getNState() {
		return nstate;
	}

	/** 返回 o state / Returns the o state */
	public int getOState() {
		return ostate;
	}

	/** 返回 p state / Returns the p state */
	public int getPState() {
		return pstate;
	}

	/** 返回 r state / Returns the r state */
	public int getRState() {
		return rstate;
	}

	/** 返回 t state / Returns the t state */
	public int getTState() {
		return tstate;
	}

	/** 返回 z state / Returns the z state */
	public int getZState() {
		return zstate;
	}

	/** 返回 iu state / Returns the iu state */
	public int getIUState() {
		return iustate;
	}

	/** 返回 op state / Returns the op state */
	public int getOPState() {
		return opstate;
	}

	/** 返回 alternate ids / Returns the alternate ids */
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

	/** 返回 select probs / Returns the select probs */
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
	
	/** 返回 creator id / Returns the creator id */
	public int getCreatorId() {
		return creatorId;
	}

	/** 设置 creator id / Sets the creator id */
	public void setCreatorId(int creatorId) {
		this.creatorId = creatorId;
	}

	/** 返回大师名称 / Returns the master name */
	public String getMasterName() {
		return masterName;
	}

	/** 设置 master name / Sets the master name */
	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}

	/** 获取可见对象。 / Returns the visible object. */
	public VisibleObject getVisibleObject() {
		return visibleObject;
	}

	/** 设置可见对象。 / Sets the visible object. */
	public void setVisibleObject(VisibleObject visibleObject) {
		this.visibleObject = visibleObject;
	}

	/** 返回 visible objects / Returns the visible objects */
	public List<VisibleObject> getVisibleObjects() {
		return this.visibleObjects;
	}

	/** 添加可见对象。 / Adds visible object. */
	public void addVisibleObject(VisibleObject visibleObject) {
		if (this.visibleObjects == null) {
			this.visibleObjects = new ArrayList<>();
		}
		this.visibleObjects.add(visibleObject);
	}
}
