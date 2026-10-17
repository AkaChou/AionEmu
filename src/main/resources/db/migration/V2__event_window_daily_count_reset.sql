-- EventWindow（在线时长图章）每日领取计数按自然日重置：
-- count_date 记录 reward_recived_count 的归属日期，跨自然日读取时计数视为 0。
-- Daily reset for the EventWindow reward counter: count_date carries the calendar day
-- the counter belongs to; reads on a later day treat the count as 0.
ALTER TABLE `player_events_window`
  ADD COLUMN `count_date` date NULL DEFAULT NULL AFTER `reward_recived_count`;
