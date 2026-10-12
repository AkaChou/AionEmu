package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;
import com.aionemu.gameserver.persistence.entity.PlayerEquipmentSettingPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家装备方案设置数据访问层。
 * Player equipment-setting data access layer.
 */
public interface PlayerEquipmentSettingDao extends IService<PlayerEquipmentSettingPo> {

	/**
	 * 加载玩家装备方案设置。
	 * Loads the player's equipment settings.
	 * @param player 玩家 / player
	 */
	void loadEquipmentSetting(Player player);

	/**
	 * 插入一条装备方案设置。
	 * Inserts an equipment setting entry.
	 * @param player 玩家 / player
	 * @param equipmentSetting 装备设置 / equipment setting
	 */
	void insertEquipmentSetting(Player player, EquipmentSetting equipmentSetting);

	/**
	 * 持久化玩家所有需要更新的装备方案。
	 * Persists all equipment settings that require an update for the player.
	 * @param player 玩家 / player
	 */
	default void store(Player player) {
		if (player.getEquipmentSettingList() == null) {
			return;
		}
		for (EquipmentSetting setting : player.getEquipmentSettingList().getEquipmentSetting()) {
			if (setting.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
				insertEquipmentSetting(player, setting);
			}
		}
	}
}
