package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingPicture;

/**
 * Picture 对象。
 * Picture Object game object.
 */

public class PictureObject extends HouseObject<HousingPicture> {

	public PictureObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 使用时 / on Use. */
	@Override
	public void onUse(Player player) {
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
