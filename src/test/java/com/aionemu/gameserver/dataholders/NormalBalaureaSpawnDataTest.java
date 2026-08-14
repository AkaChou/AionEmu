package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class NormalBalaureaSpawnDataTest {

	private static final Path NPCS = Path.of("src/main/resources/aion/data/static_data/spawns/Npcs");

	@Test
	void keepsRestoredOpenWorldMovement() throws Exception {
		SpawnsData2.load(NPCS.toFile(), null);
		assertEquals(2527, movingSpots("210050000_Inggison.xml"));
		assertEquals(993, movingSpots("220070000_Gelkmaros.xml"));
		assertEquals(168, movingSpots("600010000_Silentera_Canyon.xml"));
		assertEquals(631, movingSpots("400010000_Reshanta.xml"));
		assertEquals(531, movingSpots("400020000_Belus.xml"));
		assertEquals(528, movingSpots("400040000_Aspida.xml"));
		assertEquals(527, movingSpots("400050000_Atanatos.xml"));
		assertEquals(527, movingSpots("400060000_Disillon.xml"));
		assertEquals(7, movingSpots("600040000_Tiamaranta_Eye.xml"));
		assertEquals(4140, movingSpots("600050000_Katalam.xml"));
	}

	private static int movingSpots(String fileName) throws Exception {
		var spots = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(NPCS.resolve(fileName).toFile())
			.getElementsByTagName("spot");
		int moving = 0;
		for (int i = 0; i < spots.getLength(); i++) {
			Element spot = (Element) spots.item(i);
			if (spot.hasAttribute("random_walk") || spot.hasAttribute("walker_id")) {
				moving++;
			}
		}
		return moving;
	}
}
