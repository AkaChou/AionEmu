package com.aionemu.gameserver.questEngine.definition;

/** Ownership states used by the migration gates. */
public enum QuestOwnership {
	CURRENT,
	CURRENT_FALLBACK,
	UNRESOLVED,
	CATALOG_ONLY,
	/** Auxiliary machine projection. It may be syntax-checked but must never enter a runtime catalog. */
	ANALYSIS_DRAFT,
	COMPILED_CANDIDATE,
	RETAIL_ALIGNED
}
