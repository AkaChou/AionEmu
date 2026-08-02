package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Stable JSON serialization and atomic persistence of one shadow batch report.
 *
 * <p>Keys are emitted in a fixed order so the payload is deterministic and can
 * be validated by the read-only audit tooling (docs/quest/tools/shadow-report-audit.py).
 * The payload carries a {@code schemaVersion}, the expected/covered/missing and
 * unexpected unique-owner sets, per-owner path results and the typed difference
 * counts. Writes go to a temporary file and are atomically renamed; reading
 * back fails closed on truncated, empty, non-object or schema-incompatible
 * payloads so a corrupt batch can never masquerade as shadow evidence.</p>
 */
public final class QuestShadowReportWriter {
	public static final int SCHEMA_VERSION = 3;
	private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "expectedOwners", "coveredOwners",
		"missingOwners", "unexpectedOwners", "expectedCoverage", "coveredCoverage", "missingCoverage",
		"unexpectedCoverage", "expectedCoverageCount", "coveredCoverageCount", "expectedInvocations",
		"actualInvocations", "complete", "clean", "differenceCounts", "comparisons");
	private static final Set<String> COVERAGE_FIELDS = Set.of("questId", "eventType", "eventSelector",
		"sourceNode", "targetNode", "priority", "dispatchContract");
	private static final Set<String> COMPARISON_FIELDS = Set.of("eventType", "eventSelector", "inputs",
		"differences");
	private static final Set<String> INPUT_FIELDS = Set.of("questId", "status", "packedVariables");
	private static final Set<String> DIFFERENCE_FIELDS = Set.of("kind", "questId");

	private QuestShadowReportWriter() {
	}

	public static String toJson(QuestShadowBatchReport report) {
		if (report == null) {
			throw new IllegalArgumentException("report must not be null");
		}
		StringBuilder out = new StringBuilder();
		out.append('{');
		out.append("\"schemaVersion\":").append(SCHEMA_VERSION).append(',');
		out.append("\"expectedOwners\":").append(owners(report.expectedOwners())).append(',');
		out.append("\"coveredOwners\":").append(owners(report.coveredOwners())).append(',');
		out.append("\"missingOwners\":").append(owners(report.missingOwners())).append(',');
		out.append("\"unexpectedOwners\":").append(owners(report.unexpectedOwners())).append(',');
		out.append("\"expectedCoverage\":").append(coverage(report.expectedCoverage())).append(',');
		out.append("\"coveredCoverage\":").append(coverage(report.coveredCoverage())).append(',');
		out.append("\"missingCoverage\":").append(coverage(report.missingCoverage())).append(',');
		out.append("\"unexpectedCoverage\":").append(coverage(report.unexpectedCoverage())).append(',');
		out.append("\"expectedCoverageCount\":").append(report.expectedCoverage().size()).append(',');
		out.append("\"coveredCoverageCount\":").append(report.coveredCoverage().size()).append(',');
		out.append("\"expectedInvocations\":").append(report.expectedInvocations()).append(',');
		out.append("\"actualInvocations\":").append(report.actualInvocations()).append(',');
		out.append("\"complete\":").append(report.complete()).append(',');
		out.append("\"clean\":").append(report.clean()).append(',');
		out.append("\"differenceCounts\":").append(counts(report.differenceCounts())).append(',');
		out.append("\"comparisons\":").append(comparisons(report.comparisons()));
		out.append('}');
		return out.toString();
	}

	/**
	 * Atomically persists the serialized report: writes to a sibling temporary
	 * file and renames over the target, so a crash mid-write never leaves a
	 * truncated payload under the report name.
	 */
	public static void writeAtomic(Path path, QuestShadowBatchReport report) throws IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		Path absolute = path.toAbsolutePath();
		Path parent = absolute.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
		Files.writeString(temp, toJson(report) + "\n", StandardCharsets.UTF_8);
		try {
			Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomicFailure) {
			try {
				Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException fallbackFailure) {
				atomicFailure.addSuppressed(fallbackFailure);
				throw atomicFailure;
			}
		}
	}

	/**
	 * Parses and validates all gate-bearing fields and their set relationships.
	 * Duplicate entries, stale schemas and self-contradictory booleans fail closed.
	 */
	public static int readSchemaVersion(Path path) throws IOException {
		read(path);
		return SCHEMA_VERSION;
	}

	/** Returns the fully validated report so a production run can resume after restart. */
	public static QuestShadowBatchReport read(Path path) throws IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		String text = Files.readString(path, StandardCharsets.UTF_8).trim();
		if (text.isEmpty()) {
			throw new IllegalArgumentException("shadow report is empty: " + path);
		}
		try {
			JsonElement parsed = JsonParser.parseString(text);
			if (!parsed.isJsonObject()) {
				throw invalid(path, "root must be an object");
			}
			JsonObject root = parsed.getAsJsonObject();
			if (!root.keySet().equals(ROOT_FIELDS)) {
				throw invalid(path, "root fields do not match schema");
			}
			int version = integer(root, "schemaVersion");
			if (version != SCHEMA_VERSION) {
				throw invalid(path, "unsupported schemaVersion " + version);
			}
			Set<Integer> expectedOwners = ownerSet(root, "expectedOwners");
			Set<Integer> coveredOwners = ownerSet(root, "coveredOwners");
			assertSet(root, "missingOwners", difference(expectedOwners, coveredOwners), path);
			assertSet(root, "unexpectedOwners", difference(coveredOwners, expectedOwners), path);

			Set<QuestShadowCoverageKey> expectedCoverage = coverageSet(root, "expectedCoverage");
			Set<QuestShadowCoverageKey> coveredCoverage = coverageSet(root, "coveredCoverage");
			assertCoverage(root, "missingCoverage", difference(expectedCoverage, coveredCoverage), path);
			assertCoverage(root, "unexpectedCoverage", difference(coveredCoverage, expectedCoverage), path);
			if (integer(root, "expectedCoverageCount") != expectedCoverage.size()
					|| integer(root, "coveredCoverageCount") != coveredCoverage.size()) {
				throw invalid(path, "coverage counts do not match coverage sets");
			}
			if (integer(root, "expectedInvocations") != expectedOwners.size()
					|| integer(root, "actualInvocations") != coveredOwners.size()) {
				throw invalid(path, "owner counts do not match owner sets");
			}
			boolean complete = expectedOwners.equals(coveredOwners) && expectedCoverage.equals(coveredCoverage);
			if (bool(root, "complete") != complete) {
				throw invalid(path, "complete does not match exact coverage");
			}
			List<QuestShadowComparison> comparisons = comparisons(root, path);
			Map<QuestShadowDifferenceKind, Integer> actualCounts = comparisonCounts(comparisons);
			Map<QuestShadowDifferenceKind, Integer> declaredCounts = differenceCounts(root, path);
			if (!actualCounts.equals(declaredCounts)) {
				throw invalid(path, "differenceCounts do not match comparisons");
			}
			if (bool(root, "clean") != (complete && actualCounts.isEmpty())) {
				throw invalid(path, "clean does not match coverage and differences");
			}
			return new QuestShadowBatchReport(expectedOwners, coveredOwners, expectedCoverage,
				coveredCoverage, comparisons);
		} catch (JsonParseException | IllegalStateException | NumberFormatException e) {
			throw invalid(path, "invalid JSON payload", e);
		}
	}

	private static String owners(Set<Integer> ownerIds) {
		StringBuilder out = new StringBuilder();
		out.append('[');
		boolean first = true;
		for (int ownerId : ownerIds.stream().sorted().toList()) {
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append(ownerId);
		}
		out.append(']');
		return out.toString();
	}

	private static String counts(Map<QuestShadowDifferenceKind, Integer> counts) {
		StringBuilder out = new StringBuilder();
		out.append('{');
		boolean first = true;
		for (QuestShadowDifferenceKind kind : QuestShadowDifferenceKind.values()) {
			Integer count = counts.get(kind);
			if (count == null || count == 0) {
				continue;
			}
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append('"').append(kind.name()).append("\":").append(count);
		}
		out.append('}');
		return out.toString();
	}

	private static String coverage(Set<QuestShadowCoverageKey> keys) {
		Comparator<QuestShadowCoverageKey> order = Comparator.comparingInt(QuestShadowCoverageKey::questId)
			.thenComparing(QuestShadowCoverageKey::eventType)
			.thenComparing(QuestShadowCoverageKey::eventSelector)
			.thenComparing(QuestShadowCoverageKey::sourceNode)
			.thenComparing(QuestShadowCoverageKey::targetNode)
			.thenComparing(QuestShadowCoverageKey::priority, Comparator.nullsFirst(Integer::compareTo))
			.thenComparing(QuestShadowCoverageKey::dispatchContract);
		StringBuilder out = new StringBuilder("[");
		boolean first = true;
		for (QuestShadowCoverageKey key : keys.stream().sorted(order).toList()) {
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append('{')
				.append("\"questId\":").append(key.questId()).append(',')
				.append("\"eventType\":").append(json(key.eventType())).append(',')
				.append("\"eventSelector\":").append(json(key.eventSelector())).append(',')
				.append("\"sourceNode\":").append(json(key.sourceNode())).append(',')
				.append("\"targetNode\":").append(json(key.targetNode())).append(',')
				.append("\"priority\":").append(key.priority() == null ? "null" : key.priority()).append(',')
				.append("\"dispatchContract\":").append(json(key.dispatchContract()))
				.append('}');
		}
		return out.append(']').toString();
	}

	private static String comparisons(List<QuestShadowComparison> comparisons) {
		StringBuilder out = new StringBuilder();
		out.append('[');
		for (int i = 0; i < comparisons.size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			out.append(comparison(comparisons.get(i)));
		}
		out.append(']');
		return out.toString();
	}

	private static String comparison(QuestShadowComparison comparison) {
		StringBuilder out = new StringBuilder();
		out.append('{');
		out.append("\"eventType\":").append(json(comparison.eventType())).append(',');
		out.append("\"eventSelector\":").append(json(comparison.eventSelector())).append(',');
		out.append("\"inputs\":[");
		List<QuestShadowComparison.OwnerInput> inputs = comparison.inputs();
		for (int i = 0; i < inputs.size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			QuestShadowComparison.OwnerInput input = inputs.get(i);
			out.append("{\"questId\":").append(input.questId())
				.append(",\"status\":").append(json(input.status().name()))
				.append(",\"packedVariables\":").append(input.packedVariables()).append('}');
		}
		out.append("],");
		out.append("\"differences\":[");
		List<QuestShadowDifference> differences = comparison.differences();
		for (int i = 0; i < differences.size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			QuestShadowDifference difference = differences.get(i);
			out.append("{\"kind\":\"").append(difference.kind().name()).append("\",\"questId\":")
				.append(difference.questId()).append('}');
		}
		out.append("]}");
		return out.toString();
	}

	private static String json(String value) {
		return new JsonPrimitive(value).toString();
	}

	private static int integer(JsonObject object, String name) {
		JsonElement value = required(object, name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
				|| !value.getAsString().matches("-?\\d+")) {
			throw new IllegalArgumentException(name + " must be an integer");
		}
		return value.getAsInt();
	}

	private static boolean bool(JsonObject object, String name) {
		JsonElement value = required(object, name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException(name + " must be a boolean");
		}
		return value.getAsBoolean();
	}

	private static String string(JsonObject object, String name) {
		JsonElement value = required(object, name);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().isBlank()) {
			throw new IllegalArgumentException(name + " must be a non-blank string");
		}
		return value.getAsString();
	}

	private static JsonElement required(JsonObject object, String name) {
		JsonElement value = object.get(name);
		if (value == null) {
			throw new IllegalArgumentException("missing " + name);
		}
		return value;
	}

	private static JsonArray array(JsonObject object, String name) {
		JsonElement value = required(object, name);
		if (!value.isJsonArray()) {
			throw new IllegalArgumentException(name + " must be an array");
		}
		return value.getAsJsonArray();
	}

	private static Set<Integer> ownerSet(JsonObject root, String name) {
		Set<Integer> owners = new LinkedHashSet<>();
		for (JsonElement element : array(root, name)) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()
					|| !element.getAsString().matches("\\d+")) {
				throw new IllegalArgumentException(name + " must contain positive integers");
			}
			int owner = element.getAsInt();
			if (owner <= 0 || !owners.add(owner)) {
				throw new IllegalArgumentException(name + " contains invalid or duplicate owner " + owner);
			}
		}
		return Set.copyOf(owners);
	}

	private static Set<QuestShadowCoverageKey> coverageSet(JsonObject root, String name) {
		Set<QuestShadowCoverageKey> coverage = new LinkedHashSet<>();
		for (JsonElement element : array(root, name)) {
			if (!element.isJsonObject() || !element.getAsJsonObject().keySet().equals(COVERAGE_FIELDS)) {
				throw new IllegalArgumentException(name + " contains an invalid coverage object");
			}
			JsonObject object = element.getAsJsonObject();
			JsonElement priorityElement = required(object, "priority");
			Integer priority = priorityElement.isJsonNull() ? null : integer(object, "priority");
			QuestShadowCoverageKey key = new QuestShadowCoverageKey(integer(object, "questId"),
				string(object, "eventType"), string(object, "eventSelector"), string(object, "sourceNode"),
				string(object, "targetNode"), priority, string(object, "dispatchContract"));
			if (!coverage.add(key)) {
				throw new IllegalArgumentException(name + " contains duplicate coverage key");
			}
		}
		return Set.copyOf(coverage);
	}

	private static Map<QuestShadowDifferenceKind, Integer> differenceCounts(JsonObject root, Path path) {
		JsonElement element = required(root, "differenceCounts");
		if (!element.isJsonObject()) {
			throw invalid(path, "differenceCounts must be an object");
		}
		EnumMap<QuestShadowDifferenceKind, Integer> counts = new EnumMap<>(QuestShadowDifferenceKind.class);
		for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
			QuestShadowDifferenceKind kind = QuestShadowDifferenceKind.valueOf(entry.getKey());
			JsonObject wrapper = new JsonObject();
			wrapper.add("count", entry.getValue());
			int count = integer(wrapper, "count");
			if (count <= 0) {
				throw invalid(path, "difference count must be positive");
			}
			counts.put(kind, count);
		}
		return Map.copyOf(counts);
	}

	private static List<QuestShadowComparison> comparisons(JsonObject root, Path path) {
		List<QuestShadowComparison> comparisons = new java.util.ArrayList<>();
		for (JsonElement comparisonElement : array(root, "comparisons")) {
			if (!comparisonElement.isJsonObject()
					|| !comparisonElement.getAsJsonObject().keySet().equals(COMPARISON_FIELDS)) {
				throw invalid(path, "comparison must be an object");
			}
			JsonObject comparison = comparisonElement.getAsJsonObject();
			String eventType = string(comparison, "eventType");
			String eventSelector = string(comparison, "eventSelector");
			List<QuestShadowComparison.OwnerInput> inputs = new java.util.ArrayList<>();
			Set<Integer> inputOwners = new java.util.HashSet<>();
			for (JsonElement inputElement : array(comparison, "inputs")) {
				if (!inputElement.isJsonObject()
						|| !inputElement.getAsJsonObject().keySet().equals(INPUT_FIELDS)) {
					throw invalid(path, "comparison input must be an object");
				}
				JsonObject input = inputElement.getAsJsonObject();
				int questId = integer(input, "questId");
				if (questId <= 0 || !inputOwners.add(questId)) {
					throw invalid(path, "comparison input questId must be unique and positive");
				}
				QuestStatus status = QuestStatus.valueOf(string(input, "status"));
				int packedVariables = integer(input, "packedVariables");
				if (packedVariables < 0) {
					throw invalid(path, "comparison input packedVariables must be non-negative");
				}
				inputs.add(new QuestShadowComparison.OwnerInput(questId, status, packedVariables));
			}
			List<QuestShadowDifference> differences = new java.util.ArrayList<>();
			for (JsonElement differenceElement : array(comparison, "differences")) {
				if (!differenceElement.isJsonObject()
						|| !differenceElement.getAsJsonObject().keySet().equals(DIFFERENCE_FIELDS)) {
					throw invalid(path, "difference must be an object");
				}
				JsonObject difference = differenceElement.getAsJsonObject();
				QuestShadowDifferenceKind kind = QuestShadowDifferenceKind.valueOf(string(difference, "kind"));
				int questId = integer(difference, "questId");
				if (questId < 0) {
					throw invalid(path, "difference questId must be non-negative");
				}
				differences.add(new QuestShadowDifference(kind, questId));
			}
			comparisons.add(new QuestShadowComparison(eventType, eventSelector, inputs, differences));
		}
		return List.copyOf(comparisons);
	}

	private static Map<QuestShadowDifferenceKind, Integer> comparisonCounts(
			List<QuestShadowComparison> comparisons) {
		EnumMap<QuestShadowDifferenceKind, Integer> counts = new EnumMap<>(QuestShadowDifferenceKind.class);
		for (QuestShadowComparison comparison : comparisons) {
			for (QuestShadowDifference difference : comparison.differences()) {
				counts.merge(difference.kind(), 1, Integer::sum);
			}
		}
		return Map.copyOf(counts);
	}

	private static void assertSet(JsonObject root, String name, Set<Integer> expected, Path path) {
		if (!ownerSet(root, name).equals(expected)) {
			throw invalid(path, name + " does not match owner set difference");
		}
	}

	private static void assertCoverage(JsonObject root, String name, Set<QuestShadowCoverageKey> expected, Path path) {
		if (!coverageSet(root, name).equals(expected)) {
			throw invalid(path, name + " does not match coverage set difference");
		}
	}

	private static <T> Set<T> difference(Set<T> left, Set<T> right) {
		Set<T> result = new LinkedHashSet<>(left);
		result.removeAll(right);
		return Set.copyOf(result);
	}

	private static IllegalArgumentException invalid(Path path, String message) {
		return new IllegalArgumentException("invalid shadow report " + path + ": " + message);
	}

	private static IllegalArgumentException invalid(Path path, String message, RuntimeException cause) {
		return new IllegalArgumentException("invalid shadow report " + path + ": " + message, cause);
	}
}
