package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 自动副本处理器。
 * Auto Instance Handler interface.
 */

public interface AutoInstanceHandler {
	/** 初始化 / Initialize. */
	void initsialize(int instanceMaskId);

	/** 副本创建 / On Instance Create*/
	void onInstanceCreate(WorldMapInstance instance);

	/** 添加玩家。 / Adds player. */
	AGQuestion addPlayer(Player player, SearchInstance searchInstance);

	/** 进入副本 / On Enter Instance*/
	void onEnterInstance(Player player);

	/** 离开副本 / On Leave Instance*/
	void onLeaveInstance(Player player);

	/** 按下回车时 / on Press Enter. */
	void onPressEnter(Player player);

	/** 注销。 / Unregister. */
	void unregister(Player player);

	/** 清空。 / Clear. */
	void clear();
}
