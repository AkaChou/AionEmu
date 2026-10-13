package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.LimitedQuestCounterPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 limited_quest_counters 的映射层。
 * Mapper for table limited_quest_counters.
 * <p>
 * {@code initialize} 依赖 {@code INSERT IGNORE}，Flex 1.11.8 无 SQL 级 upsert，故逐字保留；
 * {@code acquire}/{@code recover} 的列自减与 LEAST 已由 DAO 用 Flex 更新链表达。
 * {@code initialize} relies on {@code INSERT IGNORE}, which Flex 1.11.8 cannot express (no SQL-level upsert),
 * so it is kept; the column self-decrement and LEAST of {@code acquire}/{@code recover} are expressed with the
 * Flex update chain in the DAO.
 * </p>
 */
public interface LimitedQuestCounterMapper extends BaseMapper<LimitedQuestCounterPo> {

	/**
	 * 初始化名额，已存在时忽略。
	 * Initializes the quota, ignored when the row already exists.
	 * @param questId 任务 ID / quest id
	 * @param maxCount 名额上限 / quota cap
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT IGNORE INTO `limited_quest_counters` (`quest_id`, `remaining`) VALUES (#{questId}, #{maxCount})")
	int initialize(@Param("questId") int questId, @Param("maxCount") int maxCount);
}
