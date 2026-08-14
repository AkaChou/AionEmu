package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingChair;

/**
 * Chair 对象。
 * Chair Object game object.
 */

public class ChairObject extends HouseObject<HousingChair> {

	public ChairObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 使用时 / On use. */
	@Override
	public void onUse(Player player) {
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
