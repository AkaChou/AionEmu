package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;

import com.aionemu.gameserver.persistence.entity.PlayerSettingPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_settings 的映射层，仅保留无法用 Flex CRUD 表达的 REPLACE INTO。
 * Mapper for table player_settings, keeping only the REPLACE INTO that Flex CRUD cannot express.
 */
public interface PlayerSettingMapper extends BaseMapper<PlayerSettingPo> {

	/**
	 * 整体替换某类玩家设置行（REPLACE INTO，复合主键 player_id + settings_type）。
	 * Replaces one player settings row (REPLACE INTO, composite key player_id + settings_type).
	 *
	 * @param po 设置行 / settings row
	 * @return 影响行数 / affected rows
	 */
	@Insert("REPLACE INTO player_settings VALUES (#{playerId}, #{settingsType}, #{settings})")
	int replaceSetting(PlayerSettingPo po);
}
