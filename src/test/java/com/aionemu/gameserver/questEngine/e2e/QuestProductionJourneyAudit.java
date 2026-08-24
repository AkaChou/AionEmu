package com.aionemu.gameserver.questEngine.e2e;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionDirectoryLoader;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.QuestTrace;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestJourneyRunner;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eWorldFixture;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 从生产任务目录编译 owner，自动规划并执行持续无头 Journey 的命令行入口；可按任务 ID 聚焦，
 * 不读取测试中手写的任务路径。
 * Command-line entry point that compiles owners from the production quest directory, automatically plans and runs
 * persistent headless journeys, optionally focused by quest id, without reading handwritten test journeys.
 */
public final class QuestProductionJourneyAudit {
	private QuestProductionJourneyAudit() {
	}

	/**
	 * 参数依次为任务 ID 或 {@code all}、客户端映射目录和报告文件。
	 * Arguments are quest id or {@code all}, client-mapping directory, and report file.
	 */
	public static void main(String[] args) throws Exception {
		Logger questRuntimeLogger = (Logger) LoggerFactory.getLogger("QUEST_RUNTIME");
		Level previousLevel = questRuntimeLogger.getLevel();
		questRuntimeLogger.setLevel(Level.ERROR);
		try {
			String selector = args.length > 0 ? args[0] : "all";
			Path mapping = args.length > 1 ? Path.of(args[1]) : Path.of("docs/quest/client-dialog-mapping");
			Path report = args.length > 2 ? Path.of(args[2]) : Path.of("target/quest-e2e/production-journeys.jsonl");
			QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(
				QuestProductionJourneyAudit.class.getClassLoader());
			ClientResourceOracle oracle = ClientResourceOracle.load(mapping);
			List<Row> rows = audit(select(catalog, selector), oracle);
			write(rows, report);
			long completed = rows.stream().filter(row -> "COMPLETE".equals(row.status())).count();
			long failed = rows.stream().filter(row -> "FAILED".equals(row.status())).count();
			long unplanned = rows.stream().filter(row -> "UNPLANNED".equals(row.status())).count();
			System.out.println("quest-production-journeys owners=" + rows.size() + " complete=" + completed
				+ " failed=" + failed + " unplanned=" + unplanned + " report=" + report);
		} finally {
			questRuntimeLogger.setLevel(previousLevel);
			QuestE2eWorldFixture.shutdownPacketProcessor();
		}
	}

	/** 审计显式生产 owner 集合。 / Audits an explicit collection of production owners. */
	static List<Row> audit(List<CompiledQuestDefinition> definitions, ClientResourceOracle oracle) throws Exception {
		Objects.requireNonNull(definitions, "definitions");
		Objects.requireNonNull(oracle, "oracle");
		QuestProductionJourneyPlanner planner = new QuestProductionJourneyPlanner();
		QuestProductionJourneyExecutor executor = new QuestProductionJourneyExecutor();
		List<Row> rows = new ArrayList<>();
		for (CompiledQuestDefinition definition : definitions) {
			QuestProductionJourneyPlanner.Result planned = planner.plan(definition, oracle);
			if (!planned.planned()) {
				QuestProductionJourneyPlanner.Failure failure = planned.failure();
				rows.add(Row.unplanned(definition.id(), failure));
				continue;
			}
			try {
				QuestProductionJourneyExecutor.Result executed = executor.execute(definition, oracle, planned.plan());
				rows.add(executed.completed()
					? Row.completed(definition.id(), planned.plan().steps().size())
					: Row.failed(definition.id(), executed.failure()));
			} catch (Exception failure) {
				rows.add(Row.runtimeFailure(definition.id(), planned.plan(), failure));
			}
		}
		return List.copyOf(rows);
	}

	private static List<CompiledQuestDefinition> select(QuestCatalog catalog, String selector) {
		Objects.requireNonNull(catalog, "catalog");
		if (selector == null || selector.isBlank() || "all".equalsIgnoreCase(selector)) {
			return catalog.executables().stream().sorted(java.util.Comparator.comparingInt(CompiledQuestDefinition::id))
				.toList();
		}
		int questId;
		try {
			questId = Integer.parseInt(selector);
		} catch (NumberFormatException failure) {
			throw new IllegalArgumentException("quest selector must be a positive quest id or all", failure);
		}
		return List.of(catalog.findExecutable(questId)
			.orElseThrow(() -> new IllegalArgumentException("production catalog has no executable quest " + questId)));
	}

	private static void write(List<Row> rows, Path report) throws IOException {
		Path parent = report.toAbsolutePath().normalize().getParent();
		if (parent != null) Files.createDirectories(parent);
		StringBuilder output = new StringBuilder();
		for (Row row : rows) output.append(row.json()).append('\n');
		Files.writeString(report, output.toString(), StandardCharsets.UTF_8);
	}

	/** 每个生产 owner 的完成或首错记录。 / Completion or first-failure record for one production owner. */
	public record Row(int questId, String status, int steps, int failedStep, String failureStatus,
			String sourceNode, String targetNode, int page, int npcId, int objectId, int expectedObjectId,
			int dialogId, String observedStatus, int observedPackedVariables, String reason,
			List<String> packets, List<String> committedActions,
			List<QuestTrace.Entry> trace) {
		public Row {
			if (questId <= 0) throw new IllegalArgumentException("questId must be positive");
			status = requireText(status, "status");
			failureStatus = Objects.requireNonNullElse(failureStatus, "");
			sourceNode = Objects.requireNonNullElse(sourceNode, "");
			targetNode = Objects.requireNonNullElse(targetNode, "");
			observedStatus = Objects.requireNonNullElse(observedStatus, "");
			reason = Objects.requireNonNullElse(reason, "");
			packets = List.copyOf(packets);
			committedActions = List.copyOf(committedActions);
			trace = List.copyOf(trace);
		}

		private static Row completed(int questId, int steps) {
			return new Row(questId, "COMPLETE", steps, -1, "", "", "", 0, 0, 0, 0, 0, "COMPLETE", 0,
				"", List.of(), List.of(), List.of());
		}

		private static Row unplanned(int questId, QuestProductionJourneyPlanner.Failure failure) {
			return new Row(questId, "UNPLANNED", 0, -1, "PLANNING_REQUIRED", failure.node(), "",
				failure.page(), 0, 0, 0, 0, "", 0, failure.reason(), List.of(), List.of(), List.of());
		}

		private static Row failed(int questId, QuestProductionJourneyExecutor.Failure failure) {
			QuestJourneyRunner.Step step = failure.observed();
			var transition = failure.expected().transition();
			return new Row(questId, "FAILED", failure.stepIndex() + 1, failure.stepIndex(), failure.status().name(),
				transition == null ? "" : transition.sourceNode(), transition == null ? "" : transition.targetNode(),
				step == null ? 0 : step.page(), transition == null ? 0 : QuestProductionJourneyAudit.npcId(transition.event()),
				step == null ? 0 : step.objectId(),
				step == null ? 0 : step.expectedDialogTargetObjectId(),
				transition == null ? 0 : QuestProductionJourneyAudit.dialogId(transition.event()),
				step == null ? "" : step.status().name(), step == null ? 0 : step.packedVariables(), failure.reason(),
				step == null ? List.of() : step.outcome().packets().stream().map(Row::packet).toList(),
				step == null ? List.of() : step.committedActions().stream().map(Object::toString).toList(),
				step == null ? List.of() : step.trace());
		}

		private static Row runtimeFailure(int questId, QuestProductionJourneyPlanner.Plan plan, Exception failure) {
			var transition = plan.steps().stream().map(QuestProductionJourneyPlanner.PlannedStep::transition)
				.filter(Objects::nonNull).findFirst().orElse(null);
			return new Row(questId, "FAILED", 0, 0, QuestE2eStatus.RUNTIME_REQUIRED.name(),
				transition == null ? "" : transition.sourceNode(), transition == null ? "" : transition.targetNode(), 0,
				transition == null ? 0 : QuestProductionJourneyAudit.npcId(transition.event()), 0, 0,
				transition == null ? 0 : QuestProductionJourneyAudit.dialogId(transition.event()), "", 0,
				failure.getClass().getName() + ":" + failure.getMessage(), List.of(), List.of(), List.of());
		}

		private String json() {
			return "{"
				+ "\"questId\":" + questId
				+ ",\"status\":" + quote(status)
				+ ",\"steps\":" + steps
				+ ",\"failedStep\":" + failedStep
				+ ",\"failureStatus\":" + quote(failureStatus)
				+ ",\"sourceNode\":" + quote(sourceNode)
				+ ",\"targetNode\":" + quote(targetNode)
				+ ",\"page\":" + page
				+ ",\"npcId\":" + npcId
				+ ",\"objectId\":" + objectId
				+ ",\"expectedObjectId\":" + expectedObjectId
				+ ",\"dialogId\":" + dialogId
				+ ",\"observedStatus\":" + quote(observedStatus)
				+ ",\"observedPackedVariables\":" + observedPackedVariables
				+ ",\"reason\":" + quote(reason)
				+ ",\"packets\":" + strings(packets)
				+ ",\"committedActions\":" + strings(committedActions)
				+ ",\"trace\":[" + trace.stream().map(Row::trace).reduce((left, right) -> left + "," + right).orElse("")
				+ "]}";
		}

		private static String packet(ServerPacketObservation packet) {
			return packet.type() + ":target=" + packet.targetObjectId() + ":dialog=" + packet.dialogId()
				+ ":quest=" + packet.questId() + ":action=" + packet.action() + ":status=" + packet.status()
				+ ":step=" + packet.step() + ":movie=" + packet.movieId();
		}

		private static String trace(QuestTrace.Entry entry) {
			return "{\"sequence\":" + entry.sequence() + ",\"phase\":" + quote(entry.phase())
				+ ",\"detail\":" + quote(entry.detail()) + "}";
		}

		private static String strings(List<String> values) {
			return "[" + values.stream().map(Row::quote).reduce((left, right) -> left + "," + right).orElse("") + "]";
		}

		private static String quote(String value) {
			StringBuilder result = new StringBuilder("\"");
			for (char character : Objects.requireNonNullElse(value, "").toCharArray()) {
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

	private static int npcId(com.aionemu.gameserver.questEngine.definition.QuestEvent event) {
		return switch (event) {
			case com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc talk -> talk.npcId();
			case com.aionemu.gameserver.questEngine.definition.QuestEvent.KillNpc kill -> kill.npcId();
			case com.aionemu.gameserver.questEngine.definition.QuestEvent.AttackNpc attack -> attack.npcId();
			default -> 0;
		};
	}

	private static int dialogId(com.aionemu.gameserver.questEngine.definition.QuestEvent event) {
		return switch (event) {
			case com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc talk ->
				talk.dialogId() == null ? 0 : talk.dialogId();
			case com.aionemu.gameserver.questEngine.definition.QuestEvent.QuestDialog dialog -> dialog.dialogId();
			default -> 0;
		};
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
		return value;
	}
}
