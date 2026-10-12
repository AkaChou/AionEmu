package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.persistence.entity.PlayerSkillSkinPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家技能皮肤列表数据访问层。
 * Data access layer for player skill skin list persistence.
 * @author Rinzler (Encom)
 */
public interface PlayerSkillSkinDao extends IService<PlayerSkillSkinPo> {

	/**
	 * 加载玩家技能皮肤列表。
	 * Loads the skill skin list for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 技能皮肤列表 / skill skin list
	 */
	SkillSkinList loadSkillSkinList(int playerId);

	/**
	 * 保存玩家一条技能皮肤记录。
	 * Stores a skill skin entry for the player.
	 * @param player 玩家 / player
	 * @param entry 技能皮肤条目 / skill skin entry
	 */
	void storeSkillSkins(Player player, SkillSkin entry);

	/**
	 * 移除玩家一条技能皮肤。
	 * Removes a skill skin from the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param skinId 皮肤 ID / skin id
	 */
	void removeSkillSkin(int playerId, int skinId);

	/**
	 * 激活玩家指定技能皮肤。
	 * Activates a skill skin for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param skinId 皮肤 ID / skin id
	 * @return 是否激活成功 / true if activated
	 */
	boolean setActive(int playerId, int skinId);

	/**
	 * 取消激活玩家指定技能皮肤。
	 * Deactivates a skill skin for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param skinId 皮肤 ID / skin id
	 */
	void setDeactive(int playerId, int skinId);
}
