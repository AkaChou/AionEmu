package com.aionemu.gameserver.persistence.mapper;

import java.sql.Timestamp;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.PlayerEventsWindowPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_events_window 的映射层。
 * Mapper for table player_events_window.
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

	/**
	 * 设置已领取奖励次数并重置进度（保留库内 NOW() 求值）。
	 * Sets the reward-received count and resets progress (keeping in-database NOW() evaluation).
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @param rewardRecivedCount 已领取奖励次数 / reward-received count
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE `player_events_window` SET `reward_recived_count` = #{rewardRecivedCount}, `elapsed` = 0, "
		+ "`last_stamp` = NOW() WHERE `account_id` = #{accountId} AND `event_id` = #{eventId}")
	int updateReward(@Param("accountId") int accountId, @Param("eventId") int eventId,
		@Param("rewardRecivedCount") int rewardRecivedCount);
}
