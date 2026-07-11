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
	 * class name
	 */
	@Override
	public final String getClassName() {
		return GuideDAO.class.getName();
	}

	/**
	 * 删除指定引导。
	 * Deletes a guide by ID.
	 *
	 * guide ID
	 * whether successful
	 */
	public abstract boolean deleteGuide(int guide_id);

	/**
	 * 加载玩家的全部引导。
	 * Loads all guides for a player.
	 *
	 * player ID
	 * guide list
	 */
	public abstract List<Guide> loadGuides(int playerId);

	/**
	 * 加载指定玩家的指定引导。
	 * Loads a specific guide for a player.
	 *
	 * player ID
	 * guide ID
	 * guide
	 */
	public abstract Guide loadGuide(int player_id, int guide_id);

	/**
	 * 保存引导记录。
	 * Saves a guide record.
	 *
	 * guide ID
	 * 玩家 / player
	 * title
	 */
	public abstract void saveGuide(int guide_id, Player player, String title);
}
