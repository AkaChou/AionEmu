package com.aionemu.gameserver.scriptEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class RetailScriptNpcConsumerClosureTest {

	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts");
	private static final Map<String, String> EXPECTED = Map.ofEntries(
		Map.entry("idgelDome/IdgelDomeLandmarkInstance.java", "RETAIL_SCORE_FALLBACK"),
		Map.entry("idgelDome/IdgelDomeInstance.java", "RETAIL_SCORE_FALLBACK"),
		Map.entry("KamarBattlefieldInstance.java", "RETAIL_SCORE_FALLBACK"),
		Map.entry("pvparenas/PvPArenaInstance.java", "RETAIL_SCORE_FALLBACK"),
		Map.entry("pvparenas/HarmonyArenaInstance.java", "RETAIL_SCORE_FALLBACK"),
		Map.entry("SeizedDanuarSanctuaryInstance.java", "STATEFUL_LEGACY"),
		Map.entry("DanuarSanctuaryInstance.java", "STATEFUL_LEGACY"),
		Map.entry("EsoterraceInstance.java", "STATEFUL_LEGACY"),
		Map.entry("SmolderingFireTempleInstance.java", "STATEFUL_LEGACY"),
		Map.entry("BeshmundirTempleInstance.java", "STATEFUL_LEGACY"),
		Map.entry("SealedArgentManorInstance.java", "STATEFUL_LEGACY"),
		Map.entry("OccupiedRentusBaseInstance.java", "STATEFUL_LEGACY"),
		Map.entry("RentusBaseInstance.java", "STATEFUL_LEGACY"),
		Map.entry("EngulfedOphidanBridgeInstance.java", "STATEFUL_LEGACY"),
		Map.entry("TreasureIslandOfCourageInstance.java", "STATEFUL_LEGACY"),
		Map.entry("OphidanWarpathInstance.java", "STATEFUL_LEGACY"),
		Map.entry("TiamatStrongholdInstance.java", "STATEFUL_LEGACY"),
		Map.entry("KumukiCaveInstance.java", "STATEFUL_LEGACY"),
		Map.entry("AdmaStrongholdInstance.java", "STATEFUL_LEGACY"),
		Map.entry("TrialsOfEternityInstance.java", "STATEFUL_LEGACY"),
		Map.entry("CradleOfEternityInstance.java", "STATEFUL_LEGACY"),
		Map.entry("ElementisForestInstance.java", "NON_PRODUCTION"));

	@Test
	void locksEveryRemainingInstanceItemUseConsumer() throws Exception {
		Set<String> actual;
		try (var paths = Files.walk(HANDLERS)) {
			actual = paths.filter(path -> path.toString().endsWith(".java")).filter(path -> {
				try {
					return Files.readString(path).contains("void handleUseItemFinish(Player player, Npc npc)");
				} catch (java.io.IOException e) {
					throw new java.io.UncheckedIOException(e);
				}
			}).map(HANDLERS::relativize).map(Path::toString).map(path -> path.replace('\\', '/')).collect(Collectors.toSet());
		}

		assertEquals(EXPECTED.keySet(), actual);
		assertEquals(Map.of("RETAIL_SCORE_FALLBACK", 5L, "STATEFUL_LEGACY", 16L, "NON_PRODUCTION", 1L),
			EXPECTED.values().stream().collect(Collectors.groupingBy(value -> value, Collectors.counting())));
	}
}
