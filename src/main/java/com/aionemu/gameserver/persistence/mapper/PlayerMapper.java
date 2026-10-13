package com.aionemu.gameserver.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.PlayerPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 players 的映射层。
 * Mapper for table players.
 * <p>
 * {@code setAllOnline} 为无 WHERE 的全表更新（Flex 运行期拒绝），保留注解 SQL；
 * 计数与删除查询已由 DAO 用 Flex 查询链表达。
 * {@code setAllOnline} is a WHERE-less whole-table update (rejected by Flex at runtime), so it stays annotated;
 * the count and delete queries are expressed with the Flex query chain in the DAO.
 * </p>
 */
public interface PlayerMapper extends BaseMapper<PlayerPo> {

	/**
	 * 批量重置全部角色在线状态（无 WHERE，Flex 运行期拒绝）。
	 * Resets the online flag of every player (no WHERE, rejected by Flex at runtime).
	 *
	 * @param online 目标在线状态 / target online flag
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE players SET online = #{online}")
	int setAllOnline(@Param("online") boolean online);
}

