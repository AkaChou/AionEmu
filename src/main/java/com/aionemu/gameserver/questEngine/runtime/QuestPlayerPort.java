package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/** Typed lookup boundary for a player whose quest state is being committed. */
@FunctionalInterface
public interface QuestPlayerPort {
	Player find(int playerId);
}
