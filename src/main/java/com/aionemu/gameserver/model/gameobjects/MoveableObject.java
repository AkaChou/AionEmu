package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingMoveableItem;

/**
 * Moveable 对象。
 * Moveable Object game object.
 */

public class MoveableObject extends HouseObject<HousingMoveableItem> {

	public MoveableObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
