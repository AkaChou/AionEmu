package com.aionemu.gameserver.persistence.dao;

import java.sql.Timestamp;
import java.util.List;

import com.aionemu.gameserver.model.event_window.PlayerEventWindowList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerEventsWindowPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家活动窗口数据访问层。
 * Player events-window data access layer.
 * @author Ghostfur (Aion-Unique)
 */
public interface PlayerEventsWindowDao extends IService<PlayerEventsWindowPo> {

	/**
	 * 加载玩家活动窗口列表。
	 * Loads the player's events-window list.
	 * @param player 玩家 / player
	 * @return 活动窗口列表 / events-window list
	 */
	PlayerEventWindowList load(Player player);

	/**
	 * 插入一条活动窗口记录。
	 * Inserts an events-window record.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @param lastStamp 最后时间戳 / last stamp
	 */
	void insert(int accountId, int eventId, Timestamp lastStamp);

	/**
	 * 存储活动窗口进度。
	 * Stores events-window progress.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @param lastStamp 最后时间戳 / last stamp
	 * @param elapsed 已用时间 / elapsed time
	 * @return 若成功则为 true / true if successful
	 */
	boolean store(int accountId, int eventId, Timestamp lastStamp, int elapsed);

	/**
	 * 删除活动窗口记录。
	 * Deletes an events-window record.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 */
	void delete(int accountId, int eventId);

	/**
	 * 获取活动最后时间戳。
	 * Returns the last stamp for the event.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @return 最后时间戳 / last stamp
	 */
	Timestamp getLastStamp(int accountId, int eventId);

	/**
	 * 获取活动已用时间。
	 * Returns elapsed time for the event.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @return 已用时间 / elapsed time
	 */
	int getElapsed(int accountId, int eventId);

	/**
	 * 更新活动已用时间。
	 * Updates elapsed time for the event.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @param elapsed 已用时间 / elapsed time
	 */
	void updateElapsed(int accountId, int eventId, int elapsed);

	/**
	 * 获取已领取奖励次数。
	 * Returns the reward-received count.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @return 已领取奖励次数 / reward received count
	 */
	int getRewardRecivedCount(int accountId, int eventId);

	/**
	 * 设置已领取奖励次数。
	 * Sets the reward-received count.
	 * @param accountId 账号 ID / account id
	 * @param eventId 事件 ID / event id
	 * @param rewardRecivedCount 已领取奖励次数 / reward received count
	 */
	void setRewardRecivedCount(int accountId, int eventId, int rewardRecivedCount);

	/**
	 * 获取账号下全部活动窗口 ID。
	 * Returns all events-window IDs for the account.
	 * @param accountId 账号 ID / account id
	 * @return 事件 ID 列表 / list of event ids
	 */
	List<Integer> getEventsWindow(int accountId);
}
