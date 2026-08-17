package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.e2e.client.QuestTrace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将端到端结果写成稳定 JSONL、汇总 CSV 和中文交接摘要；写入目录由调用方显式指定。
 * Writes stable JSONL, summary CSV, and a Chinese handoff summary to caller-selected directories.
 */
public final class QuestE2eReportWriter {
	private QuestE2eReportWriter() {
	}

	/** 写出计划约定的三个报告文件。 / Writes the three report files required by the plan. */
	public static void write(List<QuestE2eAuditRow> rows, Path reportDirectory, Path summaryFile) throws IOException {
		Files.createDirectories(reportDirectory);
		Files.createDirectories(summaryFile.toAbsolutePath().normalize().getParent());
		StringBuilder jsonl = new StringBuilder();
		for (QuestE2eAuditRow row : rows) {
			if (!jsonl.isEmpty()) jsonl.append('\n');
			jsonl.append(json(row));
		}
		jsonl.append('\n');
		Files.writeString(reportDirectory.resolve("quest-e2e-report.jsonl"), jsonl.toString(), StandardCharsets.UTF_8);
		Map<QuestE2eStatus, Long> counts = new LinkedHashMap<>();
		for (QuestE2eStatus status : QuestE2eStatus.values()) counts.put(status, rows.stream().filter(row -> row.status() == status).count());
		Map<QuestE2eTransitionMatch, Long> matchCounts = new LinkedHashMap<>();
		for (QuestE2eTransitionMatch match : QuestE2eTransitionMatch.values()) {
			matchCounts.put(match, rows.stream().filter(row -> row.transitionMatch() == match).count());
		}
		Map<String, Long> modeCounts = new LinkedHashMap<>();
		rows.forEach(row -> modeCounts.merge(row.validationMode(), 1L, Long::sum));
		StringBuilder csv = new StringBuilder("status,count\n");
		counts.forEach((status, count) -> csv.append(status).append(',').append(count).append('\n'));
		matchCounts.forEach((match, count) -> csv.append("TRANSITION_MATCH:").append(match).append(',').append(count).append('\n'));
		modeCounts.forEach((mode, count) -> csv.append("VALIDATION_MODE:").append(mode).append(',').append(count).append('\n'));
		Files.writeString(reportDirectory.resolve("quest-e2e-summary.csv"), csv.toString(), StandardCharsets.UTF_8);
		StringBuilder markdown = new StringBuilder("# quest-e2e 摘要\n\n");
		markdown.append("本报告由正式 production catalog、内存事务和 Aion 5.8 客户端四份资源表独立生成；未读取顺序审计文件。\n\n");
		markdown.append("| 状态 | 数量 |\n|---|---:|\n");
		counts.forEach((status, count) -> markdown.append('|').append(status).append('|').append(count).append("|\n"));
		markdown.append("\n| transition 归因 | 数量 |\n|---|---:|\n");
		matchCounts.forEach((match, count) -> markdown.append('|').append(match).append('|').append(count).append("|\n"));
		markdown.append("\n| 验证模式 | 数量 |\n|---|---:|\n");
		modeCounts.forEach((mode, count) -> markdown.append('|').append(mode).append('|').append(count).append("|\n"));
		markdown.append("\n确定性错误才进入核心门禁；EVIDENCE_REQUIRED/RUNTIME_REQUIRED 保留为后续证据或运行时队列。\n");
		Files.writeString(summaryFile, markdown.toString(), StandardCharsets.UTF_8);
	}

	private static String json(QuestE2eAuditRow row) {
		return "{"
			+ "\"questId\":" + row.questId()
			+ ",\"eventType\":" + quote(row.eventType())
			+ ",\"sourceNode\":" + quote(row.sourceNode())
			+ ",\"targetNode\":" + quote(row.targetNode())
			+ ",\"matchedSourceNode\":" + quote(row.matchedSourceNode())
			+ ",\"matchedTargetNode\":" + quote(row.matchedTargetNode())
			+ ",\"transitionMatch\":" + quote(row.transitionMatch().name())
			+ ",\"validationMode\":" + quote(row.validationMode())
			+ ",\"targetStatus\":" + quote(row.targetStatus())
			+ ",\"observedStatus\":" + quote(row.observedStatus())
			+ ",\"observedPackedVariables\":" + row.observedPackedVariables()
			+ ",\"npcId\":" + row.npcId()
			+ ",\"objectId\":" + row.objectId()
			+ ",\"dialogId\":" + row.dialogId()
			+ ",\"shownPage\":" + row.shownPage()
			+ ",\"status\":" + quote(row.status().name())
			+ ",\"reason\":" + quote(row.reason())
			+ ",\"evidence\":" + quote(row.evidence())
			+ ",\"trace\":[" + row.trace().stream().map(QuestE2eReportWriter::traceJson).reduce((a, b) -> a + "," + b).orElse("") + "]}";
	}

	private static String traceJson(QuestTrace.Entry entry) {
		return "{\"sequence\":" + entry.sequence() + ",\"phase\":" + quote(entry.phase())
			+ ",\"detail\":" + quote(entry.detail()) + "}";
	}

	private static String quote(String value) {
		StringBuilder result = new StringBuilder("\"");
		String text = value == null ? "" : value;
		for (char character : text.toCharArray()) {
			switch (character) {
				case '\\' -> result.append("\\\\");
				case '"' -> result.append("\\\"");
				case '\n' -> result.append("\\n");
				case '\r' -> result.append("\\r");
				default -> result.append(character);
			}
		}
		return result.append('"').toString();
	}
}
