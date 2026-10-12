package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.aionemu.gameserver.persistence.entity.PlayerCpPo;

/**
 * 表 player_cp 的映射层。
 * Mapper for table player_cp.
 */
public interface PlayerCpMapper extends BaseMapper<PlayerCpPo> {

	/**
	 * 插入或更新创意点（方言 SQL，逐字保留）。
	 * Inserts or updates a creativity-points entry (dialect SQL kept verbatim).
	 * @param playerId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @param point 点数 / points
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_cp` (`player_id`, `slot`, `point`) VALUES(#{playerId},#{slot},#{point}) "
		+ "ON DUPLICATE KEY UPDATE `slot` = VALUES(`slot`), `point` = VALUES(`point`)")
	int upsert(@Param("playerId") int playerId, @Param("slot") int slot, @Param("point") int point);
}
