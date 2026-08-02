package com.aionemu.gameserver.questEngine.runtime;

/** Existing caller contracts preserved as explicit routing policies. */
public enum QuestDispatchContract {
	EXCLUSIVE,
	FIRST_NON_UNKNOWN,
	FIRST_REGISTERED,
	BROADCAST
}
