package com.aionemu.gameserver.persistence.dao;

import java.util.List;

import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import com.aionemu.gameserver.persistence.entity.WebRewardPo;
import com.mybatisflex.core.service.IService;

/**
 * 奖励服务数据访问抽象层。
 * DAO for external/reward-service entry item persistence.
 */
public interface WebRewardDao extends IService<WebRewardPo> {

	/**
	 * 查询玩家可领取的奖励条目。
	 * Returns available reward entries for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 可领取奖励列表 / available reward entry list
	 */
	List<RewardEntryItem> getAvailable(int playerId);

	/**
	 * 将指定奖励标记为已处理/不可再领。
	 * Marks the given reward ids as unchecked/unavailable.
	 * @param ids 奖励条目 ID 列表 / reward entry id list
	 */
	void uncheckAvailable(List<Integer> ids);

	/**
	 * 将奖励下载状态置为 down。
	 * Sets the update/download flag to down for the entry.
	 * @param unique 唯一条目 ID / unique entry id
	 */
	void setUpdateDown(int unique);

	/**
	 * 更新奖励条目状态。
	 * Updates the reward entry status.
	 * @param unique 唯一条目 ID / unique entry id
	 * @return 是否更新成功 / true if updated
	 */
	boolean setUpdate(int unique);
}
