package com.aionemu.gameserver.questEngine.runtime;

/** Typed owner boundary; an owner cannot access another owner's mutable state. */
@FunctionalInterface
public interface QuestRouteHandler {
	QuestRouteResult handle(QuestEventIndex.Route route);
}
