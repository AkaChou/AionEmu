package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingEmblem;

/**
 * 徽章对象。
 * Emblem Object game object.
 */

public class EmblemObject extends HouseObject<HousingEmblem> {
	public EmblemObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return false;
	}
}
