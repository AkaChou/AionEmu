package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.WebRewardPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 web_reward 的映射层。
 * Mapper for table web_reward.
 *
 * <p>领取标记写入数据库函数 NOW()，无法用 Flex CRUD 表达，故保留注解 SQL；`unique` 为 MySQL 8 保留字。
 * The claim marker writes the NOW() database function, which Flex CRUD cannot express, so the
 * statement is kept as annotated SQL; `unique` is a MySQL 8 reserved word.
 */
public interface WebRewardMapper extends BaseMapper<WebRewardPo> {

	/**
	 * 标记奖励已领取并写入领取时间（原 UPDATE_QUERY）。
	 * Marks the reward as received and stamps the receive time (formerly UPDATE_QUERY).
	 *
	 * @param rewarded 已领取标志 / received flag
	 * @param unique 唯一条目 ID / unique entry id
	 * @return 受影响行数 / affected rows
	 */
	@Update("UPDATE `web_reward` SET `rewarded` = #{rewarded}, received = NOW() WHERE `unique` = #{unique}")
	int updateReceived(@Param("rewarded") int rewarded, @Param("unique") int unique);
}
