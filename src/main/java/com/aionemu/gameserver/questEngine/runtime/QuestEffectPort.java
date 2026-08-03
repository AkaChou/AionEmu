package com.aionemu.gameserver.questEngine.runtime;

/** Typed boundary for player effects applied after a successful commit. */
public interface QuestEffectPort {
	/** Morphs the player into the ascension form for the given ascension id. */
	boolean morph(QuestSnapshot snapshot, QuestMutationPlan plan, int ascensionId);

	/** Starts a flight teleport for the given route id. */
	boolean flightTeleport(QuestSnapshot snapshot, QuestMutationPlan plan, int flightTeleportId);
}
