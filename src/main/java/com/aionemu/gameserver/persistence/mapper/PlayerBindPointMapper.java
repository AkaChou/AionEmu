package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerBindPointPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_bind_point 的映射层。
 * Mapper for table player_bind_point.
 */
public interface PlayerBindPointMapper extends BaseMapper<PlayerBindPointPo> {

	/**
	 * 替换玩家绑定点（MySQL REPLACE INTO）。
	 * Replaces a player bind point (MySQL REPLACE INTO).
	 * @param playerId 玩家对象 ID / player object id
	 * @param mapId 地图 ID / map id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param heading 朝向 / heading
	 * @return 影响行数 / affected rows
	 */
	@Insert("REPLACE INTO `player_bind_point` (`player_id`, `map_id`, `x`, `y`, `z`, `heading`) "
		+ "VALUES (#{playerId},#{mapId},#{x},#{y},#{z},#{heading})")
	int replaceBindPoint(@Param("playerId") int playerId, @Param("mapId") int mapId, @Param("x") float x,
		@Param("y") float y, @Param("z") float z, @Param("heading") byte heading);
}
