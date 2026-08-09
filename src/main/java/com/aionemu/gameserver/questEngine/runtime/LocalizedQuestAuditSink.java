package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/** Localized audit adapter; routing never depends on an exception message or stack text. */
@Slf4j(topic = "QUEST_AUDIT")
public final class LocalizedQuestAuditSink implements QuestAuditSink {
	@Override
	public void record(QuestAuditEvent event) {
		log.warn(I18n.get("log.quest_engine.owner_failed", event.questId(), event.eventType(),
			event.contract(), event.result(), event.sourceNode(), event.targetNode(), event.npcId(),
			event.dialogId(), event.failureStage(), event.committed(), event.failureType()), event.failure());
	}
}
