package com.aionemu.gameserver.persistence.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aionemu.gameserver.persistence.entity.CompetitionRankingPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 competition_ranking 的映射层。
 * Mapper for table competition_ranking.
 *
 * <p>赛季排行跨 players 表 JOIN 且依赖 MySQL 的「表名.列名」合格列标签，无法用本表实体的
 * Flex CRUD 表达，故以注解 SQL 保留；`rank` 为 MySQL 8 保留字，列引用加反引号。
 * Season ranking joins players and relies on MySQL table-qualified column labels, so it is kept
 * as annotated SQL; `rank` is a MySQL 8 reserved word, hence the backticks.
 */
public interface CompetitionRankingMapper extends BaseMapper<CompetitionRankingPo> {

	/**
	 * 指定排行表的前 300 名竞技排行玩家（原 SELECT_PLAYERS_RANKING）。
	 * Top 300 competition ranking players for the given table (formerly SELECT_PLAYERS_RANKING).
	 *
	 * @param tableId 排行表 ID / ranking table id
	 * @return 排行行投影，键为列标签 / ranking rows keyed by column label
	 */
	@Select("SELECT competition_ranking.`rank`, competition_ranking.last_rank, "
		+ "competition_ranking.points, competition_ranking.player_id, players.name, "
		+ "players.id, players.player_class, players.race FROM competition_ranking "
		+ "INNER JOIN players ON competition_ranking.player_id = players.id "
		+ "WHERE competition_ranking.table_id = #{tableId} AND competition_ranking.points > 0 "
		+ "ORDER BY competition_ranking.points DESC LIMIT 300")
	List<Map<String, Object>> selectCompetitionRankingPlayers(@Param("tableId") int tableId);
}
