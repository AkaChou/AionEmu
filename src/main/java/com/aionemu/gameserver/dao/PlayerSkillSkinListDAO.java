package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;

/**
 * 玩家技能皮肤列表数据访问抽象层。
 * DAO for player skill skin list persistence.
 *
 * @author Rinzler (Encom)
 */
public abstract class PlayerSkillSkinListDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerSkillSkinListDAO.class.getName();
	}

	/**
	 * 加载玩家技能皮肤列表。
	 * Loads the skill skin list for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @return 技能皮肤列表 / skill skin list
	 */
	public abstract SkillSkinList loadSkillSkinList(int playerId);

	/**
	 * 保存玩家一条技能皮肤记录。
	 * Stores a skill skin entry for the player.
	 *
	 * @param player 玩家 / player
	 * @param entry 技能皮肤条目 / skill skin entry
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeSkillSkins(Player player, SkillSkin entry);

	/**
	 * 移除玩家一条技能皮肤。
	 * Removes a skill skin from the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param skinId 皮肤 ID / skin id
	 * @return 是否移除成功 / true if removed
	 */
	public abstract boolean removeSkillSkin(int playerId, int skinId);

	/**
	 * 激活玩家指定技能皮肤。
	 * Activates a skill skin for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param skinId 皮肤 ID / skin id
	 * @return 是否激活成功 / true if activated
	 */
	public abstract boolean setActive(int playerId, int skinId);

	/**
	 * 取消激活玩家指定技能皮肤。
	 * Deactivates a skill skin for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param skinId 皮肤 ID / skin id
	 * @return 是否取消成功 / true if deactivated
	 */
	public abstract boolean setDeactive(int playerId, int skinId);
}
