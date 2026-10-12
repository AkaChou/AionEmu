package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerSkillSkinPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_skill_skins 的映射层。
 * Mapper for table player_skill_skins.
 */
public interface PlayerSkillSkinMapper extends BaseMapper<PlayerSkillSkinPo> {

	/**
	 * 插入或更新技能皮肤（表无唯一键，方言 SQL 逐字保留）。
	 * Inserts or updates a skill skin (dialect SQL kept verbatim; the table has no unique key).
	 * @param playerId 玩家对象 ID / player object id
	 * @param skinId 皮肤 ID / skin id
	 * @param remaining 剩余时间 / remaining time
	 * @param active 激活值 / active flag
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_skill_skins`(`player_id`, `skin_id`, `remaining`, `active`) VALUES (#{playerId},#{skinId},#{remaining},#{active}) ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`), `active` = VALUES(`active`)")
	int upsert(@Param("playerId") int playerId, @Param("skinId") int skinId, @Param("remaining") int remaining,
		@Param("active") int active);
}
