package com.aionemu.gameserver.world.knownlist;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class KnownListIterationSafetyTest {

	private static final Pattern LIVE_KNOWN_LIST_VALUES = Pattern.compile(
			"for\\s*\\([^:]+:\\s*(?!new\\s+(?:java\\.util\\.)?ArrayList<>)\\S+\\.(?:getKnownObjects|getVisibleObjects)\\(\\)\\.values\\(\\)\\)|"
					+ "for\\s*\\([^:]+:\\s*(?!new\\s+(?:java\\.util\\.)?ArrayList<>)(?:knownObjects|knownPlayers|visualObjects|visualPlayers)\\.values\\(\\)\\)");

	@Test
	void knownListValuesAreSnapshotBeforeEnhancedForIteration() throws IOException {
		try (var files = Files.walk(Path.of("src/main/java"))) {
			List<String> offenders = files
					.filter(path -> path.toString().endsWith(".java"))
					.flatMap(KnownListIterationSafetyTest::unsafeKnownListValueIterations)
					.toList();

			assertTrue(offenders.isEmpty(), "Use new ArrayList<>(...values()) before iterating known-list maps:\n" + String.join("\n", offenders));
		}
	}

	private static java.util.stream.Stream<String> unsafeKnownListValueIterations(Path path) {
		try {
			List<String> lines = Files.readAllLines(path);
			return java.util.stream.IntStream.range(0, lines.size())
					.filter(index -> LIVE_KNOWN_LIST_VALUES.matcher(lines.get(index)).find())
					.mapToObj(index -> path + ":" + (index + 1) + ": " + lines.get(index).trim());
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}
}
