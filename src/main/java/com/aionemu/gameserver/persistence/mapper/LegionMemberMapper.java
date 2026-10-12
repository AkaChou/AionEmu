package com.aionemu.gameserver.persistence.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aionemu.gameserver.persistence.entity.LegionMemberPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 legion_members 的映射层。
 * Mapper for table legion_members.
 */
public interface LegionMemberMapper extends BaseMapper<LegionMemberPo> {

	/**
	 * 按玩家 ID 联表加载军团成员扩展信息（players + legion_members）。
	 * Loads extended legion member info by player id (players joined with legion_members).
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 列名到值的单行映射，无记录为 null / single-row map of column name to value, or null if none
	 */
	@Select("SELECT players.name AS name, players.exp AS exp, players.player_class AS player_class, "
		+ "players.last_online AS last_online, players.world_id AS world_id, "
		+ "legion_members.legion_id AS legion_id, legion_members.`rank` AS `rank`, "
		+ "legion_members.nickname AS nickname, legion_members.selfintro AS selfintro "
		+ "FROM players, legion_members "
		+ "WHERE players.id = #{playerObjId} AND players.id = legion_members.player_id")
	Map<String, Object> selectLegionMemberExById(@Param("playerObjId") int playerObjId);

	/**
	 * 按玩家名联表加载军团成员扩展信息（players + legion_members）。
	 * Loads extended legion member info by player name (players joined with legion_members).
	 *
	 * @param playerName 玩家名 / player name
	 * @return 列名到值的单行映射，无记录为 null / single-row map of column name to value, or null if none
	 */
	@Select("SELECT players.id AS id, players.exp AS exp, players.player_class AS player_class, "
		+ "players.last_online AS last_online, players.world_id AS world_id, "
		+ "legion_members.legion_id AS legion_id, legion_members.`rank` AS `rank`, "
		+ "legion_members.nickname AS nickname, legion_members.selfintro AS selfintro "
		+ "FROM players, legion_members "
		+ "WHERE players.name = #{playerName} AND players.id = legion_members.player_id")
	Map<String, Object> selectLegionMemberExByName(@Param("playerName") String playerName);
}
