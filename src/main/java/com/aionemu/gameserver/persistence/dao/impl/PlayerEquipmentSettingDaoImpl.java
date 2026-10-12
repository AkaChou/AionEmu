package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSettingList;
import com.aionemu.gameserver.persistence.dao.PlayerEquipmentSettingDao;
import com.aionemu.gameserver.persistence.entity.PlayerEquipmentSettingPo;
import com.aionemu.gameserver.persistence.mapper.PlayerEquipmentSettingMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家装备预设数据访问实现。
 * Player equipment-setting data access implementation.
 */
@Slf4j
@Service
@Lazy
public class PlayerEquipmentSettingDaoImpl extends ServiceImpl<PlayerEquipmentSettingMapper, PlayerEquipmentSettingPo>
	implements PlayerEquipmentSettingDao {

	/**
	 * 加载玩家装备预设列表。
	 * Loads a player's equipment setting list.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadEquipmentSetting(Player player) {
		EquipmentSettingList equipmentSettingList = new EquipmentSettingList(player);
		try {
			mapper.selectListByQuery(QueryWrapper.create().eq(PlayerEquipmentSettingPo::getPlayerId, player.getObjectId()))
				.forEach(po -> equipmentSettingList.add(po.getSlot(), po.getName(), po.getDisplay(), po.getMHand(),
					po.getSHand(), po.getHelmet(), po.getTorso(), po.getGlove(), po.getBoots(), po.getEarringsLeft(),
					po.getEarringsRight(), po.getRingLeft(), po.getRingRight(), po.getNecklace(), po.getShoulder(),
					po.getPants(), po.getPowershardLeft(), po.getPowershardRight(), po.getWings(), po.getWaist(),
					po.getMOffHand(), po.getSOffHand(), po.getPlume(), po.getBracelet(), false));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3864e5946e2d", player.getObjectId()), e);
		}
		player.setEquipmentSettingList(equipmentSettingList);
	}

	/**
	 * 插入或更新玩家装备预设。
	 * Inserts or updates a player's equipment setting.
	 * @param player 玩家 / player
	 * @param equipmentSetting 装备设置 / equipment setting
	 */
	@Override
	public void insertEquipmentSetting(Player player, EquipmentSetting equipmentSetting) {
		try {
			PlayerEquipmentSettingPo po = new PlayerEquipmentSettingPo();
			po.setPlayerId(player.getObjectId());
			po.setSlot(equipmentSetting.getSlot());
			po.setName(equipmentSetting.getName());
			po.setDisplay(equipmentSetting.getDisplay());
			po.setMHand(equipmentSetting.getmHand());
			po.setSHand(equipmentSetting.getsHand());
			po.setHelmet(equipmentSetting.getHelmet());
			po.setTorso(equipmentSetting.getTorso());
			po.setGlove(equipmentSetting.getGlove());
			po.setBoots(equipmentSetting.getBoots());
			po.setEarringsLeft(equipmentSetting.getEarringsLeft());
			po.setEarringsRight(equipmentSetting.getEarringsRight());
			po.setRingLeft(equipmentSetting.getRingLeft());
			po.setRingRight(equipmentSetting.getRingRight());
			po.setNecklace(equipmentSetting.getNecklace());
			po.setShoulder(equipmentSetting.getShoulder());
			po.setPants(equipmentSetting.getPants());
			po.setPowershardLeft(equipmentSetting.getPowershardLeft());
			po.setPowershardRight(equipmentSetting.getPowershardRight());
			po.setWings(equipmentSetting.getWings());
			po.setWaist(equipmentSetting.getWaist());
			po.setMOffHand(equipmentSetting.getmOffHand());
			po.setSOffHand(equipmentSetting.getsOffHand());
			po.setPlume(equipmentSetting.getPlume());
			po.setBracelet(equipmentSetting.getBracelet());
			mapper.upsertSetting(po);
			equipmentSetting.setPersistentState(PersistentState.UPDATED);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.593407c982a0", player.getObjectId()), e);
		}
	}
}
