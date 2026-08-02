package com.aionemu.gameserver.questEngine.definition;

/** Declares whether one offline-proven transition must also be observed in production shadow traffic. */
public enum QuestShadowCoverageRequirement {
	PRODUCTION_REQUIRED,
	OFFLINE_ONLY
}
