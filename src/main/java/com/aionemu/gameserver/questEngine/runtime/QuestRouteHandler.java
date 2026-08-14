package com.aionemu.gameserver.questEngine.runtime;

/** 类型化 owner 边界；一个 owner 不能访问另一个 owner 的可变状态。 / Typed owner boundary; an owner cannot access another owner's mutable state. */
@FunctionalInterface
public interface QuestRouteHandler {
	QuestRouteResult handle(QuestEventIndex.Route route);
}
