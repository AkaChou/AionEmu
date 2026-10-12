package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;

import com.aionemu.gameserver.persistence.entity.PlayerEquipmentSettingPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_equipment_setting 的映射层，仅保留无法用 Flex CRUD 表达的 upsert。
 * Mapper for table player_equipment_setting, keeping only the upsert that Flex CRUD cannot express.
 */
public interface PlayerEquipmentSettingMapper extends BaseMapper<PlayerEquipmentSettingPo> {

	/**
	 * 插入或更新装备方案（ON DUPLICATE KEY UPDATE；方案名仅在库内为空时被覆盖）。
	 * Inserts or updates an equipment setting (ON DUPLICATE KEY UPDATE; the name is only overwritten
	 * when the stored one is empty).
	 *
	 * @param po 装备方案行 / equipment setting row
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_equipment_setting` ("
		+ "`player_id`, `slot`, `name`, `display`, `m_hand`, `s_hand`, `helmet`, `torso`, "
		+ "`glove`, `boots`, `earrings_left`, `earrings_right`, `ring_left`, `ring_right`, `necklace`, `shoulder`, "
		+ "`pants`, `powershard_left`, `powershard_right`, `wings`, `waist`, `m_off_hand`, `s_off_hand`, `plume`, "
		+ "`bracelet`"
		+ ") VALUES ("
		+ "#{playerId}, #{slot}, #{name}, #{display}, #{mHand}, #{sHand}, #{helmet}, #{torso}, "
		+ "#{glove}, #{boots}, #{earringsLeft}, #{earringsRight}, #{ringLeft}, #{ringRight}, #{necklace}, #{shoulder}, "
		+ "#{pants}, #{powershardLeft}, #{powershardRight}, #{wings}, #{waist}, #{mOffHand}, #{sOffHand}, #{plume}, "
		+ "#{bracelet})"
		+ " ON DUPLICATE KEY UPDATE "
		+ "`name` = IF(`name` = '', VALUES(`name`), `name`), `display` = VALUES(`display`), "
		+ "`m_hand` = VALUES(`m_hand`), `s_hand` = VALUES(`s_hand`), `helmet` = VALUES(`helmet`), "
		+ "`torso` = VALUES(`torso`), `glove` = VALUES(`glove`), `boots` = VALUES(`boots`), "
		+ "`earrings_left` = VALUES(`earrings_left`), `earrings_right` = VALUES(`earrings_right`), "
		+ "`ring_left` = VALUES(`ring_left`), `ring_right` = VALUES(`ring_right`), `necklace` = VALUES(`necklace`), "
		+ "`shoulder` = VALUES(`shoulder`), `pants` = VALUES(`pants`), `powershard_left` = VALUES(`powershard_left`), "
		+ "`powershard_right` = VALUES(`powershard_right`), `wings` = VALUES(`wings`), `waist` = VALUES(`waist`), "
		+ "`m_off_hand` = VALUES(`m_off_hand`), `s_off_hand` = VALUES(`s_off_hand`), `plume` = VALUES(`plume`), "
		+ "`bracelet` = VALUES(`bracelet`)")
	int upsertSetting(PlayerEquipmentSettingPo po);
}
