package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingPassiveItem;

/**
 * Passive 对象。
 * Passive Object game object.
 */

public class PassiveObject extends HouseObject<HousingPassiveItem> {

	public PassiveObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
