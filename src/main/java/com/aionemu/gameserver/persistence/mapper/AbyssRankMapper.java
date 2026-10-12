package com.aionemu.gameserver.persistence.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.AbyssRankPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 abyss_rank 的映射层。
 * Mapper for table abyss_rank.
 *
 * <p>玩家/军团排行查询跨 players、legions、legion_members 三张表（归属其它闭包），且依赖
 * MySQL 的「表名.列名」合格列标签，无法用本表实体的 Flex CRUD 表达，故以注解 SQL 逐字保留。
 * Player/legion ranking queries span players, legions and legion_members (owned by other
 * closures) and rely on MySQL table-qualified column labels, so they are kept verbatim as
 * annotated SQL.
 */
public interface AbyssRankMapper extends BaseMapper<AbyssRankPo> {

	/**
	 * 按玩家 ID 查询欧比斯军阶（原 SELECT_QUERY）。
	 * 列含 MySQL 8 保留字 `rank`，Flex 实体的结果映射会原样使用带反引号的列名（JDBC findColumn 不匹配），
	 * 故以 Map 接收、逐列取值，语句逐字保留。
	 * Selects the abyss rank by player id (formerly SELECT_QUERY). The projection contains the
	 * MySQL 8 reserved word `rank`, and Flex's entity ResultMap would pass the backticked column
	 * name straight to JDBC findColumn (which does not match), so the row is read as a Map.
	 */
	@Select("SELECT daily_ap, daily_gp, weekly_ap, weekly_gp, ap, gp, `rank`, "
		+ "top_ranking, daily_kill, weekly_kill, all_kill, max_rank, last_kill, "
		+ "last_ap, last_gp, last_update FROM abyss_rank WHERE player_id = #{playerId}")
	Map<String, Object> selectRankByPlayerId(@Param("playerId") int playerId);

	/**
	 * 更新玩家军阶与名额（原 UPDATE_RANK，无 WHERE 之外的复杂语义，逐字保留）。
	 * Updates the player's rank and top-ranking quota (formerly UPDATE_RANK, kept verbatim).
	 */
	@Update("UPDATE abyss_rank SET `rank` = #{rank}, top_ranking = #{topRanking} WHERE player_id = #{playerId}")
	int updateRankAndQuota(@Param("rank") int rank, @Param("topRanking") int topRanking,
		@Param("playerId") int playerId);

	/**
	 * 阵营玩家欧比斯排行（原 SELECT_PLAYERS_RANKING）。
	 * 仅将重名的 players.name / legions.name 加别名以消除列标签冲突，其余语句逐字保留。
	 * Player abyss ranking for a race (formerly SELECT_PLAYERS_RANKING). Only the duplicated
	 * players.name / legions.name labels gained aliases; the rest of the statement is verbatim.
	 */
	@Select("SELECT abyss_rank.`rank`, abyss_rank.ap, abyss_rank.gp, abyss_rank.old_rank_pos, "
		+ "abyss_rank.rank_pos, players.name AS player_name, legions.name AS legion_name, players.id, "
		+ "players.title_id, players.player_class, players.exp FROM abyss_rank INNER JOIN players "
		+ "ON abyss_rank.player_id = players.id LEFT JOIN legion_members "
		+ "ON legion_members.player_id = players.id LEFT JOIN legions "
		+ "ON legions.id = legion_members.legion_id WHERE players.race = #{race} "
		+ "AND abyss_rank.gp > 1243 ORDER BY abyss_rank.gp DESC LIMIT 300")
	List<Map<String, Object>> selectPlayerRanking(@Param("race") String race);

	/**
	 * 阵营军团欧比斯排行（原 SELECT_LEGIONS_RANKING）。
	 * Legion abyss ranking for a race (formerly SELECT_LEGIONS_RANKING).
	 */
	@Select("SELECT legions.id, legions.name, legions.contribution_points, "
		+ "legions.level as lvl, legions.old_rank_pos, legions.rank_pos "
		+ "FROM legions, legion_members, players WHERE players.race = #{race} "
		+ "AND legion_members.`rank` = 'BRIGADE_GENERAL' AND legion_members.player_id = players.id "
		+ "AND legion_members.legion_id = legions.id AND legions.contribution_points > 0 "
		+ "GROUP BY id ORDER BY legions.contribution_points DESC LIMIT 50")
	List<Map<String, Object>> selectLegionRanking(@Param("race") String race);

	/**
	 * 统计军团成员数（原 SELECT_LEGION_COUNT）。
	 * Counts the members of a legion (formerly SELECT_LEGION_COUNT).
	 */
	@Select("SELECT COUNT(player_id) as players FROM legion_members WHERE legion_id = #{legionId}")
	Integer countLegionMembers(@Param("legionId") int legionId);

	/**
	 * 阵营玩家 GP 映射（原 SELECT_GP_PLAYER）。
	 * Player GP map for a race (formerly SELECT_GP_PLAYER).
	 */
	@Select("SELECT player_id, gp FROM abyss_rank, players WHERE "
		+ "abyss_rank.player_id = players.id AND players.race = #{race} AND gp > #{lowerGpLimit} "
		+ "ORDER BY gp DESC")
	List<Map<String, Object>> selectPlayerGp(@Param("race") String race, @Param("lowerGpLimit") int lowerGpLimit);

	/**
	 * 阵营活跃玩家 GP 映射（原 SELECT_GP_PLAYER_ACTIVE_ONLY）。
	 * Active-player GP map for a race (formerly SELECT_GP_PLAYER_ACTIVE_ONLY).
	 */
	@Select("SELECT player_id, gp FROM abyss_rank, players WHERE "
		+ "abyss_rank.player_id = players.id AND players.race = #{race} AND gp > #{lowerGpLimit} "
		+ "AND UNIX_TIMESTAMP(CURDATE()) - UNIX_TIMESTAMP(players.last_online) <= #{maxOfflineDays} * 24 * 60 * 60 "
		+ "ORDER BY gp DESC")
	List<Map<String, Object>> selectActivePlayerGp(@Param("race") String race, @Param("lowerGpLimit") int lowerGpLimit,
		@Param("maxOfflineDays") int maxOfflineDays);

	/**
	 * 插入军阶记录；经 storeInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Insert statement; executed by raw PreparedStatement inside storeInTransaction, so `?` is kept.
	 */
	String INSERT_QUERY_JDBC = "INSERT INTO abyss_rank (player_id, daily_ap, daily_gp, weekly_ap, "
		+ "weekly_gp, ap, gp, `rank`, top_ranking, daily_kill, weekly_kill, "
		+ "all_kill, max_rank, last_kill, last_ap, last_gp, last_update) "
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 更新军阶记录；经 storeInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Update statement; executed by raw PreparedStatement inside storeInTransaction, so `?` is kept.
	 */
	String UPDATE_QUERY_JDBC = "UPDATE abyss_rank SET daily_ap = ?, daily_gp = ?, weekly_ap = ?, "
		+ "weekly_gp = ?, ap = ?, gp = ?, `rank` = ?, top_ranking = ?, daily_kill = ?, "
		+ "weekly_kill = ?, all_kill = ?, max_rank = ?, last_kill = ?, last_ap = ?, "
		+ "last_gp = ?, last_update = ? WHERE player_id = ?";

	/**
	 * 按 GP 重算玩家排行位置；依赖会话变量与无 WHERE 的 UPDATE，经原生语句执行，保留 ? 原文。
	 * Recalculates player rank positions by GP; uses a session variable and a WHERE-less UPDATE,
	 * executed as a raw statement, so `?` is kept.
	 */
	String UPDATE_PLAYER_RANK_LIST_JDBC = "UPDATE abyss_rank SET abyss_rank.old_rank_pos = abyss_rank.rank_pos, "
		+ "abyss_rank.rank_pos = @a:=@a+1 WHERE player_id IN "
		+ "(SELECT id FROM players WHERE race = ?) ORDER BY gp DESC ";

	/**
	 * 按贡献点重算军团排行位置；依赖会话变量与无 WHERE 的 UPDATE，经原生语句执行，保留 ? 原文。
	 * Recalculates legion rank positions by contribution points; uses a session variable and a
	 * WHERE-less UPDATE, executed as a raw statement, so `?` is kept.
	 */
	String UPDATE_LEGION_RANK_LIST_JDBC = "UPDATE legions SET legions.old_rank_pos = legions.rank_pos, "
		+ "legions.rank_pos = @a:=@a+1 WHERE id IN "
		+ "(SELECT legion_id FROM legion_members, players WHERE legion_members.`rank` = 'BRIGADE_GENERAL' "
		+ "AND players.id = legion_members.player_id AND players.race = ?) "
		+ "ORDER BY legions.contribution_points DESC ";
}
