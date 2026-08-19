package com.aionemu.gameserver.questEngine.definition;

/**
 * 诱导任务 NPC 到达目标坐标后的世界完成副作用。
 * World-side completion effect after a lure quest NPC reaches its destination.
 */
public enum QuestLureCompletion {
	/** 安排重生并删除常驻 NPC。 / Schedule respawn and delete the resident NPC. */
	DELETE,
	/** 以玩家为死亡归因击杀 NPC。 / Kill the NPC with the player as the death attribution. */
	KILL
}
