package com.aionemu.gameserver.questEngine.definition;

/**
 * 进度字段持久化于 quest_vars 还是仅保存在内存中。
 * Whether a progress field is persisted in quest_vars or held in memory.
 */
public enum PersistenceMode {
	PERSISTENT,
	MEMORY
}
