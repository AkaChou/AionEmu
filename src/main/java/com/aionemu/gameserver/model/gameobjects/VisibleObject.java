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
	 * 返回当前对象所在的 WorldRegion。
	 * Returns the current WorldRegion the AionObject is in.
	 *
	 * @return 当前区域 / current WorldRegion
	 */
	public MapRegion getActiveRegion() {
		return position.getMapRegion();
	}

	/** 返回副本 ID / Returns the instance id */
	public int getInstanceId() {
		return position.getInstanceId();
	}

	/**
	 * 返回世界地图 ID。
	 * Returns the world map id.
	 *
	 * @return 地图 ID / world map id
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
	 * 返回世界 X 坐标。
	 * Returns the world x position.
	 *
	 * @return X 坐标 / x position
	 */
	public float getX() {
		return position.getX();
	}

	/**
	 * 返回世界 Y 坐标。
	 * Returns the world y position.
	 *
	 * @return Y 坐标 / y position
	 */
	public float getY() {
		return position.getY();
	}

	/**
	 * 返回世界 Z 坐标。
	 * Returns the world z position.
	 *
	 * @return Z 坐标 / z position
	 */
	public float getZ() {
		return position.getZ();
	}

	/** 设置坐标与朝向 / Sets the xyzh */
	public void setXYZH(Float x, Float y, Float z, Byte h) {
		position.setXYZH(x, y, z, h);
	}

	/**
	 * 返回对象朝向。
	 * Returns the object heading.
	 *
	 * @return 对象朝向，取值 [0,120) / heading of the object, values from [0,120)
	 */
	public byte getHeading() {
		return position.getHeading();
	}

	/**
	 * 返回对象位置。
	 * Return object position.
	 *
	 * @return 世界位置 / position
	 */
	public WorldPosition getPosition() {
		return position;
	}

	/**
	 * 检查对象是否已生成。
	 * Checks whether the object is spawned.
	 *
	 * @return 是否已生成 / true if spawned
	 */
	public boolean isSpawned() {
		return position.isSpawned();
	}

	/**
	 * 对象是否已在世界中。
	 * Whether the object is in the world.
	 *
	 * @return 是否在世界中 / whether in the world
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

	/** 清除已知列表 / Clears the known list */
	public void clearKnownlist() {
		getKnownList().clear();
	}

	/** 更新已知列表 / Updates the known list */
	public void updateKnownlist() {
		getKnownList().doUpdate();
	}

	/**
	 * 判断能否看见指定生物。
	 * Whether this object can see the given creature.
	 *
	 * @param creature 目标生物 / target creature
	 * @return 是否可见 / whether visible
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
	  * 返回此对象的 KnownList。
	  * Returns the KnownList of this VisibleObject.
	  *
	  * @return 已知列表 / known list
	  */
	public KnownList getKnownList() {
		return knownlist;
	}

	 /**
	  * 返回此对象的控制器。
	  * Returns the controller of this VisibleObject.
	  *
	  * @return 控制器 / VisibleObjectController
	  */
	public VisibleObjectController<? extends VisibleObject> getController() {
		return controller;
	}

	/**
	 * 返回当前目标。
	 * Returns the current target.
	 *
	 * @return 目标对象 / target VisibleObject
	 */
	public final VisibleObject getTarget() {
		return target;
	}

	/**
	 * 返回到目标的距离，无目标时为 0。
	 * Returns the distance to the target, or 0 if no target.
	 *
	 * @return 到目标的距离 / distance to target
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
	 * 设置目标对象。
	 * Sets the target object.
	 *
	 * @param creature 目标对象 / target object
	 */
	public void setTarget(VisibleObject creature) {
		target = creature;
	}

	/**
	 * 目标是否为指定对象 ID。
	 * Whether the target has the given object id.
	 *
	 * @param objectId 对象 ID / object id
	 * @return 是否为目标 / whether the target matches
	 */
	public boolean isTargeting(int objectId) {
		return target != null && target.getObjectId() == objectId;
	}

	/**
	 * 返回该对象的生成模板。
	 * Returns the spawn template of this VisibleObject.
	 *
	 * @return 生成模板 / SpawnTemplate
	 */
	public SpawnTemplate getSpawn() {
		return spawn;
	}

	/** 设置刷新点。 / Sets the spawn. */
	public void setSpawn(SpawnTemplate spawn) {
		this.spawn = spawn;
	}

	/**
	 * 返回对象模板。
	 * Returns the object template.
	 *
	 * @return 对象模板 / the objectTemplate
	 */
	public VisibleObjectTemplate getObjectTemplate() {
		return objectTemplate;
	}

	/**
	 * 设置对象模板。
	 * Sets the object template.
	 *
	 * @param objectTemplate 要设置的对象模板 / the objectTemplate to set
	 */
	public void setObjectTemplate(VisibleObjectTemplate objectTemplate) {
		this.objectTemplate = objectTemplate;
	}

	/**
	 * 设置世界位置。
	 * Sets the world position.
	 *
	 * @param position 世界位置 / world position
	 */
	public void setPosition(WorldPosition position) {
		this.position = position;
	}

	/** 返回可见距离 / Returns the visibility distance */
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

	/** 返回最大 Z 可见距离 / Returns the max z visible distance */
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
