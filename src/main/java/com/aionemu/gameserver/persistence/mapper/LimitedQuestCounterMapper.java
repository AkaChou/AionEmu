package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.LimitedQuestCounterPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 limited_quest_counters 的映射层。
 * Mapper for table limited_quest_counters.
 * <p>
 * 三条语句均为方言 SQL（{@code INSERT IGNORE}、列自减、{@code LEAST}）。
 * All three statements are dialect SQL ({@code INSERT IGNORE}, column self-decrement, {@code LEAST}); only
 * {@code ?} was turned into named parameters.
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

	/**
	 * 原子扣减一个名额，名额耗尽时为 0 行。
	 * Atomically decrements one slot; zero rows when the quota is exhausted.
	 * @param questId 任务 ID / quest id
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE `limited_quest_counters` SET `remaining` = `remaining` - 1 "
		+ "WHERE `quest_id` = #{questId} AND `remaining` > 0")
	int acquire(@Param("questId") int questId);

	/**
	 * 恢复名额，但不超过上限。
	 * Restores slots, never above the cap.
	 * @param amount 恢复数量 / restores amount
	 * @param maxCount 名额上限 / quota cap
	 * @param questId 任务 ID / quest id
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE `limited_quest_counters` SET `remaining` = LEAST(`remaining` + #{amount}, #{maxCount}) "
		+ "WHERE `quest_id` = #{questId}")
	int recover(@Param("amount") int amount, @Param("maxCount") int maxCount, @Param("questId") int questId);
}
