package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerNpcFactionPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家 NPC 势力声望数据访问对象。
 * Player NPC-factions data access object.
 * @author MrPoke
 */
public interface PlayerNpcFactionDao extends IService<PlayerNpcFactionPo> {

	/**
	 * 加载玩家 NPC 势力数据。
	 * Loads player NPC-faction data.
	 * @param player 玩家 / player
	 */
	void loadNpcFactions(Player player);

	/**
	 * 存储玩家 NPC 势力数据。
	 * Stores player NPC-faction data.
	 * @param player 玩家 / player
	 */
	void storeNpcFactions(Player player);
}
