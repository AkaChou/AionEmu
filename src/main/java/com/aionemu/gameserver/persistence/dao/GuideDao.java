package com.aionemu.gameserver.persistence.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.guide.Guide;
import com.aionemu.gameserver.persistence.entity.GuidePo;
import com.mybatisflex.core.service.IService;

/**
 * 新手引导数据访问对象。
 * Guide data access object.
 * @author xTz
 */
public interface GuideDao extends IService<GuidePo> {

	/**
	 * 删除指定引导。
	 * Deletes a guide by ID.
	 * @param guide_id 向导 ID / guide ID
	 */
	void deleteGuide(int guide_id);

	/**
	 * 加载玩家的全部引导。
	 * Loads all guides for a player.
	 * @param playerId 玩家 ID / player ID
	 * @return 向导列表 / guide list
	 */
	List<Guide> loadGuides(int playerId);

	/**
	 * 加载指定玩家的指定引导。
	 * Loads a specific guide for a player.
	 * @param player_id 玩家 ID / player ID
	 * @param guide_id 向导 ID / guide ID
	 * @return 向导 / guide
	 */
	Guide loadGuide(int player_id, int guide_id);

	/**
	 * 保存引导记录。
	 * Saves a guide record.
	 * @param guide_id 向导 ID / guide ID
	 * @param player 玩家 / player
	 * @param title 称号 / title
	 */
	void saveGuide(int guide_id, Player player, String title);

	/**
	 * 返回库内已使用的引导 ID 集合。
	 * Returns the guide ids already used in the database.
	 * @return 已用 ID 数组 / used id array
	 */
	int[] getUsedIDs();
}
