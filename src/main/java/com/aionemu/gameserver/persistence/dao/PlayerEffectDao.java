package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerEffectPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家效果（Buff 等）数据访问层。
 * Data access layer for player effects (buffs etc.).
 * @author ATracer
 */
public interface PlayerEffectDao extends IService<PlayerEffectPo> {

	/**
	 * 加载玩家效果数据。
	 * Loads player effect data.
	 * @param player 玩家 / player
	 */
	void loadPlayerEffects(Player player);

	/**
	 * 存储玩家效果数据。
	 * Stores player effect data.
	 * @param player 玩家 / player
	 */
	void storePlayerEffects(Player player);
}
