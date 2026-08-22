package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.AIData;
import com.aionemu.gameserver.dataholders.ChargeSkillData;
import com.aionemu.gameserver.dataholders.HotspotLocationData;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.MotionData;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.dataholders.NpcDropData;
import com.aionemu.gameserver.dataholders.NpcPathBehaviorData;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.PetDopingData;
import com.aionemu.gameserver.dataholders.PetMerchandData;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.StaticData;
import com.aionemu.gameserver.dataholders.WalkerData;
import com.aionemu.gameserver.dataholders.WindstreamData;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 负责通过 JAXB 逐分区、逐源文件加载静态数据 XML，不生成或读取合并缓存。
 * Loads static-data XML section by section and source file by source file without creating or reading a merged cache.
 *
 * @author Luno
 */
@Slf4j
public class XmlDataLoader {

	private static volatile ObjectProvider<XmlDataLoader> instanceProvider;
	private static final String MAIN_XML_FILE = "./data/static_data/static_data.xml";
	private static final String ITEM_DATA_DIR = "./data/static_data/items";
	private static final String ITEM_SHARD_DIR = ITEM_DATA_DIR + "/item";
	private static final String NPC_DROP_DEFINITIONS_DIR = "./definitions/compact/npc_drops";
	private static final String NPC_COMBAT_DEFINITIONS_FILE = "./definitions/compact/npc-combat.xml";
	private static final String HOTSPOT_LOCATION_DEFINITIONS_FILE = "./definitions/compact/hotspot_location/hotspot_location.xml";
	private static final String SKILL_DEFINITIONS_DIR = "./definitions/compact/skills";
	private static final String NPC_SKILL_DEFINITIONS_FILE = SKILL_DEFINITIONS_DIR + "/npc-skills.xml";
	private static final String MOTION_DEFINITIONS_FILE = SKILL_DEFINITIONS_DIR + "/motion_times.xml";
	private static final String CHARGE_SKILL_DEFINITIONS_FILE = SKILL_DEFINITIONS_DIR + "/charge_skills.xml";
	private static final String AI_DEFINITIONS_DIR = "./definitions/compact/ai";
	private static final String AI_BOMBS_FILE = AI_DEFINITIONS_DIR + "/bombs.xml";
	private static final String AI_SPAWN_HELPERS_FILE = AI_DEFINITIONS_DIR + "/spawn_helpers.xml";
	private static final String NPC_PATH_BEHAVIOR_FILE = "./definitions/compact/ai/npc-ai.xml";
	private static final String RETAIL_NPC_AI_MAPPINGS_FILE = AI_DEFINITIONS_DIR + "/npc-ai.xml";
	private static final String RETAIL_AI_STRINGS_FILE = AI_DEFINITIONS_DIR + "/ai-strings.xml";
	private static final String RETAIL_AI_AREAS_FILE = AI_DEFINITIONS_DIR + "/ai-areas.xml";
	private static final String RETAIL_AI_LOCATION_ALIASES_FILE = AI_DEFINITIONS_DIR + "/ai-location-aliases.xml";
	private static final String RETAIL_DIRECT_PORTALS_FILE = AI_DEFINITIONS_DIR + "/direct-portals.xml";
	private static final String RETAIL_CONDITION_SPAWNS_FILE = AI_DEFINITIONS_DIR + "/condition-spawns.xml";
	private static final String RETAIL_SKILL_CATEGORIES_FILE = SKILL_DEFINITIONS_DIR + "/skill-categories.xml";
	private static final String RETAIL_NPC_SCORES_FILE = AI_DEFINITIONS_DIR + "/npc-scores.xml";
	private static final String RETAIL_GROUP_CONTROLLERS_FILE = AI_DEFINITIONS_DIR + "/group-controllers.xml";
	private static final String RETAIL_NPC_PARTIES_FILE = AI_DEFINITIONS_DIR + "/npc-parties.xml";
	private static final String RETAIL_DYNAMIC_AREAS_FILE = AI_DEFINITIONS_DIR + "/dynamic-areas.xml";
	private static final String RETAIL_AI_WAYPOINTS_FILE = AI_DEFINITIONS_DIR + "/ai-waypoints.xml";
	private static final String RETAIL_AI_WAYPOINTS_SCHEMA = "./definitions/schemas/ai-waypoints.xsd";
	private static final String PET_RIDES_DEFINITIONS_FILE = "./definitions/compact/pets-rides.xml";
	private static final String FLY_PATH_DEFINITIONS_FILE = "./definitions/compact/world/fly_path.xml";
	private static final String WIND_DEFINITIONS_FILE = "./definitions/compact/wind.xml";
	private static final String ID_DEFINITIONS_FILE = "./definitions/compact/id-mappings.xml";
	/** NPC 模板源分片目录 / NPC template source-shard directory */
	private static final String NPC_SHARD_DIR = "./data/static_data/npcs";
	/**
	 * 自定义物品模板覆盖文件（按 ID 覆盖或新增分片模板，不会被分片器改动） /
	 * Custom item override file (overrides or appends by id; never touched by the shard writer).
	 */
	private static final String CUSTOM_ITEM_DEFINITIONS_FILE = "./data/static_data/items/item_template_custom.xml";
	private static final Pattern ITEM_SHARD_PATTERN = Pattern.compile("item_template_\\d+_\\d+\\.xml");
	private static final Pattern NPC_SHARD_PATTERN = Pattern.compile("npc_template_\\d+_\\d+\\.xml");

	/**
	 * 获取 XmlDataLoader 单例（优先 Spring 提供的实例）。
	 * Returns the XmlDataLoader singleton (Spring-provided if available).
	 *
	 * XmlDataLoader instance
	 */
	public static final XmlDataLoader getInstance() {
		ObjectProvider<XmlDataLoader> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 侧实例提供者，供容器接管单例解析。
	 * Sets the Spring ObjectProvider used to resolve the singleton.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<XmlDataLoader> provider) {
		instanceProvider = provider;
	}

	/**
	 * 静态数据加载专用有界线程池。
	 * Dedicated bounded pool for static-data loading.
	 *
	 * <p>分片/紧凑定义加载存在"池任务内部再提交子任务并 join"的嵌套结构；放在 commonPool
	 * 上会触发 managedBlock 补偿线程的丢失唤醒竞态（实测 JDK 26 下主线程 join 永久挂起、
	 * 全部 worker 空闲），因此使用独立定长池。父任务最多 2 个（NPC/物品）且只等待自己的
	 * 子任务，池至少保留 4 个线程以避免父任务占满 worker。
	 * Shard/compact loads nest "submit children and join them" inside pool tasks; on the commonPool this
	 * exposed a managedBlock lost-wakeup race (observed on JDK 26: main joined forever while all workers
	 * idled), so a dedicated fixed pool is used instead. At most 2 parent tasks (NPC/items) wait only on
	 * their own children, so at least 4 workers leave capacity for their child tasks.
	 */
	private static final java.util.concurrent.ExecutorService STATIC_DATA_POOL =
		java.util.concurrent.Executors.newFixedThreadPool(
			Math.max(4, Runtime.getRuntime().availableProcessors()), runnable -> {
				Thread thread = new Thread(runnable, "static-data-loader");
				thread.setDaemon(true);
				return thread;
			});

	/**
	 * 返回静态数据加载专用线程池（DataManager 的物品/技能并行路径同样使用）。
	 * Returns the dedicated static-data pool (also used by DataManager's parallel item/skill paths).
	 *
	 * @return 专用线程池 / dedicated executor
	 */
	public static java.util.concurrent.Executor staticDataExecutor() {
		return STATIC_DATA_POOL;
	}

	/** 默认构造函数 / default constructor */
	public XmlDataLoader() {

	}

	/**
	 * 从 static_data.xml 读取入口并逐分区加载源文件，返回 {@link StaticData}。
	 * Reads the static_data.xml entry point and loads each section's source files into {@link StaticData}.
	 *
	 * @return 包含全部游戏静态数据的对象 / object containing all game static data from XML
	 */
	public StaticData loadStaticData() {
		return loadStaticData(ConsoleStaticDataProgressReporter.forCurrentConsole(), this::loadSkillData,
			new ConcurrentHashMap<>(), true);
	}

	/**
	 * 使用可预先启动的技能数据提供器加载静态数据。
	 * Loads static data with a skill-data supplier that callers may start in advance.
	 */
	public StaticData loadStaticData(Supplier<SkillData> skillDataSupplier) {
		return loadStaticData(ConsoleStaticDataProgressReporter.forCurrentConsole(), skillDataSupplier,
			new ConcurrentHashMap<>(), true);
	}

	/**
	 * 使用外部并行阶段计时加载静态数据；调用方负责在所有外部 Future 完成后输出最终
	 * 汇总。
	 * Loads static data with timings shared by externally parallel phases; the caller logs the final
	 * summary after all external futures have completed.
	 *
	 * @param skillDataSupplier 技能数据提供器 / skill-data supplier
	 * @param phaseTimings 线程安全的阶段计时表 / thread-safe phase timing map
	 * @return 静态数据 / static data
	 */
	public StaticData loadStaticData(Supplier<SkillData> skillDataSupplier, ConcurrentMap<String, Long> phaseTimings) {
		return loadStaticData(ConsoleStaticDataProgressReporter.forCurrentConsole(), skillDataSupplier,
			phaseTimings, false);
	}

	/**
	 * 带进度回调的静态数据加载：解析入口文件后逐分区、逐源文件 JAXB 反序列化。
	 * Loads static data with a progress reporter by parsing the entry point and unmarshalling each source file separately.
	 *
	 * @param progressReporter 进度报告器 / progress reporter
	 * @return 静态数据，失败则为 null / static data, or null on failure
	 */
	StaticData loadStaticData(StaticDataProgressReporter progressReporter) {
		return loadStaticData(progressReporter, this::loadSkillData, new ConcurrentHashMap<>(), true);
	}

	private StaticData loadStaticData(StaticDataProgressReporter progressReporter, Supplier<SkillData> skillDataSupplier,
			ConcurrentMap<String, Long> phaseTimings, boolean logPhaseTimings) {
		if (phaseTimings == null) {
			throw new IllegalArgumentException("phaseTimings must not be null");
		}
		File cleanMainXml = Config.dataFile(MAIN_XML_FILE);
		// 各紧凑定义文件与主文档互不依赖，提前并行加载，隐藏到主 XML 分区读取
		// 背后，与物品/技能并行路径同思路。
		// Compact definition files are independent of the main document; start them concurrently so they hide behind
		// section-by-section source loading (same idea as the parallel item/skill paths).
		CompletableFuture<PetDefinitionLoader.Result> petDefinitionsFuture = timedAsync("PetDefinitions",
			this::loadPetDefinitions, phaseTimings);
		CompletableFuture<NpcDropData> npcDropDataFuture = timedAsync("NpcDropData", this::loadNpcDropData, phaseTimings);
		CompletableFuture<HotspotLocationData> hotspotLocationDataFuture = timedAsync("HotspotLocationData",
			this::loadHotspotLocationData, phaseTimings);
		CompletableFuture<MotionData> motionDataFuture = timedAsync("MotionData", this::loadMotionData, phaseTimings);
		CompletableFuture<ChargeSkillData> chargeSkillDataFuture = timedAsync("ChargeSkillData",
			this::loadChargeSkillData, phaseTimings);
		CompletableFuture<NpcSkillData> npcSkillDataFuture = timedAsync("NpcSkillData", this::loadNpcSkillData,
			phaseTimings);
		CompletableFuture<AIData> aiDataFuture = timedAsync("AIData", this::loadAiData, phaseTimings);
		CompletableFuture<NpcPathBehaviorData> npcPathBehaviorDataFuture = timedAsync("NpcPathBehaviorData",
			this::loadNpcPathBehaviorData, phaseTimings);
		CompletableFuture<RetailAiData> retailAiDataFuture = timedAsync("RetailAiData", this::loadRetailAiData,
			phaseTimings);
		CompletableFuture<WalkerData> waypointDataFuture = timedAsync("RetailAiWaypoints", this::loadRetailAiWaypointData,
			phaseTimings);
		CompletableFuture<WindstreamData> windstreamDataFuture = timedAsync("WindstreamData", this::loadWindstreamData,
			phaseTimings);
		// NPC 模板直接从 data/static_data/npcs 下的源分片并行加载（见 loadNpcDataSharded）。
		// NPC templates load directly from source shards under data/static_data/npcs (see loadNpcDataSharded).
		CompletableFuture<NpcData> npcDataFuture = timedAsync("NpcData", () -> {
			NpcData data = loadNpcDataSharded();
			NpcCombatDefinitionLoader.apply(Config.definitionFile(NPC_COMBAT_DEFINITIONS_FILE), data);
			return data;
		}, phaseTimings);
		List<CompletableFuture<?>> definitionFutures = List.of(petDefinitionsFuture, npcDropDataFuture,
			hotspotLocationDataFuture,
			motionDataFuture, chargeSkillDataFuture, npcSkillDataFuture, aiDataFuture, npcPathBehaviorDataFuture,
			retailAiDataFuture, waypointDataFuture, windstreamDataFuture, npcDataFuture);
		List<StaticDataSection> sections;
		List<CompletableFuture<LoadedStaticDataSection>> sectionFutures = new ArrayList<>();
		try {
			sections = readStaticDataSections(cleanMainXml);
			int totalSections = sections.size();
			progressReporter.start(totalSections);
			log.info(I18n.get("log.113840e671fd", cleanMainXml.getPath()));
			long unmarshalStart = System.currentTimeMillis();
			AtomicInteger sectionIndex = new AtomicInteger();
			sectionFutures = new ArrayList<>(sections.size());
			for (StaticDataSection section : sections) {
				sectionFutures.add(CompletableFuture.supplyAsync(() -> loadStaticDataSection(section,
					progressReporter, sectionIndex, totalSections, phaseTimings), STATIC_DATA_POOL));
			}

			StaticData data = new StaticData();
			for (CompletableFuture<LoadedStaticDataSection> future : sectionFutures) {
				LoadedStaticDataSection loaded = joinDefinition(future);
				loaded.assignTo(data);
			}
			data.npcData = joinDefinition(npcDataFuture);
			PetDefinitionLoader.Result petDefinitions = joinDefinition(petDefinitionsFuture);
			data.npcDropData = joinDefinition(npcDropDataFuture);
			data.hotspotLocationData = joinDefinition(hotspotLocationDataFuture);
			long skillStart = System.nanoTime();
			try {
				data.skillData = skillDataSupplier.get();
			} finally {
				phaseTimings.putIfAbsent("SkillData", elapsedMillis(skillStart));
			}
			data.motionData = joinDefinition(motionDataFuture);
			data.chargeSkillData = joinDefinition(chargeSkillDataFuture);
			data.npcSkillData = joinDefinition(npcSkillDataFuture);
			data.aiData = joinDefinition(aiDataFuture);
			data.npcPathBehaviorData = joinDefinition(npcPathBehaviorDataFuture);
			data.retailAiData = joinDefinition(retailAiDataFuture);
			data.walkerData.merge(joinDefinition(waypointDataFuture));
			data.petDopingData = petDefinitions.doping();
			data.petMerchandData = petDefinitions.merchant();
			data.windstreamsData = joinDefinition(windstreamDataFuture);
			data.logSummary();
			long elapsed = System.currentTimeMillis() - unmarshalStart;
			progressReporter.finish(totalSections, elapsed);
			if (logPhaseTimings) {
				logStaticDataPhaseTimings(phaseTimings);
			}
			log.info(I18n.get("log.20818547282f", elapsed));
			return data;
		}
		/*
		 * catch (IllegalAnnotationsException e) {
		 * log.error(I18n.get("log.a30b9e9db6fa", e)); throw new
		 * Error("Error while loading static data", e); } catch (FileNotFoundException
		 * e) { log.error(I18n.get("log.a30b9e9db6fa", e)); throw new
		 * Error("Error while loading static data", e); } catch (JAXBException e) {
		 * log.error(I18n.get("log.a30b9e9db6fa", e)); throw new
		 * Error("Error while loading static data", e); }
		 */
		catch (Exception e) {
			definitionFutures.forEach(future -> future.cancel(true));
			sectionFutures.forEach(future -> future.cancel(true));
			progressReporter.failed();
			// 将异常同时作为 throwable 附上：I18n 占位符只含 toString，栈必须单独输出，
			// 否则吞掉根因。
			// Attach the throwable separately: the I18n placeholder only carries toString, the stack must be
			// logged too or the root cause is swallowed.
			log.error(I18n.get("log.a30b9e9db6fa", e.toString()), e);
		}
		return null;
	}

	/**
	 * 读取入口文件中的分区 import，并按 StaticData 字段归组源文件。
	 * Reads section imports from the entry file and groups source files by StaticData field.
	 */
	private static List<StaticDataSection> readStaticDataSections(File entryFile) throws Exception {
		Map<String, Field> fieldsByXmlName = staticDataFieldsByXmlName();
		Map<Field, List<File>> sourcesByField = new LinkedHashMap<>();
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		try (InputStream input = new BufferedInputStream(new FileInputStream(entryFile))) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(input);
			int depth = 0;
			String wrapperSection = null;
			try {
				while (reader.hasNext()) {
					int event = reader.next();
					if (event == XMLStreamConstants.START_ELEMENT) {
						depth++;
						if (depth == 2) {
							if ("import".equals(reader.getLocalName())) {
								addStaticDataImport(entryFile.getParentFile(), reader, null, fieldsByXmlName,
									sourcesByField);
							} else if (fieldsByXmlName.containsKey(reader.getLocalName())) {
								wrapperSection = reader.getLocalName();
							}
						} else if (depth == 3 && wrapperSection != null && "import".equals(reader.getLocalName())) {
							addStaticDataImport(entryFile.getParentFile(), reader, wrapperSection, fieldsByXmlName,
								sourcesByField);
						}
					} else if (event == XMLStreamConstants.END_ELEMENT) {
						if (depth == 2) {
							wrapperSection = null;
						}
						depth--;
					}
				}
			} finally {
				reader.close();
			}
		}
		List<StaticDataSection> sections = new ArrayList<>(sourcesByField.size());
		for (Map.Entry<Field, List<File>> entry : sourcesByField.entrySet()) {
			sections.add(new StaticDataSection(entry.getKey(), List.copyOf(entry.getValue())));
		}
		return sections;
	}

	private static void addStaticDataImport(File baseDirectory, XMLStreamReader reader, String expectedSection,
		Map<String, Field> fieldsByXmlName, Map<Field, List<File>> sourcesByField) throws Exception {
		String importPath = reader.getAttributeValue(null, "file");
		if (importPath == null || importPath.isBlank()) {
			throw new Error("Static-data import is missing its file attribute");
		}
		List<File> sourceFiles = listStaticDataSourceFiles(new File(baseDirectory, importPath));
		for (File sourceFile : sourceFiles) {
			String sectionName = expectedSection == null ? staticDataRootName(sourceFile) : expectedSection;
			Field field = fieldsByXmlName.get(sectionName);
			if (field == null) {
				throw new Error("No StaticData field is mapped to XML section " + sectionName + " from "
					+ sourceFile.getPath());
			}
			sourcesByField.computeIfAbsent(field, ignored -> new ArrayList<>()).add(sourceFile);
		}
	}

	private static List<File> listStaticDataSourceFiles(File source) throws IOException {
		if (source.isFile()) {
			return List.of(source);
		}
		if (!source.isDirectory()) {
			throw new FileNotFoundException("Static-data source not found: " + source.getPath());
		}
		try (var paths = Files.walk(source.toPath())) {
			return paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".xml"))
				.sorted()
				.map(Path::toFile)
				.toList();
		}
	}

	private static String staticDataRootName(File sourceFile) throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		try (InputStream input = new BufferedInputStream(new FileInputStream(sourceFile))) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(input);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT) {
						return reader.getLocalName();
					}
				}
			} finally {
				reader.close();
			}
		}
		throw new Error("Static-data source has no root element: " + sourceFile.getPath());
	}

	private static Map<String, Field> staticDataFieldsByXmlName() {
		Map<String, Field> fields = new LinkedHashMap<>();
		for (Field field : StaticData.class.getDeclaredFields()) {
			XmlElement element = field.getAnnotation(XmlElement.class);
			if (element == null) {
				continue;
			}
			String xmlName = "##default".equals(element.name()) ? field.getName() : element.name();
			field.setAccessible(true);
			fields.put(xmlName, field);
		}
		return fields;
	}

	private static LoadedStaticDataSection loadStaticDataSection(StaticDataSection section,
		StaticDataProgressReporter progressReporter, AtomicInteger sectionIndex, int totalSections,
		ConcurrentMap<String, Long> phaseTimings) {
		int currentSection = sectionIndex.incrementAndGet();
		String sectionName = section.field().getType().getSimpleName();
		long start = System.nanoTime();
		progressReporter.sectionStarted(currentSection, totalSections, sectionName, 1);
		try {
			Object combined = null;
			for (File source : section.sources()) {
				Object loaded = unmarshalSection(source, section.field().getType());
				if (combined == null) {
					combined = loaded;
				} else {
					mergeStaticDataSection(combined, loaded);
				}
			}
			if (combined == null) {
				throw new Error("Static-data section has no source files: " + sectionName);
			}
			progressReporter.sectionFinished(currentSection, totalSections, sectionName, 1);
			return new LoadedStaticDataSection(section.field(), combined);
		} finally {
			phaseTimings.put(sectionName, elapsedMillis(start));
		}
	}

	private static void mergeStaticDataSection(Object target, Object source) {
		try {
			Method merge = target.getClass().getMethod("merge", target.getClass());
			merge.invoke(target, source);
			return;
		} catch (NoSuchMethodException ignored) {
			// Most data holders expose only their JAXB-built collections and maps.
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to merge static-data section " + target.getClass().getName(), e);
		}
		for (Class<?> type = target.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
					continue;
				}
				try {
					field.setAccessible(true);
					Object targetValue = field.get(target);
					Object sourceValue = field.get(source);
					if (targetValue instanceof Map<?, ?> targetMap && sourceValue instanceof Map<?, ?> sourceMap) {
						mergeStaticDataMaps(targetMap, sourceMap);
					} else if (targetValue instanceof Collection<?> targetCollection
						&& sourceValue instanceof Collection<?> sourceCollection) {
						@SuppressWarnings("unchecked")
						Collection<Object> writableTarget = (Collection<Object>) targetCollection;
						writableTarget.addAll(sourceCollection);
					} else if (targetValue instanceof Number targetNumber && sourceValue instanceof Number sourceNumber
						&& field.getType().isPrimitive() && field.getName().endsWith("count")) {
						field.set(target, addNumbers(targetNumber, sourceNumber, field.getType()));
					} else if (targetValue == null && sourceValue != null) {
						field.set(target, sourceValue);
					}
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Failed to merge field " + field.getName() + " in "
						+ target.getClass().getName(), e);
				}
			}
		}
	}

	private static void mergeStaticDataMaps(Map<?, ?> target, Map<?, ?> source) {
		@SuppressWarnings("unchecked")
		Map<Object, Object> writableTarget = (Map<Object, Object>) target;
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			Object targetValue = writableTarget.get(entry.getKey());
			Object sourceValue = entry.getValue();
			if (targetValue instanceof Map<?, ?> targetMap && sourceValue instanceof Map<?, ?> sourceMap) {
				mergeStaticDataMaps(targetMap, sourceMap);
			} else if (targetValue instanceof Collection<?> targetCollection
				&& sourceValue instanceof Collection<?> sourceCollection) {
				@SuppressWarnings("unchecked")
				Collection<Object> writableCollection = (Collection<Object>) targetCollection;
				writableCollection.addAll(sourceCollection);
			} else {
				writableTarget.put(entry.getKey(), sourceValue);
			}
		}
	}

	private static Object addNumbers(Number left, Number right, Class<?> primitiveType) {
		if (primitiveType == int.class) {
			return left.intValue() + right.intValue();
		}
		if (primitiveType == long.class) {
			return left.longValue() + right.longValue();
		}
		if (primitiveType == short.class) {
			return (short) (left.shortValue() + right.shortValue());
		}
		if (primitiveType == byte.class) {
			return (byte) (left.byteValue() + right.byteValue());
		}
		if (primitiveType == float.class) {
			return left.floatValue() + right.floatValue();
		}
		if (primitiveType == double.class) {
			return left.doubleValue() + right.doubleValue();
		}
		return left;
	}

	private static <T> T unmarshalSection(File sourceFile, Class<T> type) {
		try (InputStream input = new BufferedInputStream(new FileInputStream(sourceFile))) {
			Unmarshaller unmarshaller = sharedJaxbContext(type).createUnmarshaller();
			unmarshaller.setEventHandler(new XmlValidationHandler());
			return type.cast(unmarshaller.unmarshal(input));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load static-data source: " + sourceFile.getPath(), e);
		}
	}

	private record StaticDataSection(Field field, List<File> sources) {
	}

	private record LoadedStaticDataSection(Field field, Object value) {
		private void assignTo(StaticData target) {
			try {
				field.set(target, value);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Failed to assign static-data field " + field.getName(), e);
			}
		}
	}

	/**
	 * 等待并行紧凑定义加载完成；失败时按原始异常类型重抛，保持与串行加载一致的失败
	 * 语义。
	 * Joins a parallel compact-definition load; rethrows the original cause so failures keep the same
	 * semantics as the former sequential loading.
	 *
	 * @param future 并行加载任务 / parallel load task
	 * @return 加载结果 / loaded result
	 */
	private static <T> T joinDefinition(CompletableFuture<T> future) {
		try {
			return future.join();
		} catch (CompletionException e) {
			Throwable cause = e.getCause() == null ? e : e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException(cause);
		}
	}

	/**
	 * 在线程池任务内部记录一个静态数据阶段的实际执行耗时。
	 * Records the actual execution time of one static-data phase inside its pool task.
	 *
	 * @param phaseName 阶段名称 / phase name
	 * @param supplier 阶段加载任务 / phase loader
	 * @param phaseTimings 线程安全的阶段计时表 / thread-safe phase timing map
	 * @return 带计时的 Future / timed future
	 */
	private static <T> CompletableFuture<T> timedAsync(String phaseName, Supplier<T> supplier,
			ConcurrentMap<String, Long> phaseTimings) {
		return CompletableFuture.supplyAsync(() -> {
			long start = System.nanoTime();
			try {
				return supplier.get();
			} finally {
				phaseTimings.put(phaseName, elapsedMillis(start));
			}
		}, STATIC_DATA_POOL);
	}

	private static long elapsedMillis(long startNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
	}

	/**
	 * 输出已收集的全部静态数据阶段耗时，按耗时从慢到快排序。
	 * Logs all collected static-data phase timings, sorted from slowest to fastest.
	 *
	 * @param phaseTimings 阶段计时表 / phase timing map
	 */
	public void logStaticDataPhaseTimings(Map<String, Long> phaseTimings) {
		if (phaseTimings == null || phaseTimings.isEmpty()) {
			return;
		}
		logSlowSectionTimings(phaseTimings);
	}

	public NpcDropData loadNpcDropData() {
		return NpcDropData.loadEager(Config.definitionFile(NPC_DROP_DEFINITIONS_DIR));
	}

	public HotspotLocationData loadHotspotLocationData() {
		return HotspotLocationData.load(Config.definitionFile(HOTSPOT_LOCATION_DEFINITIONS_FILE));
	}

	public NpcSkillData loadNpcSkillData() {
		NpcSkillData data = NpcSkillDefinitionLoader.load(Config.definitionFile(NPC_SKILL_DEFINITIONS_FILE));
		log.info(I18n.get("log.b3e7ebfb7d92", data.size()));
		return data;
	}

	public SkillData loadSkillData() {
		SkillData data = SkillDefinitionLoader.load(Config.definitionFile(SKILL_DEFINITIONS_DIR));
		log.info(I18n.get("log.static_data.compact_skills_loaded", data.size()));
		return data;
	}

	MotionData loadMotionData() {
		return loadDefinition(MotionData.class, MOTION_DEFINITIONS_FILE);
	}

	ChargeSkillData loadChargeSkillData() {
		return loadDefinition(ChargeSkillData.class, CHARGE_SKILL_DEFINITIONS_FILE);
	}

	private static <T> T loadDefinition(Class<T> type, String relativePath) {
		File file = Config.definitionFile(relativePath);
		try {
			return type.cast(createJaxbContext(type).createUnmarshaller().unmarshal(file));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load " + file.getPath(), e);
		}
	}

	public AIData loadAiData() {
		AIData data = loadDefinition(AIData.class, AI_BOMBS_FILE);
		data.merge(loadDefinition(AIData.class, AI_SPAWN_HELPERS_FILE));
		return data;
	}

	public NpcPathBehaviorData loadNpcPathBehaviorData() {
		NpcPathBehaviorData data = NpcPathBehaviorDefinitionLoader.load(Config.definitionFile(NPC_PATH_BEHAVIOR_FILE));
		log.info(I18n.get("log.static_data.npc_paths_loaded", data.size()));
		return data;
	}

	public RetailAiData loadRetailAiData() {
		RetailAiData data = RetailAiDefinitionLoader.load(Config.definitionFile(AI_DEFINITIONS_DIR),
			Config.definitionFile(RETAIL_NPC_AI_MAPPINGS_FILE), Config.definitionFile(RETAIL_AI_STRINGS_FILE),
			Config.definitionFile(RETAIL_AI_AREAS_FILE), Config.definitionFile(RETAIL_CONDITION_SPAWNS_FILE),
			Config.definitionFile(RETAIL_SKILL_CATEGORIES_FILE), Config.definitionFile(RETAIL_AI_LOCATION_ALIASES_FILE),
			Config.definitionFile(RETAIL_DIRECT_PORTALS_FILE), Config.definitionFile(RETAIL_NPC_SCORES_FILE),
			Config.definitionFile(RETAIL_GROUP_CONTROLLERS_FILE), Config.definitionFile(RETAIL_NPC_PARTIES_FILE),
			Config.definitionFile(RETAIL_DYNAMIC_AREAS_FILE));
		log.info(I18n.get("log.static_data.condition_spawns_loaded", data.conditionSpawnCount()));
		log.info(I18n.get("log.static_data.npc_parties_loaded", data.npcPartyCount(), data.npcPartyMemberCount()));
		log.info(I18n.get("log.static_data.dynamic_areas_loaded", data.dynamicAreaCount()));
		return data;
	}

	WalkerData loadRetailAiWaypointData() {
		return loadRetailAiWaypointData(Config.definitionFile(RETAIL_AI_WAYPOINTS_FILE),
			Config.definitionFile(RETAIL_AI_WAYPOINTS_SCHEMA));
	}

	static WalkerData loadRetailAiWaypointData(File file, File schemaFile) {
		try {
			Unmarshaller unmarshaller = createJaxbContext(WalkerData.class).createUnmarshaller();
			unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(schemaFile));
			WalkerData data = (WalkerData) unmarshaller.unmarshal(file);
			log.info(I18n.get("log.static_data.ai_waypoints_loaded", data.size()));
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail AI waypoints from " + file.getPath(), e);
		}
	}

	public PetDopingData loadPetDopingData() {
		return loadPetDefinitions().doping();
	}

	public PetMerchandData loadPetMerchandData() {
		return loadPetDefinitions().merchant();
	}

	private PetDefinitionLoader.Result loadPetDefinitions() {
		return PetDefinitionLoader.load(Config.definitionFile(PET_RIDES_DEFINITIONS_FILE));
	}

	public WindstreamData loadWindstreamData() {
		WindstreamData data = WindstreamDefinitionLoader.load(Config.definitionFile(FLY_PATH_DEFINITIONS_FILE),
			Config.definitionFile(WIND_DEFINITIONS_FILE), Config.definitionFile(ID_DEFINITIONS_FILE));
		log.info(I18n.get("log.e7553a368e56", data.size()));
		return data;
	}

	/**
	 * 直接从源分片并行加载物品模板数据（与主静态数据并行），并应用自定义覆盖文件。
	 * Loads item template data directly from source shards (parallel to main static data) and applies custom overrides.
	 *
	 * @return 物品数据 / item data
	 */
	public ItemData loadItemData() {
		return loadItemData(Config.dataFile(ITEM_SHARD_DIR), Config.dataFile(CUSTOM_ITEM_DEFINITIONS_FILE));
	}

	/**
	 * 直接从源分片目录并行加载物品数据，再按 ID 应用自定义覆盖或新增。
	 * Loads item data directly from source shards in parallel, then applies custom overrides or additions by id.
	 *
	 * @param itemShardDir 物品源分片目录 / item source-shard directory
	 * @param customOverrideFile 自定义覆盖文件（可不存在）/ custom override file (may be absent)
	 */
	ItemData loadItemData(File itemShardDir, File customOverrideFile) {
		long start = System.currentTimeMillis();
		List<File> shards = listTemplateShards(itemShardDir, ITEM_SHARD_PATTERN, "item");
		ItemData custom = customOverrideFile != null && customOverrideFile.isFile()
			? unmarshalShard(customOverrideFile, ItemData.class) : null;
		List<CompletableFuture<ItemData>> futures = new ArrayList<>(shards.size());
		for (File shard : shards) {
			futures.add(CompletableFuture.supplyAsync(() -> unmarshalShard(shard, ItemData.class), STATIC_DATA_POOL));
		}
		List<ItemData> shardResults = new ArrayList<>(futures.size());
		for (CompletableFuture<ItemData> future : futures) {
			shardResults.add(joinDefinition(future));
		}
		ItemData combined = new ItemData();
		combined.assembleFrom(shardResults, custom);
		log.info(I18n.get("log.static_data.item_shards_loaded", combined.size(), shards.size(),
			System.currentTimeMillis() - start));
		return combined;
	}

	/**
	 * 直接从源分片并行加载 NPC 模板（独立于静态数据入口分区）。
	 * Loads NPC templates directly from source shards (independent of entry-point sections).
	 *
	 * @return NPC 数据 / NPC data
	 */
	NpcData loadNpcDataSharded() {
		return loadNpcDataSharded(Config.dataFile(NPC_SHARD_DIR));
	}

	/**
	 * 直接从源分片目录并行加载 NPC 模板（测试用入口）。
	 * Loads NPC templates in parallel directly from the given source-shard directory (test entry point).
	 *
	 * @param shardDir NPC 模板源分片目录 / NPC template source-shard directory
	 * @return 合并后的 NPC 数据 / merged NPC data
	 */
	NpcData loadNpcDataSharded(File shardDir) {
		long start = System.currentTimeMillis();
		List<File> shards = listTemplateShards(shardDir, NPC_SHARD_PATTERN, "NPC");
		List<CompletableFuture<NpcData>> futures = new ArrayList<>(shards.size());
		for (File shard : shards) {
			futures.add(CompletableFuture.supplyAsync(() -> unmarshalShard(shard, NpcData.class), STATIC_DATA_POOL));
		}
		NpcData combined = new NpcData();
		for (CompletableFuture<NpcData> future : futures) {
			combined.getNpcData().putAll(joinDefinition(future).getNpcData());
		}
		log.info(I18n.get("log.static_data.npc_shards_loaded", combined.size(), shards.size(),
			System.currentTimeMillis() - start));
		return combined;
	}

	/**
	 * 枚举已提交的源分片；缺少目录或分片时直接失败，启动期间不会生成任何文件。
	 * Lists committed source shards; missing directories or shards fail fast and startup never writes files.
	 *
	 * @param shardDir 源分片目录 / source-shard directory
	 * @param shardPattern 分片文件名模式 / shard filename pattern
	 * @param type 数据类型 / data type
	 * @return 按源文件首个模板 ID 排序的分片 / shards sorted by the first template id in each source file
	 */
	private static List<File> listTemplateShards(File shardDir, Pattern shardPattern, String type) {
		if (shardDir == null || !shardDir.isDirectory()) {
			throw new Error(type + " template shard directory not found: " + shardDir);
		}
		File[] files = shardDir.listFiles((dir, name) -> shardPattern.matcher(name).matches());
		if (files == null || files.length == 0) {
			throw new Error("No " + type + " template shards found under " + shardDir.getPath()
				+ "; expected files matching " + shardPattern.pattern());
		}
		List<File> shards = new ArrayList<>(List.of(files));
		String elementName = "item".equals(type) ? "item_template" : "npc_template";
		shards.sort(Comparator.comparingLong(file -> firstTemplateId(file, elementName)));
		return shards;
	}

	/**
	 * 读取分片内首个模板 ID，恢复源文件顺序而不依赖范围文件名的字典序。
	 * Reads the first template id in a shard so source order does not depend on filename lexicographic order.
	 */
	private static long firstTemplateId(File shard, String elementName) {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		try (InputStream input = new BufferedInputStream(new FileInputStream(shard))) {
			XMLStreamReader reader = factory.createXMLStreamReader(input);
			try {
				while (reader.hasNext()) {
					if (reader.next() != XMLStreamConstants.START_ELEMENT || !elementName.equals(reader.getLocalName())) {
						continue;
					}
					String attribute = "item_template".equals(elementName) ? "id" : "npc_id";
					return Long.parseLong(reader.getAttributeValue(null, attribute));
				}
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			throw new Error("Failed to read first template ID from " + shard.getPath(), e);
		}
		throw new Error("Template shard is empty: " + shard.getPath());
	}

	/**
	 * 分片 JAXBContext 缓存，避免每个分片重复构建上下文 /
	 * Caches shard JAXB contexts so shards reuse one context per type.
	 */
	private static final Map<Class<?>, JAXBContext> SHARD_CONTEXTS = new ConcurrentHashMap<>();

	/**
	 * 获取（或创建）指定类型的共享 JAXBContext。
	 * Returns (or creates) the shared JAXBContext for the given type.
	 *
	 * @param type 绑定类型 / bound type
	 * @return 共享上下文 / shared context
	 */
	private static JAXBContext sharedJaxbContext(Class<?> type) {
		return SHARD_CONTEXTS.computeIfAbsent(type, t -> {
			try {
				return createJaxbContext(t);
			} catch (jakarta.xml.bind.JAXBException e) {
				throw new IllegalStateException("Failed to create JAXB context for " + t.getName(), e);
			}
		});
	}

	/**
	 * 反序列化单个分片文件（字节流直读）。
	 * Unmarshals one shard file (raw byte stream).
	 *
	 * @param shardFile 分片文件 / shard file
	 * @param type 目标类型 / target type
	 * @return 分片数据 / shard data
	 */
	private static <T> T unmarshalShard(File shardFile, Class<T> type) {
		try (InputStream input = new BufferedInputStream(new FileInputStream(shardFile))) {
			Unmarshaller un = sharedJaxbContext(type).createUnmarshaller();
			un.setEventHandler(new XmlValidationHandler());
			return type.cast(un.unmarshal(input));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load template shard: " + shardFile.getPath(), e);
		}
	}

	/**
	 * 创建绑定进度监听的 StaticData Unmarshaller。
	 * Creates a StaticData Unmarshaller wired with progress reporting.
	 *
	 * @param progressReporter 进度报告器 / progress reporter
	 * @param totalSections 分段总数 / total section count
	 * @param sectionEntryCounts 各分区条目数 / per-section entry counts
	 * @return 配置的反序列化器 / configured unmarshaller
	 */
	Unmarshaller createStaticDataUnmarshaller(StaticDataProgressReporter progressReporter, int totalSections, Map<String, Integer> sectionEntryCounts)
			throws Exception {
		return createStaticDataUnmarshaller(new StaticDataProgressListener(progressReporter, totalSections, sectionEntryCounts));
	}

	private Unmarshaller createStaticDataUnmarshaller(StaticDataProgressListener progressListener) throws Exception {
		JAXBContext jc = createJaxbContext(StaticData.class);
		Unmarshaller un = jc.createUnmarshaller();
		un.setEventHandler(new XmlValidationHandler());
		// JAXB 源文件读取使用事件处理器报告问题；入口解析不再依赖合并文档校验。
		// Source-file unmarshalling reports problems through the event handler and does not depend on a merged document.
		un.setListener(progressListener);
		return un;
	}

	/**
	 * 创建指定绑定类型的 JAXBContext；线程上下文类加载器临时切换为绑定类型的加载器，
	 * 保证在任意线程池线程（commonPool 等）上都能找到 JAXB runtime 实现。
	 * Creates a JAXBContext for the bound type, temporarily switching the thread context classloader
	 * to the bound type's loader so the JAXB runtime is found even on pool threads (e.g. commonPool).
	 */
	public static JAXBContext createJaxbContext(Class<?> boundType) throws jakarta.xml.bind.JAXBException {
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		ClassLoader boundTypeClassLoader = boundType.getClassLoader();
		try {
			if (boundTypeClassLoader != null) {
				thread.setContextClassLoader(boundTypeClassLoader);
			}
			return JAXBContext.newInstance(boundType);
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}




	static String staticDataSectionName(Object target, Object parent) {
		if (target == null || !(parent instanceof StaticData)) {
			return null;
		}
		return target.getClass().getSimpleName();
	}

	static int staticDataSectionCount() {
		int count = 0;
		for (Field field : StaticData.class.getFields()) {
			if (field.getAnnotation(XmlElement.class) != null) {
				count++;
			}
		}
		return count;
	}

	static Map<String, Integer> staticDataSectionEntryCounts(File staticDataXml) throws Exception {
		try (InputStream input = new BufferedInputStream(new FileInputStream(staticDataXml))) {
			return staticDataSectionEntryCounts(input);
		}
	}

	static Map<String, Integer> staticDataSectionEntryCounts(byte[] xml) throws Exception {
		return staticDataSectionEntryCounts(new ByteArrayInputStream(xml));
	}

	private static Map<String, Integer> staticDataSectionEntryCounts(InputStream input) throws Exception {
		Map<String, String> sectionNamesByXmlElement = staticDataSectionNamesByXmlElement();
		Map<String, Integer> counts = new HashMap<>();
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		XMLStreamReader reader = inputFactory.createXMLStreamReader(input);
		int depth = 0;
		String currentSectionName = null;
		try {
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					depth++;
					if (depth == 2) {
						currentSectionName = sectionNamesByXmlElement.get(reader.getLocalName());
						if (currentSectionName != null) {
							counts.putIfAbsent(currentSectionName, 0);
						}
					} else if (depth == 3 && currentSectionName != null) {
						counts.merge(currentSectionName, 1, Integer::sum);
					}
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					if (depth == 2) {
						currentSectionName = null;
					}
					depth--;
				}
			}
		} finally {
			reader.close();
		}
		counts.replaceAll((sectionName, count) -> Math.max(1, count));
		return counts;
	}

	/**
	 * 返回各分区条目数；按配置选择扫描传入 XML 或使用默认值。
	 * Returns section entry counts by scanning the supplied XML when enabled, otherwise using defaults.
	 *
	 * @param xml 待扫描的 XML / XML to scan
	 * @return 分区名到条目数映射 / map of section name to entry count
	 */
	Map<String, Integer> loadSectionEntryCounts(byte[] xml) throws Exception {
		if (!GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE) {
			return defaultSectionEntryCounts();
		}
		return staticDataSectionEntryCounts(xml);
	}

	private static Map<String, Integer> defaultSectionEntryCounts() {
		Map<String, Integer> counts = new HashMap<>();
		for (String sectionName : staticDataSectionNamesByXmlElement().values()) {
			counts.put(sectionName, 1);
		}
		return counts;
	}

	private static Map<String, String> staticDataSectionNamesByXmlElement() {
		Map<String, String> sectionNamesByXmlElement = new HashMap<>();
		for (Field field : StaticData.class.getFields()) {
			XmlElement element = field.getAnnotation(XmlElement.class);
			if (element != null) {
				sectionNamesByXmlElement.put(element.name(), field.getType().getSimpleName());
			}
		}
		return sectionNamesByXmlElement;
	}

	static List<Map.Entry<String, Long>> slowestSectionTimings(Map<String, Long> timings, int limit) {
		return timings.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.limit(limit)
			.toList();
	}

	private static void logSlowSectionTimings(Map<String, Long> timings) {
		List<Map.Entry<String, Long>> slowest = slowestSectionTimings(timings, timings.size());
		if (slowest.isEmpty()) {
			return;
		}
		log.info(I18n.get("log.6a77df723c2e"));
		for (Map.Entry<String, Long> timing : slowest) {
			log.info(I18n.get("log.b6609c40aca7", String.format("%-32s", timing.getKey()), timing.getValue()));
		}
	}

	private static final class StaticDataProgressListener extends Unmarshaller.Listener {

		private final StaticDataProgressReporter progressReporter;
		private final int totalSections;
		private final Map<String, Integer> sectionEntryCounts;
		private final Set<String> sectionNames;
		private final Map<String, Integer> sectionEntriesLoaded = new HashMap<>();
		private final Map<String, Long> sectionStartTimes = new HashMap<>();
		private final Map<String, Long> sectionElapsedTimes = new HashMap<>();
		private int sectionIndex;

		private StaticDataProgressListener(StaticDataProgressReporter progressReporter, int totalSections, Map<String, Integer> sectionEntryCounts) {
			this.progressReporter = progressReporter;
			this.totalSections = totalSections;
			this.sectionEntryCounts = sectionEntryCounts;
			this.sectionNames = sectionEntryCounts.keySet();
		}

		@Override
		public void beforeUnmarshal(Object target, Object parent) {
			String sectionName = staticDataSectionName(target, parent);
			if (sectionName == null) {
				return;
			}
			sectionEntriesLoaded.put(sectionName, 0);
			sectionStartTimes.put(sectionName, System.currentTimeMillis());
			progressReporter.sectionStarted(++sectionIndex, totalSections, sectionName, sectionEntryCounts.getOrDefault(sectionName, 1));
		}

		@Override
		public void afterUnmarshal(Object target, Object parent) {
			String sectionName = staticDataSectionName(target, parent);
			if (sectionName != null) {
				Long startTime = sectionStartTimes.remove(sectionName);
				if (startTime != null) {
					sectionElapsedTimes.put(sectionName, System.currentTimeMillis() - startTime);
				}
				progressReporter.sectionFinished(sectionIndex, totalSections, sectionName, sectionEntryCounts.getOrDefault(sectionName, 1));
				return;
			}
			String parentSectionName = staticDataChildSectionName(parent, sectionNames);
			if (parentSectionName == null) {
				return;
			}
			int totalEntries = sectionEntryCounts.getOrDefault(parentSectionName, 1);
			int currentEntries = Math.min(totalEntries, sectionEntriesLoaded.merge(parentSectionName, 1, Integer::sum));
			progressReporter.sectionProgress(sectionIndex, totalSections, parentSectionName, currentEntries, totalEntries);
		}

		private Map<String, Long> sectionElapsedTimes() {
			return sectionElapsedTimes;
		}
	}

	private static String staticDataChildSectionName(Object parent, Set<String> sectionNames) {
		if (parent == null) {
			return null;
		}
		String sectionName = parent.getClass().getSimpleName();
		return sectionNames.contains(sectionName) ? sectionName : null;
	}

	/** 内部懒加载单例持有者 / lazy-init holder for the internal singleton */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final XmlDataLoader instance = new XmlDataLoader();
	}
}
