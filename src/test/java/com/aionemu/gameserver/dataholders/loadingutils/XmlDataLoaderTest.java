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
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.NpcDropData;
import com.aionemu.gameserver.dataholders.StaticData;
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
			.newSchema(Path.of("src/main/resources/aion/data/static_data/skills/skills.xsd").toFile())
			.newValidator()
			.validate(new StreamSource(Path.of("src/main/resources/aion/data/static_data/skills/skill_templates.xml").toFile())));
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
