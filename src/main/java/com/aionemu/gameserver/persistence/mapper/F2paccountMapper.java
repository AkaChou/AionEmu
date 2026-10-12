package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.F2paccountPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 f2paccount 的映射层。
 * Mapper for table f2paccount.
 */
public interface F2paccountMapper extends BaseMapper<F2paccountPo> {

	/**
	 * 插入或更新玩家 F2P 时间（MySQL upsert 方言，保留原语义）。
	 * Inserts or updates player F2P time (MySQL upsert dialect, semantics preserved).
	 * @param playerId 玩家对象 ID / player object id
	 * @param time F2P 剩余时间 / F2P remaining time
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `f2paccount` (`player_id`, `time`) "
		+ "VALUES (#{playerId}, #{time}) "
		+ "ON DUPLICATE KEY UPDATE `time` = VALUES(`time`)")
	int upsert(@Param("playerId") int playerId, @Param("time") int time);
}
