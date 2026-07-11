package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingJukeBox;

/**
 * JukeBox 对象。
 * Juke Box Object game object.
 */

public class JukeBoxObject extends HouseObject<HousingJukeBox> {

	public JukeBoxObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
