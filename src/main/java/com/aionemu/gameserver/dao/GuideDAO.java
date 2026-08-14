package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.guide.Guide;

/**
 * 新手引导数据访问对象。
 * Guide data access object.
 *
 * @author xTz
 */
public abstract class GuideDAO implements IDFactoryAwareDAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public final String getClassName() {
		return GuideDAO.class.getName();
	}

	/**
	 * 删除指定引导。
	 * Deletes a guide by ID.
	 *
	 * @param guide_id 向导 ID / guide ID
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean deleteGuide(int guide_id);

	/**
	 * 加载玩家的全部引导。
	 * Loads all guides for a player.
	 *
	 * @param playerId 玩家 ID / player ID
	 * @return 向导列表 / guide list
	 */
	public abstract List<Guide> loadGuides(int playerId);

	/**
	 * 加载指定玩家的指定引导。
	 * Loads a specific guide for a player.
	 *
	 * @param player_id 玩家 ID / player ID
	 * @param guide_id 向导 ID / guide ID
	 * @return 向导 / guide
	 */
	public abstract Guide loadGuide(int player_id, int guide_id);

	/**
	 * 保存引导记录。
	 * Saves a guide record.
	 *
	 * @param guide_id 向导 ID / guide ID
	 * @param player 玩家 / player
	 * @param title 称号 / title
	 */
	public abstract void saveGuide(int guide_id, Player player, String title);
}
