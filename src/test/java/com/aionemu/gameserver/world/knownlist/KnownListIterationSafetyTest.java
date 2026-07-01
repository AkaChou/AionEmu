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
			"\\.(?:getKnownObjects|getVisibleObjects)\\(\\)\\.values\\(\\)|"
					+ "for\\s*\\([^:]+:\\s*(?!new\\s+(?:java\\.util\\.)?ArrayList<>)(?:knownObjects|knownPlayers|visualObjects|visualPlayers)\\.values\\(\\)\\)");
	private static final Pattern LIVE_EFFECT_VALUES = Pattern.compile(
			"for\\s*\\([^:]+:\\s*(?:abnormalEffectMap|passiveEffectMap|noshowEffects|mapToUpdate)\\.(?:values|entrySet)\\(\\)\\)");
	private static final Pattern LIVE_WORLD_INSTANCE_VALUES = Pattern.compile(
			"new\\s+ArrayList(?:<[^>]+>|<>)?\\(instances\\.(?:values|keySet)\\(\\)\\)");
	private static final Pattern LIVE_ZONE_PLAYER_VALUES = Pattern.compile(
			"for\\s*\\([^:]+:\\s*players\\.values\\(\\)\\)");
	private static final Pattern LIVE_SIEGE_LOCATION_VALUES = Pattern.compile(
			"for\\s*\\([^:]+:\\s*players\\.values\\(\\)\\)|getCreatures\\(\\)\\.values\\(\\)");

	@Test
	void knownListValuesAreSnapshotBeforeEnhancedForIteration() throws IOException {
		try (var files = Files.walk(Path.of("src/main/java"))) {
			List<String> offenders = files
					.filter(path -> path.toString().endsWith(".java"))
					.flatMap(KnownListIterationSafetyTest::unsafeKnownListValueIterations)
					.toList();

			assertTrue(offenders.isEmpty(), "Use KnownList snapshot helpers before iterating known-list maps:\n" + String.join("\n", offenders));
		}
	}

	@Test
	void effectValuesAreSnapshotBeforeEnhancedForIteration() throws IOException {
		Path path = Path.of("src/main/java/com/aionemu/gameserver/controllers/effect/EffectController.java");
		List<String> offenders = unsafeValueIterations(path, LIVE_EFFECT_VALUES).toList();

		assertTrue(offenders.isEmpty(), "Use EffectController snapshot helpers before iterating effect maps:\n" + String.join("\n", offenders));
	}

	@Test
	void worldMapInstancesAreSnapshotBeforeIteration() throws IOException {
		Path path = Path.of("src/main/java/com/aionemu/gameserver/world/WorldMap.java");
		List<String> offenders = unsafeValueIterations(path, LIVE_WORLD_INSTANCE_VALUES).toList();

		assertTrue(offenders.isEmpty(), "Use WorldMap snapshot helpers before iterating instance maps:\n" + String.join("\n", offenders));
	}

	@Test
	void zonePlayersAreSnapshotBeforeEnhancedForIteration() throws IOException {
		List<Path> paths = List.of(
				Path.of("src/main/java/com/aionemu/gameserver/world/zone/SiegeZoneInstance.java"),
				Path.of("src/main/java/com/aionemu/gameserver/world/zone/InvasionZoneInstance.java"));
		List<String> offenders = paths.stream()
				.flatMap(path -> unsafeValueIterations(path, LIVE_ZONE_PLAYER_VALUES))
				.toList();

		assertTrue(offenders.isEmpty(), "Use zone player snapshot helpers before iterating player maps:\n" + String.join("\n", offenders));
	}

	@Test
	void siegeLocationValuesAreSnapshotBeforeEnhancedForIteration() throws IOException {
		List<Path> paths = List.of(
				Path.of("src/main/java/com/aionemu/gameserver/model/siege/SiegeLocation.java"),
				Path.of("src/main/java/com/aionemu/gameserver/model/siege/FortressLocation.java"),
				Path.of("src/main/java/com/aionemu/gameserver/ai/siege/ArtifactAI2.java"));
		List<String> offenders = paths.stream()
				.flatMap(path -> unsafeValueIterations(path, LIVE_SIEGE_LOCATION_VALUES))
				.toList();

		assertTrue(offenders.isEmpty(), "Use siege snapshot helpers before iterating siege maps:\n" + String.join("\n", offenders));
	}

	private static java.util.stream.Stream<String> unsafeKnownListValueIterations(Path path) {
		return unsafeValueIterations(path, LIVE_KNOWN_LIST_VALUES);
	}

	private static java.util.stream.Stream<String> unsafeValueIterations(Path path, Pattern pattern) {
		try {
			List<String> lines = Files.readAllLines(path);
			return java.util.stream.IntStream.range(0, lines.size())
					.filter(index -> pattern.matcher(lines.get(index)).find())
					.filter(index -> !isSynchronizedSnapshotLine(lines, index))
					.mapToObj(index -> path + ":" + (index + 1) + ": " + lines.get(index).trim());
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}

	private static boolean isSynchronizedSnapshotLine(List<String> lines, int index) {
		return index > 0 && lines.get(index - 1).contains("synchronized (");
	}
}
