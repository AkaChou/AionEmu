package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerTitlePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_titles 的映射层。
 * Mapper for table player_titles.
 */
public interface PlayerTitleMapper extends BaseMapper<PlayerTitlePo> {

	/**
	 * 插入或更新玩家称号（方言 SQL，逐字保留）。
	 * Inserts or updates a player title (dialect SQL kept verbatim).
	 * @param playerId 玩家对象 ID / player object id
	 * @param titleId 称号 ID / title id
	 * @param remaining 剩余毫秒数，0 表示永久 / remaining ms, 0 for permanent
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_titles` (`player_id`, `title_id`, `remaining`) VALUES (#{playerId}, #{titleId}, #{remaining}) "
		+ "ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`)")
	int upsert(@Param("playerId") int playerId, @Param("titleId") int titleId, @Param("remaining") int remaining);

	/**
	 * 插入或更新玩家称号（原生 JDBC 常量，供调用方事务复用）。
	 * Insert or update a player title (raw JDBC constant for caller-owned transactions).
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO `player_titles` (`player_id`, `title_id`, `remaining`) VALUES (?, ?, ?) "
		+ "ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`)";
}
