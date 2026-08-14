package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/** 正在提交任务状态的玩家的类型化查找边界。 / Typed lookup boundary for a player whose quest state is being committed. */
@FunctionalInterface
public interface QuestPlayerPort {
	Player find(int playerId);
}
