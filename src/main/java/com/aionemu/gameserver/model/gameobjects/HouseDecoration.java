package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.housing.HousePart;

/**
 * 房屋 Decoration 游戏对象。
 * House Decoration game object.
 */

public class HouseDecoration extends AionObject {
	private int templateId;
	private byte floor;
	private boolean isUsed;
	private PersistentState persistentState;

	public HouseDecoration(int objectId, int templateId) {
		this(objectId, templateId, -1);
	}

	public HouseDecoration(int objectId, int templateId, int floor) {
		super(objectId);
		this.templateId = templateId;
		this.floor = (byte) floor;
		this.persistentState = PersistentState.NEW;
	}

	/** 获取模板。 / Returns the template. */
	public HousePart getTemplate() {
		return DataManager.HOUSE_PARTS_DATA.getPartById(templateId);
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		this.persistentState = persistentState;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return getTemplate().getName();
	}

	/** 返回楼层 / Returns the floor */
	public byte getFloor() {
		return floor;
	}

	/** 设置楼层 / Sets the floor */
	public void setFloor(int value) {
		if (value != floor) {
			floor = (byte) value;
			if (persistentState != PersistentState.NEW && persistentState != PersistentState.NOACTION) {
				persistentState = PersistentState.UPDATE_REQUIRED;
			}
		}
	}

	/** 是否已用 / Whether used. */
	public boolean isUsed() {
		return isUsed;
	}

	/** 设置是否使用 / Sets whether used */
	public void setUsed(boolean isUsed) {
		if (this.isUsed != isUsed && persistentState != PersistentState.DELETED) {
			this.isUsed = isUsed;
			if (persistentState != PersistentState.NEW && persistentState != PersistentState.NOACTION) {
				persistentState = PersistentState.UPDATE_REQUIRED;
			}
		}
	}

	/** 是否相等。 / Equality check. */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof HouseDecoration)) {
			return false;
		} else {
			return ((HouseDecoration) object).getObjectId().equals(this.getObjectId());
		}
	}

	/** 返回哈希码。 / Returns hash code. */
	@Override
	public int hashCode() {
		return this.getObjectId();
	}
}
