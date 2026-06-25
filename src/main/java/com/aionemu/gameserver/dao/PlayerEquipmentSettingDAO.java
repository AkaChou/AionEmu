package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;

public abstract class PlayerEquipmentSettingDAO implements DAO {

	public abstract void loadEquipmentSetting(Player player);

	public abstract void insertEquipmentSetting(Player player, EquipmentSetting equipmentSetting);

	public void store(Player player) {
		if (player.getEquipmentSettingList() == null) {
			return;
		}
		for (EquipmentSetting setting : player.getEquipmentSettingList().getEquipmentSetting()) {
			if (setting.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
				insertEquipmentSetting(player, setting);
			}
		}
	}

	@Override
	public String getClassName() {
		return PlayerEquipmentSettingDAO.class.getName();
	}
}
