package com.aionemu.gameserver.dataholders.loadingutils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.*;
import com.aionemu.gameserver.lifecycle.TestServiceProviders;
import com.aionemu.gameserver.services.MotionLoggingService;
import com.aionemu.testutil.ConfigSnapshot;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class XmlDataLoaderTest {

	@BeforeEach
	void installRetiredServiceProvider() {
		TestServiceProviders.install(MotionLoggingService.class, new MotionLoggingService());
	}

	@AfterEach
	void clearRetiredServiceProvider() {
		TestServiceProviders.clear(MotionLoggingService.class);
	}

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
		long xmlElements = Arrays.stream(StaticData.class.getFields())
			.filter(field -> field.getAnnotation(XmlElement.class) != null).count();

		assertEquals(xmlElements, XmlDataLoader.staticDataSectionCount());
	}

	@Test
	void templateShardStartIdUsesDeclaredFilenameRangeWithoutReadingTheFile() {
		Pattern pattern = Pattern.compile("item_template_(\\d+)_(\\d+)\\.xml");

		assertEquals(100000001L, XmlDataLoader.templateShardStartId(
			new File("missing/item_template_100000001_100601382.xml"), pattern, "item"));
	}

	@Test
	void largestFirstShardDispatchPreservesInputOrderAndPropagatesShardIdentity() throws Exception {
		Path first = tempDir.resolve("first.xml");
		Path second = tempDir.resolve("second.xml");
		Path third = tempDir.resolve("third.xml");
		Files.writeString(first, "<first/>");
		Files.writeString(second, "<second>".repeat(32));
		Files.writeString(third, "<third/>");
		List<File> files = List.of(first.toFile(), second.toFile(), third.toFile());

		List<String> loaded = XmlDataLoader.submitLargestFirst(files, File::getName);

		assertEquals(List.of("first.xml", "second.xml", "third.xml"), loaded);
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> XmlDataLoader.submitLargestFirst(files, file -> {
				if (file.getName().equals("second.xml")) {
					throw new IllegalArgumentException("broken second");
				}
				return file.getName();
			}));
		assertTrue(error.getMessage().contains("second.xml"));
		assertTrue(error.getCause() instanceof IllegalArgumentException);
		assertEquals("broken second", error.getCause().getMessage());
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
	void loadSectionEntryCountsUsesDefaultsWhenEntryCountsAreDisabled() throws Exception {
		byte[] staticData = "<not xml".getBytes(StandardCharsets.UTF_8);

		ConfigSnapshot snapshot = ConfigSnapshot.of(GSConfig.class, "STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE");
		try {
			GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = false;
			Map<String, Integer> counts = new XmlDataLoader().loadSectionEntryCounts(staticData);

			assertEquals(XmlDataLoader.staticDataSectionCount(), counts.size());
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void consoleReporterStaysSilentWhenEntryCountsAreDisabled() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(GSConfig.class, "STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE");
		Logger consoleLogger = (Logger) LoggerFactory.getLogger("aion.console");
		Level previousLevel = consoleLogger.getLevel();
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		appender.start();
		consoleLogger.addAppender(appender);
		consoleLogger.setLevel(Level.DEBUG);
		try {
			GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = false;
			StaticDataProgressReporter reporter = new ConsoleStaticDataProgressReporter(true);

			reporter.start(10);
			reporter.sectionProgress(1, 10, "ItemData", 1, 1);
			reporter.sectionFinished(1, 10, "ItemData", 1);
			reporter.finish(10, 1234);

			// 耗时由 DataManager 的权威结果行输出，报告器在禁用条目计数时保持静默。 / Timing is owned by DataManager; the reporter stays silent when entry counts are disabled.
			assertTrue(appender.list.isEmpty());
		} finally {
			consoleLogger.detachAppender(appender);
			appender.stop();
			consoleLogger.setLevel(previousLevel);
			snapshot.restore();
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
	void sharedNpcMappingStagesInvokeSourceOnlyOnceForBothConsumers() {
		AtomicInteger sourceLoads = new AtomicInteger();
		var mappings = new RetailAiDefinitionLoader.NpcMappings(Map.of(), Map.of());
		var stages = XmlDataLoader.startNpcMappingStages(() -> {
			sourceLoads.incrementAndGet();
			return mappings;
		}, new ConcurrentHashMap<>());
		var retailMappings = stages.source().thenApply(RetailAiDefinitionLoader.NpcMappings::npcs);

		assertEquals(Map.of(), retailMappings.join());
		assertEquals(0, stages.pathBehaviors().join().size());
		assertEquals(1, sourceLoads.get());
	}

	@Test
	void staticDataJaxbContextIgnoresRuntimeLookupCaches() {
		assertDoesNotThrow(() -> JAXBContext.newInstance(StaticData.class));
	}

	@Test
	void staticDataLoaderUsesStreamingBinderWithoutJaxbRuntime() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/dataholders/loadingutils/XmlDataLoader.java"));

		assertTrue(source.contains("StreamingXmlBinder.unmarshal"));
		assertFalse(source.contains("createUnmarshaller"));
		assertFalse(source.contains("JAXBContext"));
	}

	@Test
	void independentSourceRootsUnmarshalIntoTheirDataHolders() throws Exception {
		assertEquals(BindPointData.class, JAXBContext.newInstance(BindPointData.class)
			.createUnmarshaller()
			.unmarshal(Path.of("src/main/resources/aion/data/static_data/bind_points/bind_points.xml").toFile())
			.getClass());
		assertEquals(ItemRestrictionCleanupData.class, JAXBContext.newInstance(ItemRestrictionCleanupData.class)
			.createUnmarshaller()
			.unmarshal(Path.of("src/main/resources/aion/data/static_data/items/item_restriction_cleanups.xml").toFile())
			.getClass());
		assertEquals(HouseBuildingData.class, JAXBContext.newInstance(HouseBuildingData.class)
			.createUnmarshaller()
			.unmarshal(Path.of("src/main/resources/aion/data/static_data/housing/house_buildings.xml").toFile())
			.getClass());
	}

	@Test
	void staticDataSchemaCompiles() {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		assertDoesNotThrow(() -> schemaFactory.newSchema(Path.of("src/main/resources/aion/data/static_data/static_data.xsd").toFile()));
	}

	@Test
	void staticDataEntryPointValidatesAgainstSchema() {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		assertDoesNotThrow(() -> schemaFactory
			.newSchema(Path.of("src/main/resources/aion/data/static_data/static_data.xsd").toFile())
			.newValidator()
			.validate(new StreamSource(Path.of("src/main/resources/aion/data/static_data/static_data.xml").toFile())));
	}

	@Test
	void questDataSynthesizesFromProductionCatalog() {
		com.aionemu.gameserver.dataholders.QuestsData quests = com.aionemu.gameserver.dataholders.QuestsData.fromCatalog(
			com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions.catalog());
		// 生产目录自 P7 步 f 起为直通 overlay：catalog() = XML 目录条目（保留清单 6217 − 原生覆盖 5484）。
		// The production catalog is a pass-through overlay since P7 step f: catalog() = XML directory
		// entries (retention manifest 6217 − native-covered 5484).
		assertEquals(733, quests.size());
		assertNotNull(quests.getQuestById(1000));
		assertNotNull(quests.getQuestById(1001));
		assertNotNull(quests.getQuestById(1002));
	}

	@Test
	void aiDefinitionsValidateAgainstSchema() {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Path schema = Path.of("src/main/resources/aion/definitions/schemas/ai.xsd");

		assertDoesNotThrow(() -> {
			var validator = schemaFactory.newSchema(schema.toFile()).newValidator();
			validator.validate(new StreamSource(Path.of("src/main/resources/aion/definitions/compact/ai/bombs.xml").toFile()));
			validator.validate(new StreamSource(Path.of("src/main/resources/aion/definitions/compact/ai/spawn_helpers.xml").toFile()));
		});
	}

	@Test
	void mainStaticDataKeepsIndependentSourcesOutOfEntryPoint() throws Exception {
		String staticData = Files.readString(Path.of("src/main/resources/aion/data/static_data/static_data.xml"), StandardCharsets.UTF_8);

		assertFalse(staticData.contains("<npc_drops>"));
		assertFalse(staticData.contains("file=\"npc_drops/"));
		assertFalse(staticData.contains("<item_templates>"));
		assertFalse(staticData.contains("file=\"items/item\""));
		assertFalse(staticData.contains("<ai_templates>"));
		assertFalse(staticData.contains("file=\"ai"));
		// NPC 模板同样走源分片直读 / NPC templates also load directly from source shards
		assertFalse(staticData.contains("<npc_templates>"));
		assertFalse(staticData.contains("file=\"npcs\""));
	}

	@Test
	void npcDropsLoadDirectlyFromCompactBundle() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			assertNotNull(new XmlDataLoader().loadNpcDropData().getDrop(883526));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void skillSupportDefinitionsLoadDirectlyFromCompactBundle() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			MotionData motions = loader.loadMotionData();
			ChargeSkillData chargeSkills = loader.loadChargeSkillData();

			assertEquals(333, motions.size());
			assertNotNull(motions.getMotionTime("areaatk"));
			assertNotNull(motions.getMotionTime("FIAreaATK"));
			assertEquals(169, chargeSkills.size());
			assertNotNull(chargeSkills.getChargeSkillTemplateById(1));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void aiDefinitionsLoadDirectlyFromCompactBundle() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			AIData data = new XmlDataLoader().loadAiData();

			assertEquals(101, data.size());
			assertNotNull(data.getAiTemplate().get(207504).getBombs());
			assertNotNull(data.getAiTemplate().get(211715).getSummons());
			assertFalse(Files.exists(Path.of("src/main/resources/aion/definitions/compact/ai/skill-categories.xml")));
			assertTrue(Files.exists(Path.of("src/main/resources/aion/definitions/compact/skills/skill-categories.xml")));
			assertFalse(Files.exists(Path.of("src/main/resources/aion/data/static_data/ai/bombs.xml")));
			assertFalse(Files.exists(Path.of("src/main/resources/aion/data/static_data/ai/spawn_helpers.xml")));
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void petUtilityDefinitionsLoadDirectlyFromCompactBundle() {
		String previous = System.getProperty("aion.game.definitions.dir");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		try {
			XmlDataLoader loader = new XmlDataLoader();
			PetDopingData doping = loader.loadPetDopingData();
			PetMerchandData merchants = loader.loadPetMerchandData();

			assertEquals(33, doping.size());
			assertTrue(doping.getDopingTemplate((short) 1).isUseDrink());
			assertEquals(6, doping.getDopingTemplate((short) 2).getScrollsUsed());
			assertEquals(5, merchants.size());
			assertEquals(21, merchants.getMerchandTemplate(5).getRatePrice());
		} finally {
			if (previous == null) {
				System.clearProperty("aion.game.definitions.dir");
			} else {
				System.setProperty("aion.game.definitions.dir", previous);
			}
		}
	}

	@Test
	void itemDataLoadsFromParallelShardsWithCustomOverrides() throws Exception {
		Path itemDir = tempDir.resolve("item");
		Files.createDirectories(itemDir);
		Files.writeString(itemDir.resolve("item_template_100000001_100000002.xml"), """
			<item_templates>
				<item_template id="100000001" name="Display reward item" name_desc="retail_reward_item" restrict="1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"/>
				<item_template id="100000002" name="Second item" restrict="1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"/>
			</item_templates>
			""", StandardCharsets.UTF_8);
		Path custom = tempDir.resolve("item_template_custom.xml");
		Files.writeString(custom, """
			<item_templates>
				<item_template id="100000001" name="Customized reward item" name_desc="retail_reward_item" restrict="1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"/>
				<item_template id="100000099" name="Brand new item" restrict="1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"/>
			</item_templates>
			""", StandardCharsets.UTF_8);

		ItemData itemData = new XmlDataLoader().loadItemData(itemDir.toFile(), custom.toFile());

		assertEquals(3, itemData.size());
		assertEquals(100000001, itemData.getItemTemplate("CUSTOMIZED REWARD ITEM").getTemplateId());
		assertEquals(100000099, itemData.getItemTemplate("brand new item").getTemplateId());
		assertNull(itemData.getItemTemplate("display reward item"), "overridden template names must not survive");
		Path shard = itemDir.resolve("item_template_100000001_100000002.xml");
		assertTrue(Files.readString(shard, StandardCharsets.UTF_8).contains("<item_template"));
		assertFalse(Files.exists(tempDir.resolve("cache")), "direct shard loading must not create a cache directory");
	}

	@Test
	void npcDataLoadsFromParallelShards() throws Exception {
		Path shardDir = tempDir.resolve("npc_shards");
		Files.createDirectories(shardDir);
		Files.writeString(shardDir.resolve("npc_template_250001_250002.xml"), """
			<npc_templates>
				<npc_template npc_id="250001" name="first"/>
				<npc_template npc_id="250002" name="second"/>
			</npc_templates>
			""", StandardCharsets.UTF_8);
		NpcData data = new XmlDataLoader().loadNpcDataSharded(shardDir.toFile());

		assertEquals(2, data.size());
		assertNotNull(data.getNpcTemplate(250001));
		assertNotNull(data.getNpcTemplate(250002));
		assertFalse(Files.exists(tempDir.resolve("cache")), "direct shard loading must not create a cache directory");
	}

	/**
	 * 锁定分片解析只绑定模板列表：容器级索引必须仅在合并完成后建立一次。
	 * Locks shard parsing to template lists only: container indexes must be built once after merging.
	 * @throws Exception 读取失败 / on read failure
	 */
	@Test
	void templateShardsDeferContainerIndexesUntilMergeCompletes() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/dataholders/loadingutils/XmlDataLoader.java"));
		String itemLoader = source.substring(source.indexOf("ItemData loadItemData(File itemShardDir"),
			source.indexOf("\t/**\n\t * 直接从源分片并行加载 NPC 模板", source.indexOf("ItemData loadItemData(File itemShardDir")));
		String npcLoader = source.substring(source.indexOf("NpcData loadNpcDataSharded(File shardDir)"),
			source.indexOf("\t/**\n\t * 枚举已提交的源分片", source.indexOf("NpcData loadNpcDataSharded(File shardDir)")));

		assertFalse(itemLoader.contains("unmarshalShard(shard, ItemData.class"));
		assertFalse(npcLoader.contains("unmarshalShard(shard, NpcData.class"));
		assertTrue(itemLoader.contains("ItemTemplateShard.class"));
		assertTrue(npcLoader.contains("NpcTemplateShard.class"));
	}

	/**
	 * 锁定技能分片使用轻量包装，而不是为每个分片构造完整 SkillData 索引。
	 * Locks skill parts to the lightweight wrapper instead of full SkillData indexes per part.
	 * @throws Exception 读取失败 / on read failure
	 */
	@Test
	void skillPartsUseLightweightWrapperInsteadOfFullSkillData() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/dataholders/loadingutils/SkillDefinitionLoader.java"));

		assertTrue(source.contains("StreamingXmlBinder.unmarshal(reader, SkillTemplate.class)"));
		assertFalse(source.contains("createJaxbContext(SkillData.class)"));
		assertFalse(source.contains("SkillPart data = (SkillPart)"));
	}

	@Test
	void itemDataCreatesJaxbContextWhenThreadContextClassLoaderCannotSeeJaxbRuntime() throws Exception {
		Path itemDir = tempDir.resolve("item");
		Files.createDirectories(itemDir);
		Files.writeString(itemDir.resolve("item_template_100000001_100000001.xml"), """
			<item_templates>
				<item_template id="100000001" restrict="1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"/>
			</item_templates>
			""", StandardCharsets.UTF_8);
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
		try {
			ItemData itemData = new XmlDataLoader().loadItemData(itemDir.toFile(), null);

			assertEquals(1, itemData.size());
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}
}
