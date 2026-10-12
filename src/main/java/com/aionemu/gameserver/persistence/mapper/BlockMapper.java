package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.BlockPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 blocks 的映射层。
 * Mapper for table blocks.
 */
public interface BlockMapper extends BaseMapper<BlockPo> {

	/**
	 * 更新屏蔽原因。
	 * Updates the block reason.
	 * @param playerId 玩家对象 ID / player object id
	 * @param blockedPlayerObjId 被屏蔽对象 ID / blocked object id
	 * @param reason 屏蔽原因 / block reason
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE blocks SET reason=#{reason} WHERE player=#{playerId} AND blocked_player=#{blockedPlayerObjId}")
	int updateReason(@Param("playerId") int playerId, @Param("blockedPlayerObjId") int blockedPlayerObjId,
		@Param("reason") String reason);
}
