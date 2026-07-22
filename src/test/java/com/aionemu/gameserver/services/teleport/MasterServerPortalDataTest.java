package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class MasterServerPortalDataTest {
	private static final Path DATA = Path.of("src/main/resources/aion/data/static_data");

	@Test
	void masterEntrancesUseLevelCompatibleClientNpcsAndPortalBindings() throws Exception {
		var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document inggison = builder.parse(DATA.resolve("spawns/Npcs/210130000_Inggison [Master Server].xml").toFile());
		Document gelkmaros = builder.parse(DATA.resolve("spawns/Npcs/220140000_Gelkmaros [Master Server].xml").toFile());
		Document silentera = builder.parse(DATA.resolve("spawns/Npcs/600110000_Silentera_Canyon [Master Server].xml").toFile());
		Document npcs = builder.parse(DATA.resolve("npcs/npc_template.xml").toFile());
		Document portals = builder.parse(DATA.resolve("portals/portal_template2.xml").toFile());

		assertTrue(exists(inggison, "//spawn[@npc_id='730277']/spot[@entity_id='127']"));
		assertTrue(exists(inggison, "//spawn[@npc_id='730279']/spot[@entity_id='443']"));
		assertTrue(exists(inggison, "//spawn[@npc_id='730485']/spot[@entity_id='1315']"));
		assertTrue(exists(gelkmaros, "//spawn[@npc_id='730278']/spot[@entity_id='1589']"));
		assertTrue(exists(gelkmaros, "//spawn[@npc_id='730280']/spot[@entity_id='1588']"));
		assertTrue(exists(gelkmaros, "//spawn[@npc_id='730486']/spot[@entity_id='1235']"));
		assertTrue(exists(silentera, "//spawn[@npc_id='730231']/spot[@entity_id='33']"));
		assertTrue(exists(npcs, "//npc_template[@npc_id='730231' and @ai='beshmundirswalk']"));
		assertTrue(exists(portals,
					"//portal_use[@npc_id='730231']/portal_path[@loc_id='3001700' and @instance='true']/portal_req[@min_level='53']"));
		assertTrue(exists(portals, "//portal_dialog[@npc_id='730277']/portal_path[@loc_id='3001500']"));
		assertTrue(exists(portals, "//portal_dialog[@npc_id='730279']/portal_path[@loc_id='3001600']"));
		assertTrue(exists(portals, "//portal_dialog[@npc_id='730485']/portal_path[@loc_id='3201500']"));
		assertTrue(exists(portals, "//portal_dialog[@npc_id='730278']/portal_path[@loc_id='3001500']"));
			assertTrue(exists(portals, "//portal_dialog[@npc_id='730280']/portal_path[@loc_id='3001600']"));
			assertTrue(exists(portals, "//portal_dialog[@npc_id='730486']/portal_path[@loc_id='3201500']"));
			assertTrue(exists(portals, "//portal_use[@npc_id='730211']/portal_path[@loc_id='4000108']"));
			assertTrue(exists(portals, "//portal_use[@npc_id='205437']/portal_path[@loc_id='2101312' and @race='ELYOS']"));
			assertTrue(exists(portals, "//portal_use[@npc_id='205437']/portal_path[@loc_id='2201412' and @race='ASMODIANS']"));
			assertTrue(exists(portals, "//portal_use[@npc_id='831746']/portal_path[@loc_id='1100105' and @race='ELYOS']"));
			assertTrue(exists(portals, "//portal_use[@npc_id='831746']/portal_path[@loc_id='1200107' and @race='ASMODIANS']"));
			for (String npcId : new String[] { "831968", "831969" }) {
				assertTrue(exists(portals, "//portal_use[@npc_id='" + npcId + "']/portal_path[@loc_id='3012202']"));
			}
		}

	@Test
	void normal58WorldsAreEnabledAndKeepMapMarkerEntities() throws Exception {
		var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document worlds = builder.parse(DATA.resolve("world_maps.xml").toFile());
		Document inggison = builder.parse(DATA.resolve("spawns/Npcs/210050000_Inggison.xml").toFile());
		Document gelkmaros = builder.parse(DATA.resolve("spawns/Npcs/220070000_Gelkmaros.xml").toFile());
		Document silentera = builder.parse(DATA.resolve("spawns/Npcs/600010000_Silentera_Canyon.xml").toFile());

		assertTrue(exists(worlds, "//map[@id='210050000']"));
		assertTrue(exists(worlds, "//map[@id='220070000']"));
		assertTrue(exists(worlds, "//map[@id='600010000']"));
		assertTrue(exists(inggison, "//spawn[@npc_id='730277']/spot[@entity_id='127']"));
		assertTrue(exists(inggison, "//spawn[@npc_id='730279']/spot[@entity_id='443']"));
		assertTrue(exists(inggison, "//spawn[@npc_id='730485']/spot[@entity_id='1315']"));
		assertTrue(exists(gelkmaros, "//spawn[@npc_id='730278']/spot[@entity_id='1589']"));
		assertTrue(exists(gelkmaros, "//spawn[@npc_id='730280']/spot[@entity_id='1588']"));
		assertTrue(exists(gelkmaros, "//spawn[@npc_id='730486']/spot[@entity_id='1235']"));
		assertTrue(exists(silentera, "//spawn[@npc_id='730231']/spot[@entity_id='33']"));
		assertTrue(Files.isRegularFile(DATA.resolve("spawns/Outpost/210050000_Inggison.xml")));
		assertTrue(Files.isRegularFile(DATA.resolve("spawns/Outpost/220070000_Gelkmaros.xml")));
		assertTrue(Files.isRegularFile(DATA.resolve("zones/zones_210050000.xml")));
		assertTrue(Files.isRegularFile(DATA.resolve("zones/zones_220070000.xml")));
		assertTrue(Files.isRegularFile(DATA.resolve("zones/zones_600010000.xml")));
	}

	private static boolean exists(Document document, String expression) throws Exception {
		return (boolean) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.BOOLEAN);
	}
}
