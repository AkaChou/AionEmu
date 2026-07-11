package com.aionemu.gameserver.model.house;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;
import com.aionemu.gameserver.model.gameobjects.HouseDecoration;
import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.templates.housing.PartType;

/**
 * 房屋 Registry 模型。
 * House Registry model.
 */
@Slf4j

public class HouseRegistry {
	private House owner;
	private Map<Integer, HouseObject<?>> objects;
	private Map<Integer, HouseDecoration> customParts;
	private HouseDecoration[] defaultParts = new HouseDecoration[28];
	private PersistentState persistentState = PersistentState.UPDATED;

	public HouseRegistry(House owner) {
		this.owner = owner;
		this.objects = new HashMap<Integer, HouseObject<?>>();
		this.customParts = new HashMap<Integer, HouseDecoration>();
	}

	/** 返回所有者 / Returns the owner*/
	public House getOwner() {
		return owner;
	}

	/** 返回对象 / Returns the objects*/
	public List<HouseObject<?>> getObjects() {
		List<HouseObject<?>> temp = new ArrayList<HouseObject<?>>();
		for (HouseObject<?> obj : objects.values()) {
			temp.add(obj);
		}
		return temp;
	}

	/** 返回 spawned objects / Returns the spawned objects */
	public List<HouseObject<?>> getSpawnedObjects() {
		List<HouseObject<?>> temp = new ArrayList<HouseObject<?>>();
		for (HouseObject<?> obj : objects.values()) {
			if (obj.isSpawnedByPlayer() && obj.getPersistentState() != PersistentState.DELETED) {
				temp.add(obj);
			}
		}
		return temp;
	}

	/** 返回 not spawned objects / Returns the not spawned objects */
	public List<HouseObject<?>> getNotSpawnedObjects() {
		List<HouseObject<?>> temp = new ArrayList<HouseObject<?>>();
		for (HouseObject<?> obj : objects.values()) {
			if (!obj.isSpawnedByPlayer() && obj.getPersistentState() != PersistentState.DELETED) {
				temp.add(obj);
			}
		}
		return temp;
	}

	/** 返回按对象 ID 的对象 / Returns the object by obj id */
	public HouseObject<?> getObjectByObjId(int itemObjId) {
		return objects.get(itemObjId);
	}

	/** 放入对象。 / Put object. */
	public boolean putObject(HouseObject<?> houseObject) {
		if (objects.containsKey(houseObject.getObjectId())) {
			return false;
		}
		if (houseObject.getPersistentState() != PersistentState.NEW) {
			log.error(I18n.get("log.bb17db8cad35", houseObject.getObjectId()));
			return false;
		}
		objects.put(houseObject.getObjectId(), houseObject);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		return true;
	}

	/** 移除对象。 / Removes object. */
	public HouseObject<?> removeObject(int itemObjId) {
		if (!objects.containsKey(itemObjId)) {
			return null;
		}
		HouseObject<?> oldObject = objects.get(itemObjId);
		if (oldObject.getPersistentState() == PersistentState.NEW) {
			discardObject(itemObjId);
		} else {
			oldObject.setPersistentState(PersistentState.DELETED);
		}
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		return oldObject;
	}

	/** 返回 custom parts / Returns the custom parts */
	public List<HouseDecoration> getCustomParts() {
		List<HouseDecoration> temp = new ArrayList<HouseDecoration>();
		for (HouseDecoration decor : customParts.values()) {
			if (decor.getPersistentState() != PersistentState.DELETED && !decor.isUsed()) {
				temp.add(decor);
			}
		}
		return temp;
	}

	/** 按 type 返回 custom part / Returns the custom part by type */
	public HouseDecoration getCustomPartByType(PartType partType, int floor) {
		for (HouseDecoration deco : customParts.values()) {
			if (deco.getPersistentState() != PersistentState.DELETED && deco.getTemplate().getType() == partType) {
				if (floor == deco.getFloor()) {
					return deco;
				}
			}
		}
		return null;
	}

	/** 按 obj id 返回 custom part / Returns the custom part by obj id */
	public HouseDecoration getCustomPartByObjId(int itemObjId) {
		return customParts.get(itemObjId);
	}

	/** 按 part id 返回 custom part / Returns the custom part by part id */
	public HouseDecoration getCustomPartByPartId(int partId, int floor) {
		for (HouseDecoration deco : customParts.values()) {
			if (deco.getPersistentState() != PersistentState.DELETED && deco.getTemplate().getId() == partId
					&& deco.getFloor() == floor) {
				return deco;
			}
		}
		return null;
	}

	/** 按 part id 返回 custom part count / Returns the custom part count by part id */
	public int getCustomPartCountByPartId(int partId) {
		int counter = 0;
		for (HouseDecoration deco : customParts.values()) {
			if (deco.getPersistentState() != PersistentState.DELETED && deco.getTemplate().getId() == partId) {
				counter++;
			}
		}
		return counter;
	}

	/** Put custom part / Put custom part */
	public boolean putCustomPart(HouseDecoration houseDeco) {
		if (customParts.containsKey(houseDeco.getObjectId())) {
			return false;
		}
		if (houseDeco.getPersistentState() != PersistentState.NEW) {
			log.error(I18n.get("log.8c00f2590c9b", houseDeco.getObjectId()));
			return false;
		}
		customParts.put(houseDeco.getObjectId(), houseDeco);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		return true;
	}

	/** 移除 custom part / Removes custom part */
	public HouseDecoration removeCustomPart(int itemObjId) {
		HouseDecoration obj = null;
		if (customParts.containsKey(itemObjId)) {
			obj = customParts.get(itemObjId);
			obj.setPersistentState(PersistentState.DELETED);
			setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		return obj;
	}

	/** 返回 default parts / Returns the default parts */
	public List<HouseDecoration> getDefaultParts() {
		List<HouseDecoration> temp = new ArrayList<HouseDecoration>();
		for (HouseDecoration deco : defaultParts) {
			if (deco != null) {
				temp.add(deco);
			}
		}
		return temp;
	}

	/** 按 type 返回 default part / Returns the default part by type */
	public HouseDecoration getDefaultPartByType(PartType partType, int floor) {
		return defaultParts[partType.getStartLineNr() + floor];
	}

	/** Put default part / Put default part */
	public void putDefaultPart(HouseDecoration houseDeco, int floor) {
		defaultParts[houseDeco.getTemplate().getType().getStartLineNr() + floor] = houseDeco;
		houseDeco.setPersistentState(PersistentState.NOACTION);
	}

	/** 返回 all parts / Returns the all parts */
	public List<HouseDecoration> getAllParts() {
		List<HouseDecoration> temp = new ArrayList<HouseDecoration>();
		for (HouseDecoration deco : defaultParts) {
			if (deco != null) {
				temp.add(deco);
			}
		}
		for (HouseDecoration decor : customParts.values()) {
			temp.add(decor);
		}
		return temp;
	}

	/** 返回 render part / Returns the render part */
	public HouseDecoration getRenderPart(PartType partType, int floor) {
		for (HouseDecoration decor : customParts.values()) {
			if (decor.getTemplate().getType() == partType && decor.isUsed() && decor.getFloor() == floor) {
				return decor;
			}
		}
		return getDefaultPartByType(partType, floor);
	}

	/** 设置 part in use / Sets the part in use */
	public void setPartInUse(HouseDecoration decorationUse, int floor) {
		HouseDecoration defaultDecor = defaultParts[decorationUse.getTemplate().getType().getStartLineNr() + floor];
		if (defaultDecor.getTemplate().getId() == decorationUse.getTemplate().getId()) {
			defaultDecor.setUsed(true);
			for (HouseDecoration decor : customParts.values()) {
				if (decor.getTemplate().getType() != decorationUse.getTemplate().getType()) {
					continue;
				}
				if (decor.getPersistentState() != PersistentState.DELETED) {
					if (decor.isUsed()) {
						decor.setUsed(false);
						decor.setFloor(-1);
						if (decor.getPersistentState() == PersistentState.NEW) {
							discardPart(decor);
						} else {
							decor.setPersistentState(PersistentState.DELETED);
						}
					}
				}
			}
			return;
		}
		for (HouseDecoration decor : customParts.values()) {
			if (decor.getTemplate().getType() != decorationUse.getTemplate().getType()) {
				continue;
			}
			if (decor.getPersistentState() != PersistentState.DELETED) {
				if (decorationUse.equals(decor)) {
					decor.setUsed(true);
					decor.setFloor(floor);
					defaultDecor.setUsed(false);
				} else {
					if (decor.isUsed() && !decorationUse.equals(decor) && decor.getFloor() == floor) {
						decor.setUsed(false);
						decor.setFloor(-1);
						if (decor.getPersistentState() == PersistentState.NEW) {
							discardPart(decor);
						} else {
							decor.setPersistentState(PersistentState.DELETED);
						}
					}
				}
			}
		}
	}

	/** 丢弃对象 / Discard Object */
	public void discardObject(Integer objectId) {
		objects.remove(objectId);
	}

	/** 丢弃部件 / discard Part. */
	public void discardPart(HouseDecoration decor) {
		customParts.remove(decor.getObjectId());
	}

	/** 保存。 / Save. */
	public void save() {
		if (persistentState == PersistentState.UPDATE_REQUIRED) {
			DAOManager.getDAO(PlayerRegisteredItemsDAO.class).store(this, getOwner().getOwnerId());
		}
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public final PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public final void setPersistentState(PersistentState persistentState) {
		this.persistentState = persistentState;
	}

	/** 大小 / size. */
	public int size() {
		return objects.size() + customParts.size();
	}

	/** 移除对象 / Despawn objects */
	public void despawnObjects() {
		if (getSpawnedObjects().isEmpty()) {
			DAOManager.getDAO(PlayerRegisteredItemsDAO.class).resetRegistry(owner.getOwnerId());
		} else {
			despawnObjects(true);
		}
	}

	/** 移除对象 / Despawn objects */
	public void despawnObjects(boolean remove) {
		for (HouseObject<?> obj : getSpawnedObjects()) {
			if (obj.isInWorld()) {
				obj.getController().onDelete();
				obj.clearKnownlist();
			}
			if (remove) {
				obj.removeFromHouse();
			}
		}
		if (remove) {
			setPersistentState(PersistentState.UPDATE_REQUIRED);
			save();
		}
	}
}
