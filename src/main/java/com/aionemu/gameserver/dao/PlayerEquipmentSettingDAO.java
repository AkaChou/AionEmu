package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;

/**
 * 玩家装备方案设置数据访问对象。
 * Player equipment-setting data access object.
 */
public abstract class PlayerEquipmentSettingDAO implements DAO {

	/**
	 * 加载玩家装备方案设置。
	 * Loads the player's equipment settings.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadEquipmentSetting(Player player);

	/**
	 * 插入一条装备方案设置。
	 * Inserts an equipment setting entry.
	 *
	 * @param player 玩家 / player
	 * @param equipmentSetting 装备设置 / equipment setting
	 */
	public abstract void insertEquipmentSetting(Player player, EquipmentSetting equipmentSetting);

	/**
	 * 持久化玩家所有需要更新的装备方案。
	 * Persists all equipment settings that require an update for the player.
	 *
	 * @param player 玩家 / player
	 */
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

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerEquipmentSettingDAO.class.getName();
	}
}
