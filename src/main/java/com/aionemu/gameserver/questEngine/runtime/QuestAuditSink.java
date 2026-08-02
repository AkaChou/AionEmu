package com.aionemu.gameserver.questEngine.runtime;

/** Typed boundary for localized owner-failure audit records. */
@FunctionalInterface
public interface QuestAuditSink {
	void record(QuestAuditEvent event);
}
