package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerSettingPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家 UI/客户端设置数据访问层。
 * Data access layer for player UI/client settings persistence.
 * @author ATracer
 */
public interface PlayerSettingDao extends IService<PlayerSettingPo> {

	/**
	 * 保存玩家设置。
	 * Saves player settings.
	 * @param player 玩家 / player
	 */
	void saveSettings(Player player);

	/**
	 * 加载玩家设置。
	 * Loads player settings.
	 * @param player 玩家 / player
	 */
	void loadSettings(Player player);
}
