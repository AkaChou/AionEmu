package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.AutoGroupData;
import com.aionemu.gameserver.dataholders.BindPointData;
import com.aionemu.gameserver.dataholders.ChestData;
import com.aionemu.gameserver.dataholders.CubeExpandData;
import com.aionemu.gameserver.dataholders.DynamicRiftData;
import com.aionemu.gameserver.dataholders.InstanceBuffData;
import com.aionemu.gameserver.dataholders.InstanceCooltimeData;
import com.aionemu.gameserver.dataholders.InstanceExitData;
import com.aionemu.gameserver.dataholders.InstanceRiftData;
import com.aionemu.gameserver.dataholders.FlyPathData;
import com.aionemu.gameserver.dataholders.FlyRingData;
import com.aionemu.gameserver.dataholders.GatherableData;
import com.aionemu.gameserver.dataholders.GoodsListData;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.dataholders.NpcDropData;
import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.dataholders.PortalLocData;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.dataholders.RiftData;
import com.aionemu.gameserver.dataholders.RoadData;
import com.aionemu.gameserver.dataholders.ReviveInstanceStartPointsData;
import com.aionemu.gameserver.dataholders.ReviveWorldStartPointsData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.StaticData;
import com.aionemu.gameserver.dataholders.TeleLocationData;
import com.aionemu.gameserver.dataholders.TeleporterData;
import com.aionemu.gameserver.dataholders.TradeListData;
import com.aionemu.gameserver.dataholders.XMLQuests;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.dataholders.WarehouseExpandData;
import com.aionemu.gameserver.model.Race;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlDataLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void staticDataSectionNameIsOnlyReportedForTopLevelStaticDataChildren() {
		Object section = new Object();

		assertEquals("Object", XmlDataLoader.staticDataSectionName(section, new StaticData()));
		assertNull(XmlDataLoader.staticDataSectionName(section, new Object()));
		assertNull(XmlDataLoader.staticDataSectionName(null, new StaticData()));
	}

	@Test
	void staticDataSectionCountUsesTopLevelXmlElements() {
		assertTrue(XmlDataLoader.staticDataSectionCount() > 0);
		assertEquals(StaticData.class.getFields().length, XmlDataLoader.staticDataSectionCount());
	}

	@Test
	void staticDataSectionEntryCountsUseDirectChildrenOfTopLevelElements() throws Exception {
		Path staticData = tempDir.resolve("static_data.xml");
		Files.writeString(staticData, """
			<ae_static_data>
				<item_templates>
					<item_template id="1"/>
					<item_template id="2"/>
				</item_templates>
				<npc_drops>
					<npc_drop npc_id="1"/>
					<npc_drop npc_id="2"/>
					<npc_drop npc_id="3"/>
				</npc_drops>
				<global_rules/>
			</ae_static_data>
			""", StandardCharsets.UTF_8);

		Map<String, Integer> counts = XmlDataLoader.staticDataSectionEntryCounts(staticData.toFile());

		assertEquals(2, counts.get("ItemData"));
		assertEquals(3, counts.get("NpcDropData"));
		assertEquals(1, counts.get("GlobalDropData"));
	}

	@Test
	void loadSectionEntryCountsSkipsXmlScanByDefault() throws Exception {
		Path staticData = tempDir.resolve("static_data.xml");
		Files.writeString(staticData, "<not xml", StandardCharsets.UTF_8);

		boolean previous = GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE;
		GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = false;
		try {
			Map<String, Integer> counts = new XmlDataLoader().loadSectionEntryCounts(staticData.toFile());

			assertEquals(XmlDataLoader.staticDataSectionCount(), counts.size());
			assertFalse(Files.exists(tempDir.resolve("static_data.counts")));
		} finally {
			GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = previous;
		}
	}

	@Test
	void consoleReporterOnlyPrintsElapsedTimeWhenEntryCountsAreDisabled() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		boolean previous = GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE;
		GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = false;
		try {
			StaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(new PrintStream(output), true);

			reporter.start(10);
			reporter.sectionProgress(1, 10, "ItemData", 1, 1);
			reporter.sectionFinished(1, 10, "ItemData", 1);
			reporter.finish(10, 1234);

			assertEquals("Loaded static data in 1234 ms%n".formatted(), output.toString());
		} finally {
			GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = previous;
		}
	}

	@Test
	void slowestSectionTimingsAreSortedAndLimited() {
		Map<String, Long> timings = new LinkedHashMap<>();
		timings.put("NpcData", 700L);
		timings.put("ItemData", 1200L);
		timings.put("SpawnsData2", 900L);

		List<Map.Entry<String, Long>> slowest = XmlDataLoader.slowestSectionTimings(timings, 2);

		assertEquals("ItemData", slowest.get(0).getKey());
		assertEquals(1200L, slowest.get(0).getValue());
		assertEquals("SpawnsData2", slowest.get(1).getKey());
		assertEquals(2, slowest.size());
	}

	@Test
	void staticDataJaxbContextIgnoresRuntimeLookupCaches() {
		assertDoesNotThrow(() -> JAXBContext.newInstance(StaticData.class));
	}

	@Test
	void staticDataUnmarshallerDoesNotInstallSynchronousSchemaValidation() throws Exception {
		Unmarshaller unmarshaller = new XmlDataLoader()
			.createStaticDataUnmarshaller(StaticDataProgressReporter.noop(), 0, Map.of());

		assertNull(unmarshaller.getSchema());
	}

	@Test
	void staticDataUnmarshallerCreatesJaxbContextWhenThreadContextClassLoaderCannotSeeJaxbRuntime() throws Exception {
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
		try {
			assertDoesNotThrow(() -> new XmlDataLoader()
				.createStaticDataUnmarshaller(StaticDataProgressReporter.noop(), 0, Map.of()));
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}

	@Test
	void staticDataSchemaCompiles() {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		assertDoesNotThrow(() -> schemaFactory.newSchema(Path.of("src/main/resources/aion/data/static_data/static_data.xsd").toFile()));
	}

	@Test
	void skillTemplatesValidateAgainstSchema() {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		assertDoesNotThrow(() -> schemaFactory
			.newSchema(Path.of("src/main/resources/aion/definitions/skills/skills.xsd").toFile())
			.newValidator()
			.validate(new StreamSource(Path.of("src/main/resources/aion/definitions/skills/skill_templates.xml").toFile())));
	}

	@Test
	void xmlMergerReportsWhetherCacheWasRebuilt() throws Exception {
		Path source = tempDir.resolve("static_data.xml");
		Path cache = tempDir.resolve("cache/static_data.xml");
		Files.createDirectories(cache.getParent());
		Files.writeString(source, "<ae_static_data><global_rules/></ae_static_data>", StandardCharsets.UTF_8);
		XmlMerger merger = new XmlMerger(source.toFile(), cache.toFile(), tempDir.toFile());

		assertTrue(merger.process());
		assertFalse(merger.process());
	}

	@Test
	void mainStaticDataDoesNotImportDefinitionsNpcDropsIntoSharedCache() throws Exception {
		String staticData = Files.readString(Path.of("src/main/resources/aion/data/static_data/static_data.xml"), StandardCharsets.UTF_8);

		assertFalse(staticData.contains("<npc_drops>"));
		assertFalse(staticData.contains("file=\"npc_drops/"));
		assertTrue(!staticData.contains("<item_templates>"));
		assertTrue(!staticData.contains("file=\"items/item\""));
	}

	@Test
	void npcDropsLoadFromConfiguredDefinitionsDirectory() throws Exception {
		Path drops = tempDir.resolve("npc_drops");
		Files.createDirectories(drops);
		Files.writeString(drops.resolve("npc_drops_part_001.xml"), """
			<npc_drops><npc_drop npc_id="100"/></npc_drops>
			""", StandardCharsets.UTF_8);
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", tempDir.toString());
		try {
			NpcDropData data = new XmlDataLoader().loadNpcDropData();

			assertEquals(1, data.size());
			assertEquals(100, data.getDrop(100).getNpcId());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedNpcDefinitionsLoadFromConfiguredDirectory() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			NpcData data = new XmlDataLoader().loadNpcData();

			assertEquals(87961, data.size());
			assertEquals("DRAGON_CASTLE_DOOR", data.getNpcTemplate(256694).getRace().name());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedSkillDefinitionsLoadWithCooldownGroups() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			SkillData data = new XmlDataLoader().loadSkillData();

			assertEquals(14480, data.size());
			assertNotNull(data.getSkillTemplate(1));
			assertTrue(data.getSkillsForDelayId(792).contains(1));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedQuestDefinitionsAndScriptsLoadFromConfiguredDirectory() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			QuestsData quests = loader.loadQuestData();
			XMLQuests scripts = loader.loadQuestScripts();

			assertEquals(6424, quests.size());
			assertNotNull(quests.getQuestById(1000));
			assertEquals(3813, scripts.getQuest().size());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedRecipeDefinitionsLoadFromConfiguredDirectory() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			RecipeData data = new XmlDataLoader().loadRecipeData();

			assertEquals(14540, data.size());
			assertNotNull(data.getRecipeTemplateById(155000001));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedPortalDefinitionsLoadFromConfiguredDirectory() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			PortalLocData locations = loader.loadPortalLocData();
			Portal2Data portals = loader.loadPortal2Data();

			assertEquals(548, locations.size());
			assertNotNull(locations.getPortalLoc(1100100));
			assertEquals(826, portals.size());
			assertTrue(portals.isPortalNpc(730197));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedInstanceDefinitionsPreserveRuntimeValues() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			InstanceCooltimeData cooltimes = loader.loadInstanceCooltimeData();
			InstanceBuffData buffs = loader.loadInstanceBuffData();
			InstanceExitData exits = loader.loadInstanceExitData();

			assertEquals(110, cooltimes.size());
			assertEquals(5, cooltimes.getInstanceCooltimeByWorldId(310090000).getMaxEntriesCount());
			assertEquals(18, buffs.size());
			assertEquals(900, buffs.getInstanceBonusattr(7).getPenaltyAttr().getFirst().getValue());
			assertEquals(242, exits.size());
			assertEquals(210020000, exits.getInstanceExit(300030000, Race.ELYOS).getExitWorld());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedRiftDefinitionsLoadFromConfiguredDirectory() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			DynamicRiftData dynamicRifts = loader.loadDynamicRiftData();
			InstanceRiftData instanceRifts = loader.loadInstanceRiftData();
			RiftData rifts = loader.loadRiftData();

			assertEquals(6, dynamicRifts.size());
			assertEquals(9, instanceRifts.size());
			assertEquals(80, rifts.size());
			assertEquals(210020000, rifts.getRiftLocations().get(2120).getWorldId());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedAutoGroupDefinitionsPreserveEntranceMappings() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			AutoGroupData data = new XmlDataLoader().loadAutoGroupData();

			assertEquals(130, data.size());
			assertEquals(300110000, data.getTemplateByInstaceMaskId(1).getInstanceId());
			assertTrue(data.getTemplateByInstaceMaskId(1).getNpcIds().contains(279039));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedReviveStartPointsPreserveCoordinatesAndSelection() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			ReviveWorldStartPointsData worlds = loader.loadReviveWorldStartPointsData();
			ReviveInstanceStartPointsData instances = loader.loadReviveInstanceStartPointsData();

			assertEquals(26, worlds.size());
			assertEquals(854.45807f, worlds.getReviveStartPoint(210010000, Race.ELYOS, 75).getX());
			assertEquals(92, instances.size());
			assertEquals(513f, instances.getReviveStartPoint(300030000).getX());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedWorldMapsPreserveRuntimeGeometryAndFlags() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			WorldMapsData data = new XmlDataLoader().loadWorldMapsData();

			assertEquals(185, data.size());
			assertEquals(3072, data.getTemplate(210020000).getWorldSize());
			assertTrue(data.getTemplate(210020000).isFly());
			assertTrue(data.getTemplate(300030000).isInstance());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedTransportDefinitionsPreserveNpcLocationsAndFlightPaths() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			BindPointData bindPoints = loader.loadBindPointData();
			TeleporterData teleporters = loader.loadTeleporterData();
			TeleLocationData locations = loader.loadTeleLocationData();
			FlyPathData flyPaths = loader.loadFlyPathData();

			assertEquals(139, bindPoints.size());
			assertEquals(40, bindPoints.getBindPointTemplate(700013).getPrice());
			assertEquals(363, teleporters.size());
			assertTrue(teleporters.getTeleporterTemplateByTeleportId(1).containNpc(203726));
			assertEquals(357, locations.size());
			assertEquals(110010000, locations.getTelelocationTemplate(2).getMapId());
			assertEquals(316, flyPaths.size());
			assertEquals(310020000, flyPaths.getPathTemplate((byte) 1).getStartWorldId());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedWorldMovementDefinitionsPreserveCoordinates() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			FlyRingData flyRings = loader.loadFlyRingData();
			RoadData roads = loader.loadRoadData();

			assertEquals(72, flyRings.size());
			assertEquals(400010000, flyRings.getFlyRingTemplates().getFirst().getMap());
			assertEquals(8, roads.size());
			assertEquals(210030000, roads.getRoadTemplates().getFirst().getMap());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedStorageExpansionDefinitionsPreserveNpcPrices() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			CubeExpandData cube = loader.loadCubeExpandData();
			WarehouseExpandData warehouse = loader.loadWarehouseExpandData();

			assertEquals(11, cube.size());
			assertEquals(1000, cube.getCubeExpandListTemplate(798008).get(1).getPrice());
			assertEquals(267, warehouse.size());
			assertEquals(1200, warehouse.getWarehouseExpandListTemplate(203221).get(1).getPrice());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedWorldResourceDefinitionsPreserveKeysAndMaterials() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			ChestData chests = loader.loadChestData();
			GatherableData gatherables = loader.loadGatherableData();

			assertEquals(358, chests.size());
			assertEquals(185000263, chests.getChestTemplate(806220).getKeyItem().getFirst().getItemId());
			assertEquals(761, gatherables.size());
			assertEquals(152000006, gatherables.getGatherableTemplate(400007).getMaterials().getMaterial().getFirst().getItemid());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void migratedNpcShopDefinitionsPreserveGoodsAndNpcTabs() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			GoodsListData goods = loader.loadGoodsListData();
			TradeListData trades = loader.loadTradeListData();

			assertEquals(3898, goods.size());
			assertEquals(110100010, goods.getGoodsListById(129).getItemIdList().getFirst());
			assertEquals(2461, trades.size());
			assertEquals(129, trades.getTradeListTemplate(203060).getTradeTablist().getFirst().getId());
			assertNotNull(trades.getTradeInListTemplate(205315));
			assertNotNull(trades.getPurchaseListTemplate(206352));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void itemDataCanBeLoadedFromSeparateMergedCache() throws Exception {
		Path itemDir = tempDir.resolve("item");
		Path cache = tempDir.resolve("cache/item_templates.xml");
		Files.createDirectories(itemDir);
		Files.createDirectories(cache.getParent());
		Files.writeString(itemDir.resolve("item_misc_templates.xml"), """
			<item_templates>
				<item_template id="100000001" restrict="1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"/>
			</item_templates>
			""", StandardCharsets.UTF_8);

		ItemData itemData = new XmlDataLoader().loadItemData(cache.toFile(), tempDir.toFile());

		assertEquals(1, itemData.size());
		assertTrue(Files.readString(cache, StandardCharsets.UTF_8).contains("<item_template"));
	}

	@Test
	void migratedItemDefinitionsLoadFromSeparateCache() {
		Path cache = tempDir.resolve("cache/item_templates.xml");
		ItemData itemData = new XmlDataLoader().loadItemData(cache.toFile(),
			Path.of("src/main/resources/aion/definitions/items").toFile());

		assertEquals(128629, itemData.size());
		assertNotNull(itemData.getItemTemplate(100000001));
	}

	@Test
	void itemDataCreatesJaxbContextWhenThreadContextClassLoaderCannotSeeJaxbRuntime() throws Exception {
		Path itemDir = tempDir.resolve("item");
		Path cache = tempDir.resolve("cache/item_templates.xml");
		Files.createDirectories(itemDir);
		Files.createDirectories(cache.getParent());
		Files.writeString(itemDir.resolve("item_misc_templates.xml"), """
			<item_templates>
				<item_template id="100000001" restrict="1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"/>
			</item_templates>
			""", StandardCharsets.UTF_8);
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
		try {
			ItemData itemData = new XmlDataLoader().loadItemData(cache.toFile(), tempDir.toFile());

			assertEquals(1, itemData.size());
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}
}
