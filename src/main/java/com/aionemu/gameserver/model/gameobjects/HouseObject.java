package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.controllers.PlaceableObjectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.AbstractHouseObject;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.housing.HousingCategory;
import com.aionemu.gameserver.model.templates.housing.LimitType;
import com.aionemu.gameserver.model.templates.housing.PlaceArea;
import com.aionemu.gameserver.model.templates.housing.PlaceLocation;
import com.aionemu.gameserver.model.templates.housing.PlaceableHouseObject;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;

/**
 * 房屋对象。
 * House Object game object.
 *
 * @author Rolandas
 */
public abstract class HouseObject<T extends PlaceableHouseObject> extends VisibleObject implements IExpirable {

	private int expireEnd;
	private float x;
	private float y;
	private float z;
	private byte heading;
	private int ownerUsedCount = 0;
	private int visitorUsedCount = 0;
	private Integer color = null;
	private int colorExpireEnd;

	private House ownerHouse;
	// 切勿直接设置！！！请改用 setPersistentState()。 / don't set it directly, ever!!! Use setPersistentState() method instead
	private PersistentState persistentState = PersistentState.NEW;

	public HouseObject(House owner, int objId, int templateId) {
		super(objId, new PlaceableObjectController<T>(), null,
				DataManager.HOUSING_OBJECT_DATA.getTemplateById(templateId), null);
		this.ownerHouse = owner;
		getController().setOwner(this);
		setKnownlist(new PlayerAwareKnownList(this));
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case DELETED:
			if (this.persistentState == PersistentState.NEW) {
				this.persistentState = PersistentState.NOACTION;
			} else if (this.persistentState != PersistentState.DELETED) {
				this.persistentState = PersistentState.DELETED;
				ownerHouse.getRegistry().setPersistentState(PersistentState.UPDATE_REQUIRED);
			}
			break;
		case UPDATE_REQUIRED:
			if (this.persistentState == PersistentState.NEW) {
				break;
			}
		default:
			if (this.persistentState != persistentState) {
				this.persistentState = persistentState;
				ownerHouse.getRegistry().setPersistentState(PersistentState.UPDATE_REQUIRED);
			}
		}
	}

	/** 获取过期时间。 / Returns the expire time. */
	@Override
	public int getExpireTime() {
		return expireEnd;
	}

	/** 设置过期时间。 / Sets the expire time. */
	public void setExpireTime(int time) {
		expireEnd = time;
	}

	/** 到期结束 / Expire End */
	@Override
	public void expireEnd(Player player) {
		setPersistentState(PersistentState.DELETED);
	}

	/**
	 * 获取 secondsleft 对象 usehasnoexpirationreturn1。
	 * Gets seconds left for the object use. If has no expiration return -1
	 *
	 * @return
	 */
	public int getUseSecondsLeft() {
		if (expireEnd == 0) {
			return -1;
		}
		int diff = expireEnd - (int) (System.currentTimeMillis() / 1000);
		if (diff < 0) {
			return 0;
		}
		return diff;
	}

	/** 过期消息。 / Expire Message. */
	@Override
	public void expireMessage(Player player, int time) {
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return String.valueOf(objectTemplate.getNameId());
	}

	@SuppressWarnings("unchecked")
	/** 获取对象模板。 / Returns the object template. */
	public T getObjectTemplate() {
		return (T) objectTemplate;
	}

	/** 返回 x / Returns the x */
	@Override
	public float getX() {
		return x;
	}

	/** 设置 x / Sets the x */
	public void setX(float x) {
		if (this.x != x) {
			this.x = x;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
			if (position != null) {
				position.setXYZH(x, null, null, null);

			}
		}
	}

	/** 返回 y / Returns the y */
	@Override
	public float getY() {
		return y;
	}

	/** 设置 y / Sets the y */
	public void setY(float y) {
		if (this.y != y) {
			this.y = y;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
			if (position != null) {
				position.setXYZH(null, y, null, null);
			}
		}
	}

	/** 返回 z / Returns the z */
	@Override
	public float getZ() {
		return z;
	}

	/** 设置 z / Sets the z */
	public void setZ(float z) {
		if (this.z != z) {
			this.z = z;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
			if (position != null) {
				position.setXYZH(null, null, z, null);
			}
		}
	}

	/** 返回 heading / Returns the heading */
	@Override
	public byte getHeading() {
		return heading;
	}

	/** 设置 heading / Sets the heading */
	public void setHeading(byte heading) {
		if (this.heading != heading) {
			this.heading = heading;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
			if (position != null) {
				position.setXYZH(null, null, null, heading);
			}
		}
	}

	/** 返回 rotation / Returns the rotation */
	public int getRotation() {
		int rotation = this.heading & 0xFF;
		return rotation * 3;
	}

	/** 设置 rotation / Sets the rotation */
	public void setRotation(int rotation) {
		setHeading((byte) Math.ceil(rotation / 3f));
	}

	/** 返回 place location / Returns the place location */
	public PlaceLocation getPlaceLocation() {
		return ((PlaceableHouseObject) objectTemplate).getLocation();
	}

	/** 返回 place area / Returns the place area */
	public PlaceArea getPlaceArea() {
		return ((PlaceableHouseObject) objectTemplate).getArea();
	}

	/** 返回 placement limit / Returns the placement limit */
	public int getPlacementLimit(boolean trial) {
		LimitType limitType = ((PlaceableHouseObject) objectTemplate).getPlacementLimit();
		HouseType size = HouseType.fromValue(ownerHouse.getBuilding().getSize());
		if (trial) {
			return limitType.getTrialObjectPlaceLimit(size);
		}
		return limitType.getObjectPlaceLimit(size);
	}

	/** 返回 quality / Returns the quality */
	public ItemQuality getQuality() {
		return ((AbstractHouseObject) objectTemplate).getQuality();
	}

	/** 返回 talking distance / Returns the talking distance */
	public float getTalkingDistance() {
		return ((AbstractHouseObject) objectTemplate).getTalkingDistance();
	}

	/** 获取分类。 / Returns the category. */
	public HousingCategory getCategory() {
		return ((AbstractHouseObject) objectTemplate).getCategory();
	}

	/** 返回 owner house / Returns the owner house */
	public House getOwnerHouse() {
		return ownerHouse;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return ownerHouse.getOwnerId();
	}

	/** 返回所有者已使用数量 / Returns the owner used count*/
	public int getOwnerUsedCount() {
		return ownerUsedCount;
	}

	/** 递增所有者已用次数 / Increment Owner Used Count */
	public void incrementOwnerUsedCount() {
		this.ownerUsedCount++;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 递增 visitor used count / Increment Visitor Used Count */
	public void incrementVisitorUsedCount() {
		this.visitorUsedCount++;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 设置所有者已使用数量 / Sets the owner used count*/
	public void setOwnerUsedCount(int ownerUsedCount) {
		if (this.ownerUsedCount != ownerUsedCount) {
			this.ownerUsedCount = ownerUsedCount;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
	}

	/** 返回 visitor used count / Returns the visitor used count */
	public int getVisitorUsedCount() {
		return visitorUsedCount;
	}

	/** 设置 visitor used count / Sets the visitor used count */
	public void setVisitorUsedCount(int visitorUsedCount) {
		if (this.visitorUsedCount != visitorUsedCount) {
			this.visitorUsedCount = visitorUsedCount;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
	}

	/**
	 * @return 表示玩家已生成该对象，而非游戏服务器侧。 / Means the player has it spawned, not the game server
	 */
	public boolean isSpawnedByPlayer() {
		return x != 0 || y != 0 || z != 0;
	}

	@SuppressWarnings("unchecked")
	/** 返回 controller / Returns the controller */
	@Override
	public PlaceableObjectController<T> getController() {
		return (PlaceableObjectController<T>) super.getController();
	}

	/** 生成。 / Spawn. */
	public void spawn() {
		if (!isSpawnedByPlayer()) {
			return;
		}
		World w = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		if (position == null || !isSpawned()) {
			position = w.createPosition(ownerHouse.getWorldId(), x, y, z, heading, ownerHouse.getInstanceId());
			SpawnEngine.bringIntoWorld(this);
		}
		updateKnownlist();
	}

	/**
	 * 移除 house 从 spawnbut 其 remains 在 registry。 / Removes house from spawn but it remains in registry
	 */
	public void removeFromHouse() {
		this.setX(0);
		this.setY(0);
		this.setZ(0);
		this.setHeading((byte) 0);
	}

	/** 使用时 / on Use. */
	public void onUse(Player player) {
	}

	/** 在 DialogRequest / On Dialog Request */
	public void onDialogRequest(Player player) {
		onUse(player);
	}

	/** 消失时 / on Despawn. */
	public void onDespawn() {

	}

	/** 返回 color / Returns the color */
	public Integer getColor() {
		return color;
	}

	/** 设置 color / Sets the color */
	public void setColor(Integer color) {
		if (color != this.color) {
			this.color = color;
			setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
	}

	/** 返回 color expire end / Returns the color expire end */
	public int getColorExpireEnd() {
		return colorExpireEnd;
	}

	/** 设置 color expire end / Sets the color expire end */
	public void setColorExpireEnd(int colorExpireEnd) {
		this.colorExpireEnd = colorExpireEnd;
	}
}
