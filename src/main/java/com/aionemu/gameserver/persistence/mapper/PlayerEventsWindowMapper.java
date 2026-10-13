package com.aionemu.gameserver.persistence.mapper;

import java.sql.Timestamp;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.PlayerEventsWindowPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_events_window 的映射层。
 * Mapper for table player_events_window.
 * <p>
 * {@code upsert} 的「插入全集、冲突只回写子集」语义（插入写 elapsed、重复时不回写 elapsed）无法用 Flex 1.11.8 表达：
 * 该版本无 SQL 级 upsert，{@code insertOrUpdate} 是「主键有值即 UPDATE」的 Java 分支，会丢失插入。
 * The "insert all columns, on duplicate overwrite only a subset" semantics (insert writes elapsed, duplicate does not)
 * cannot be expressed with Flex 1.11.8: it has no SQL-level upsert, and {@code insertOrUpdate} is a Java branch that
 * runs UPDATE whenever the primary key is set, losing the insert.
 * </p>
 */
public interface PlayerEventsWindowMapper extends BaseMapper<PlayerEventsWindowPo> {

	/**
	 * 插入或更新活动窗口（MySQL upsert 方言，重复时仅回写事件 ID 与时间戳）。
	 * Inserts or updates an event window (MySQL upsert dialect). On duplicate, only the event id and the timestamp
	 * are written back.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @param lastStamp 最后时间戳 / last stamp
	 * @param elapsed 已用时间 / elapsed time
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `player_events_window` (`account_id`, `event_id`, `last_stamp`, `elapsed`) "
		+ "VALUES(#{accountId},#{eventId},#{lastStamp},#{elapsed}) "
		+ "ON DUPLICATE KEY UPDATE `event_id` = VALUES(`event_id`), `last_stamp` = VALUES(`last_stamp`)")
	int upsert(@Param("accountId") int accountId, @Param("eventId") int eventId,
		@Param("lastStamp") Timestamp lastStamp, @Param("elapsed") int elapsed);
}
