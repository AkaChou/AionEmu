package com.aionemu.gameserver.questEngine.runtime;

/** 本地化拥有者失败审计记录的类型化边界。 / Typed boundary for localized owner-failure audit records. */
@FunctionalInterface
public interface QuestAuditSink {
	void record(QuestAuditEvent event);
}
