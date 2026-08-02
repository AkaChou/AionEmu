package com.aionemu.gameserver.questEngine.runtime;

/** Result contract used by the legacy call-site adapters during shadowing. */
public enum QuestRouteResult {
	HANDLED,
	NOT_HANDLED,
	UNKNOWN,
	FAILED
}
