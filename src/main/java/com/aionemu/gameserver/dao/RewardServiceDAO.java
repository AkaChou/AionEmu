package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;

/**
 * 奖励服务数据访问抽象层。
 * DAO for external/reward-service entry item persistence.
 */
public abstract class RewardServiceDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return RewardServiceDAO.class.getName();
	}

	/**
	 * 查询玩家可领取的奖励条目。
	 * Returns available reward entries for the player.
	 *
	 * player object id
	 * @return 可领取奖励列表 / available reward entry list
	 */
	public abstract List<RewardEntryItem> getAvailable(int playerId);

	/**
	 * 将指定奖励标记为已处理/不可再领。
	 * Marks the given reward ids as unchecked/unavailable.
	 *
	 * @param ids 奖励条目 ID 列表 / reward entry id list
	 */
	public abstract void uncheckAvailable(List<Integer> ids);

	/**
	 * 将奖励下载状态置为 down。
	 * Sets the update/download flag to down for the entry.
	 *
	 * unique entry id
	 */
	public abstract void setUpdateDown(int unique);

	/**
	 * 更新奖励条目状态。
	 * Updates the reward entry status.
	 *
	 * unique entry id
	 * @return 是否更新成功 / true if updated
	 */
	public abstract boolean setUpdate(int unique);
}
