package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.FriendPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 friends 的映射层。
 * Mapper for table friends.
 */
public interface FriendMapper extends BaseMapper<FriendPo> {

	/**
	 * 设置好友备注（保留原 SQL 原文；该列在现行 DDL 中不存在，行为与原实现一致）。
	 * Sets a friend note (verbatim legacy SQL; the column is absent from the current DDL, matching the original behavior).
	 * @param playerId 玩家 ID / player id
	 * @param friendId 好友 ID / friend id
	 * @param note 备注 / note
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE `friends` SET `note` = #{note} WHERE `player` = #{playerId} AND `friend` = #{friendId}")
	int updateNote(@Param("playerId") int playerId, @Param("friendId") int friendId, @Param("note") String note);
}
