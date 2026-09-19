package com.aionemu.gameserver.network.aion;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * 任务追踪日志路由闸门：四个 trace 出口必须使用 logback 的 quest logger。
 * Quest trace routing gate: all four trace call sites must use the logback quest logger.
 */
class QuestTraceLogRoutingTest {

	private static final List<TraceSource> TRACE_SOURCES = List.of(
		new TraceSource("src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_USE_ITEM.java",
			"log.quest_trace.use_item"),
		new TraceSource("src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java",
			"log.quest_trace.dialog_select"),
		new TraceSource("src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_QUEST_ACTION.java",
			"log.quest_trace.quest_action"),
		new TraceSource("src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_DIALOG_WINDOW.java",
			"log.quest_trace.dialog_window"));

	@Test
	void traceSourcesUseQuestFileLogger() throws IOException {
		for (TraceSource traceSource : TRACE_SOURCES) {
			String source = Files.readString(Path.of(traceSource.path()));
			assertTrue(source.contains("LoggerFactory.getLogger(\"quest\")"),
				traceSource.path() + " must use the quest logger");
			Pattern call = Pattern.compile(
				"QUEST_TRACE_LOG\\.info\\s*\\(\\s*I18n\\.get\\s*\\(\\s*\""
					+ Pattern.quote(traceSource.messageKey()) + "\"");
			assertTrue(call.matcher(source).find(),
				traceSource.path() + " must route " + traceSource.messageKey() + " through QUEST_TRACE_LOG");
		}
	}

	private record TraceSource(String path, String messageKey) {
	}
}
