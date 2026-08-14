package com.aionemu.gameserver.model.actions;

import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * NpcActions，用于 actions 相关逻辑。
 * Npc Actions for actions logic.
 */

public class NpcActions extends CreatureActions {
	/** 安排重生 / Schedule respawn */
	public static void scheduleRespawn(Npc npc) {
		npc.getController().scheduleRespawn();
	}
}
