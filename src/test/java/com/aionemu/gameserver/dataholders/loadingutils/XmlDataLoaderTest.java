package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

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
	void mainStaticDataDoesNotImportNpcDropsIntoSharedCache() throws Exception {
		String staticData = Files.readString(Path.of("src/main/resources/aion/game/data/static_data/static_data.xml"), StandardCharsets.UTF_8);

		assertTrue(!staticData.contains("<npc_drops>"));
		assertTrue(!staticData.contains("file=\"npc_drops\""));
	}
}
