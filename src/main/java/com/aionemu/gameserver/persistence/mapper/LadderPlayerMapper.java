package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.LadderPlayerPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 ladder_player 的映射层。
 * Mapper for table ladder_player.
 *
 * <p>天梯加分的目标列由分数类型决定，且依赖 INSERT ... ON DUPLICATE KEY UPDATE，
 * 无法用同一实体的 Flex CRUD 表达，故按列各保留一条注解 SQL（仅 ? → #{name}）。
 * The ladder increment targets a caller-chosen column and relies on INSERT ... ON DUPLICATE KEY
 * UPDATE, so each column keeps its own annotated statement (only `?` became #{name}).
 */
public interface LadderPlayerMapper extends BaseMapper<LadderPlayerPo> {

	/**
	 * 累加胜场（原 addPlayerLadderData 的 wins 分支）。
	 * Adds wins (the wins branch of the former addPlayerLadderData).
	 */
	@Insert("INSERT INTO ladder_player (player_id, wins) VALUES (#{playerId}, #{value}) "
		+ "ON DUPLICATE KEY UPDATE wins = wins + #{value}")
	void addWins(@Param("playerId") int playerId, @Param("value") int value);

	/**
	 * 累加负场（原 addPlayerLadderData 的 losses 分支）。
	 * Adds losses (the losses branch of the former addPlayerLadderData).
	 */
	@Insert("INSERT INTO ladder_player (player_id, losses) VALUES (#{playerId}, #{value}) "
		+ "ON DUPLICATE KEY UPDATE losses = losses + #{value}")
	void addLosses(@Param("playerId") int playerId, @Param("value") int value);

	/**
	 * 累加离场次数（原 addPlayerLadderData 的 leaves 分支）。
	 * Adds leaves (the leaves branch of the former addPlayerLadderData).
	 */
	@Insert("INSERT INTO ladder_player (player_id, leaves) VALUES (#{playerId}, #{value}) "
		+ "ON DUPLICATE KEY UPDATE leaves = leaves + #{value}")
	void addLeaves(@Param("playerId") int playerId, @Param("value") int value);

	/**
	 * 累加评分；首次插入以 initial（1000 + 增量）起算（原 addPlayerLadderData 的 rating 分支）。
	 * Adds rating; the first insert starts from initial (1000 + delta)
	 * (the rating branch of the former addPlayerLadderData).
	 */
	@Insert("INSERT INTO ladder_player (player_id, rating) VALUES (#{playerId}, #{initial}) "
		+ "ON DUPLICATE KEY UPDATE rating = rating + #{delta}")
	void addRating(@Param("playerId") int playerId, @Param("initial") int initial, @Param("delta") int delta);
}
