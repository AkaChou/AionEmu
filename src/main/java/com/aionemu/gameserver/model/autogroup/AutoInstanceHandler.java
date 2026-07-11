package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 自动副本处理器。
 * Auto Instance Handler interface.
 */

public interface AutoInstanceHandler {
	/** 初始化 / initsialize. */
	public abstract void initsialize(int instanceMaskId);

	/** 副本创建 / On Instance Create*/
	public abstract void onInstanceCreate(WorldMapInstance instance);

	/** 添加玩家。 / Adds player. */
	public abstract AGQuestion addPlayer(Player player, SearchInstance searchInstance);

	/** 进入副本 / On Enter Instance*/
	public abstract void onEnterInstance(Player player);

	/** 离开副本 / On Leave Instance*/
	public abstract void onLeaveInstance(Player player);

	/** 按下回车时 / on Press Enter. */
	public abstract void onPressEnter(Player player);

	/** 注销。 / Unregister. */
	public abstract void unregister(Player player);

	/** 清空。 / Clear. */
	public abstract void clear();
}
