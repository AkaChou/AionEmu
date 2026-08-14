package com.aionemu.gameserver.questEngine.definition;

/**
 * 声明目录条目是持有执行逻辑还是仅提供规范元数据。
 * Declares whether a catalog entry owns execution or only supplies canonical metadata.
 */
public enum QuestCatalogEntryMode {
	EXECUTABLE,
	METADATA_ONLY
}
