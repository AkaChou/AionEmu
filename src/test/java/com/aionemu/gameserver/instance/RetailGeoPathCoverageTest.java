package com.aionemu.gameserver.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class RetailGeoPathCoverageTest {

	private static final Path COVERAGE = Path.of("src/main/resources/aion/definitions/compact/instance/coverage.xml");
	private static final Path GEO = Path.of("src/main/resources/aion/geo");
	private static final Path WAYPOINTS = Path.of("src/main/resources/aion/definitions/compact/ai/ai-waypoints.xml");
	private static final Pattern ROUTE_WORLD = Pattern.compile("route_id=\\\"retail:(\\d+):");

	@Test
	void locksProductionGeoAndPathResourceCoverage() throws Exception {
		Set<Integer> production = new HashSet<>();
		var worlds = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(COVERAGE.toFile())
			.getElementsByTagName("world");
		for (int i = 0; i < worlds.getLength(); i++) {
			var world = worlds.item(i).getAttributes();
			if (!"EXCLUDED_NON_PRODUCTION".equals(world.getNamedItem("behavior").getNodeValue())) {
				production.add(Integer.parseInt(world.getNamedItem("id").getNodeValue()));
			}
		}

		Set<Integer> geo = new HashSet<>();
		try (var files = Files.list(GEO)) {
			files.map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith(".geo.gz"))
				.flatMap(name -> java.util.Arrays.stream(name.substring(0, name.length() - 7).split(",")))
				.map(Integer::parseInt).forEach(geo::add);
		}
		Set<Integer> missingGeo = new TreeSet<>(production);
		missingGeo.removeAll(geo);
		assertEquals(137, production.size());
		assertEquals(137, production.stream().filter(geo::contains).count());
		assertEquals(Set.of(), missingGeo);

		Set<Integer> waypointWorlds = new HashSet<>();
		String waypointXml = Files.readString(WAYPOINTS);
		Matcher matcher = ROUTE_WORLD.matcher(waypointXml);
		while (matcher.find()) {
			waypointWorlds.add(Integer.parseInt(matcher.group(1)));
		}
		Set<Integer> productionWithWaypoints = new HashSet<>(production);
		productionWithWaypoints.retainAll(waypointWorlds);
		Set<Integer> missingWaypoints = new TreeSet<>(production);
		missingWaypoints.removeAll(waypointWorlds);
		assertEquals(78, productionWithWaypoints.size());
		assertEquals(59, missingWaypoints.size());
		assertEquals("caacc7c6852d30cd1a5d5f989458aaf35450d2c09c9a7a0945c43083f7837ed4",
			sha256(missingWaypoints));
	}

	private static String sha256(Set<Integer> values) throws Exception {
		String report = values.stream().map(String::valueOf).sorted().reduce((left, right) -> left + "\n" + right).orElse("");
		return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
			.digest(report.getBytes(StandardCharsets.UTF_8)));
	}
}
