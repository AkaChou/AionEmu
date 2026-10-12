package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerVarPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_vars 的映射层。
 * Mapper for table player_vars.
 */
public interface PlayerVarMapper extends BaseMapper<PlayerVarPo> {

	/**
	 * 插入或更新变量（MySQL upsert，保留库内 NOW() 求值）。
	 * Inserts or updates a variable (MySQL upsert, keeping in-database NOW() evaluation).
	 * @param playerId 玩家对象 ID / player object id
	 * @param param 变量键 / variable key
	 * @param value 变量值 / variable value
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO player_vars (`player_id`, `param`, `value`, `time`) VALUES (#{playerId},#{param},#{value},NOW()) ON DUPLICATE KEY UPDATE `value`=VALUES(`value`), `time`=NOW()")
	int upsert(@Param("playerId") int playerId, @Param("param") String param, @Param("value") String value);
}
