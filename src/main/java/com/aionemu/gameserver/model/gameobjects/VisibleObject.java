package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldDropType;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.WorldType;
import com.aionemu.gameserver.world.knownlist.KnownList;

/**
 * 可见对象。
 * Visible Object game object.
 *
 * @author -Nemesiss-
 */
public abstract class VisibleObject extends AionObject {

	protected VisibleObjectTemplate objectTemplate;

	// 玩家可见物体的距离 / how far player will see visible object
	public static final float VisibilityDistance = 95;

	// 最大 Z 可见距离 / maxZvisibleDistance
	public static final float maxZvisibleDistance = 95;

	 /**
	  * 构造方法。
	  * Constructor.
	  */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public VisibleObject(int objId, VisibleObjectController<? extends VisibleObject> controller,
			SpawnTemplate spawnTemplate, VisibleObjectTemplate objectTemplate, WorldPosition position) {
		super(objId);
		this.controller = controller;
		((VisibleObjectController) controller).setOwner(this);
		this.position = position;
		this.spawn = spawnTemplate;
		this.objectTemplate = objectTemplate;
	}

	 /**
	  * 对象在世界中的位置。
	  * Position of object in the world
	  */
	protected WorldPosition position;

	 /**
	  * 此 VisibleObject 的 KnownList。
	  * KnownList of this VisibleObject
	  */
	private KnownList knownlist;

	 /**
	  * 此 VisibleObject 的控制器。
	  * Controller of this VisibleObject
	  */
	private final VisibleObjectController<? extends VisibleObject> controller;

	 /**
	  * 可见对象的目标。
	  * Visible object's target
	  */
	private VisibleObject target;

	/**
	 * Spawn template of this visibleObject.
	 */
	private SpawnTemplate spawn;

	/**
	 * @return 返回当前 WorldRegionAionObject 为在。 / Returns current WorldRegion AionObject is in
	 */
	public MapRegion getActiveRegion() {
		return position.getMapRegion();
	}

	/** 返回副本 ID / Returns the instance id */
	public int getInstanceId() {
		return position.getInstanceId();
	}

	/**
	 * @return 返回 World 映射 ID。 / Return World map id
	 */
	public int getWorldId() {
		return position.getMapId();
	}

	/**
	 * 返回 WorldType 的当前 location。 / Return the WorldType of the current location
	 */
	public WorldType getWorldType() {
		return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(getWorldId()).getWorldType();
	}

	/**
	 * @return 返回 World 位置 x。 / Return World position x
	 */
	public float getX() {
		return position.getX();
	}

	/**
	 * @return 返回 World 位置 y。 / Return World position y
	 */
	public float getY() {
		return position.getY();
	}

	/**
	 * @return 返回 World 位置 z。 / Return World position z
	 */
	public float getZ() {
		return position.getZ();
	}

	/** 设置 xyzh / Sets the xyzh */
	public void setXYZH(Float x, Float y, Float z, Byte h) {
		position.setXYZH(x, y, z, h);
	}

	/**
	 * @return 对象朝向，取值 [0,120)。 / Heading of the object. Values from <0,120)
	 */
	public byte getHeading() {
		return position.getHeading();
	}

	/**
	 * 返回对象位置。 / Return object position
	 *
	 * @return position.
	 */
	public WorldPosition getPosition() {
		return position;
	}

	/**
	 * 检查对象是否已生成。 / Check whether the object is spawned.
	 *
	 * @return true if object is spawned.
	 */
	public boolean isSpawned() {
		return position.isSpawned();
	}

	/**
	 * @return
	 */
	public boolean isInWorld() {
		return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(getObjectId()) != null;
	}

	/**
	 * 检查映射 is 实例。 / Check if map is instance
	 *
	 * @return true if object in one of the instance maps
	 */
	public boolean isInInstance() {
		return position.isInstanceMap();
	}

	/** 清除 knownlist / Clear knownlist */
	public void clearKnownlist() {
		getKnownList().clear();
	}

	/** 更新 knownlist / Update knownlist */
	public void updateKnownlist() {
		getKnownList().doUpdate();
	}

	/**
	 * @param creature 是否可见。 / Whether see
	  */
	public boolean canSee(Creature creature) {
		return creature != null;
	}

	/**
	 * 设置 knownlistvisibleobject。
	 * Set KnownList to this VisibleObject
	 *
	 * @param knownlist
	 */
	public void setKnownlist(KnownList knownlist) {
		this.knownlist = knownlist;
	}

	 /**
	  * 返回此 VisibleObject 的 KnownList。
	  * Returns KnownList of this VisibleObject
	  * @return knownList.
	  */
	public KnownList getKnownList() {
		return knownlist;
	}

	 /**
	  * 返回此 VisibleObject 的控制器。
	  * Return VisibleObjectController of this VisibleObject
	  * @return VisibleObjectController.
	  */
	public VisibleObjectController<? extends VisibleObject> getController() {
		return controller;
	}

	/**
	 * @return VisibleObject
	 */
	public final VisibleObject getTarget() {
		return target;
	}

	/**
	 * @return distance to target or 0 if no target
	 */
	public float getDistanceToTarget() {
		VisibleObject currTarget = target;
		if (currTarget == null) {
			return 0;
		}
		return (float) MathUtil.getDistance(getX(), getY(), getZ(), currTarget.getX(), currTarget.getY(),
				currTarget.getZ()) - this.getObjectTemplate().getBoundRadius().getCollision()
				- currTarget.getObjectTemplate().getBoundRadius().getCollision();
	}

	/**
	 * @param creature
	 */
	public void setTarget(VisibleObject creature) {
		target = creature;
	}

	/**
	 * @param objectId
	 * @return target is object with id equal to objectId
	 */
	public boolean isTargeting(int objectId) {
		return target != null && target.getObjectId() == objectId;
	}

	/**
	 * Return spawn template of this VisibleObject
	 *
	 * @return SpawnTemplate
	 */
	public SpawnTemplate getSpawn() {
		return spawn;
	}

	/** 设置刷新点。 / Sets the spawn. */
	public void setSpawn(SpawnTemplate spawn) {
		this.spawn = spawn;
	}

	/**
	 * @return the objectTemplate
	 */
	public VisibleObjectTemplate getObjectTemplate() {
		return objectTemplate;
	}

	/**
	 * @param objectTemplate the objectTemplate to set
	 */
	public void setObjectTemplate(VisibleObjectTemplate objectTemplate) {
		this.objectTemplate = objectTemplate;
	}

	/**
	 * @param position
	 */
	public void setPosition(WorldPosition position) {
		this.position = position;
	}

	/** 返回 visibility distance / Returns the visibility distance */
	public float getVisibilityDistance() {
		if (this instanceof Npc) {
			NpcTemplate npcTemplate = (NpcTemplate) this.getObjectTemplate();
			if (npcTemplate.getNpcTemplateType().equals(NpcTemplateType.FLAG)
					|| npcTemplate.getNpcTemplateType().equals(NpcTemplateType.HOUSING)
					|| npcTemplate.getNpcTemplateType().equals(NpcTemplateType.RAID_MONSTER)) {
				return Integer.MAX_VALUE;
			}
		}
		return VisibilityDistance;
	}

	/** 返回 max z visible distance / Returns the max z visible distance */
	public float getMaxZVisibleDistance() {
		if (this instanceof Npc) {
			NpcTemplate npcTemplate = (NpcTemplate) this.getObjectTemplate();
			if (npcTemplate.getNpcTemplateType().equals(NpcTemplateType.FLAG)
					|| npcTemplate.getNpcTemplateType().equals(NpcTemplateType.HOUSING)
					|| npcTemplate.getNpcTemplateType().equals(NpcTemplateType.RAID_MONSTER)) {
				return Integer.MAX_VALUE;
			}
		}
		return maxZvisibleDistance;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		if (objectTemplate == null) {
			return super.toString();
		}
		return objectTemplate.getName() + " (" + objectTemplate.getTemplateId() + ")";
	}

	/** 获取世界掉落类型。 / Returns the world drop type. */
	public WorldDropType getWorldDropType() {
		return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(getWorldId()).getWorldDropType();
	}
}
