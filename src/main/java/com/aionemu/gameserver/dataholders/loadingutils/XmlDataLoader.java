/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.StaticData;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * This class is responsible for loading xml files. It uses JAXB to do the
 * job.<br>
 * In addition, it uses @{link {@link XmlMerger} to create input file from all
 * xml files.
 * 
 * @author Luno
 */
@Slf4j
public class XmlDataLoader {

	private static volatile ObjectProvider<XmlDataLoader> instanceProvider;
	/** File containing xml schema declaration */
	private final static String XML_SCHEMA_FILE = "./data/static_data/static_data.xsd";
	private static final String CACHE_XML_FILE = "./cache/static_data.xml";
	private static final String MAIN_XML_FILE = "./data/static_data/static_data.xml";
	private static final String ITEM_CACHE_XML_FILE = "./cache/item_templates.xml";
	private static final String ITEM_DATA_DIR = "./data/static_data/items";
	private static final String ITEM_SOURCE_XML = "<item_templates><import file=\"item\" skipRoot=\"true\"/></item_templates>";

	public static final XmlDataLoader getInstance() {
		ObjectProvider<XmlDataLoader> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<XmlDataLoader> provider) {
		instanceProvider = provider;
	}

	private static volatile Future<JAXBContext> preloadedContext;

	/**
	 * Starts asynchronous preloading of the StaticData JAXBContext so it is ready when unmarshalling begins.
	 * Modeled after aion-server JAXBUtil.preLoadContextAsync. Call as early as possible during startup.
	 */
	public static void preloadContextAsync() {
		if (preloadedContext == null) {
			preloadedContext = ForkJoinPool.commonPool().submit(() -> JAXBContext.newInstance(StaticData.class));
		}
	}

	public XmlDataLoader() {

	}

	/**
	 * Creates {@link StaticData} object based on xml files, starting from
	 * static_data.xml
	 * 
	 * @return StaticData object, containing all game data defined in xml files
	 */
	public StaticData loadStaticData() {
		return loadStaticData(ConsoleStaticDataProgressReporter.forCurrentConsole());
	}

	StaticData loadStaticData(StaticDataProgressReporter progressReporter) {
		File cachedXml = Config.cacheFile(CACHE_XML_FILE);
		makeCacheDirectory(cachedXml.getParentFile());
		File cleanMainXml = Config.dataFile(MAIN_XML_FILE);
		long cacheStart = System.currentTimeMillis();
		log.info("Preparing static data cache: {}", cachedXml.getPath());
		boolean cacheRebuilt = mergeXmlFiles(cachedXml, cleanMainXml);
		log.info("Prepared static data cache in {} ms", System.currentTimeMillis() - cacheStart);
		if (cacheRebuilt) {
			validateCacheAsync(cachedXml);
		}

		try {
			long unmarshalStart = System.currentTimeMillis();
			Map<String, Integer> sectionEntryCounts = loadSectionEntryCounts(cachedXml);
			int totalSections = sectionEntryCounts.size();
			progressReporter.start(totalSections);
			log.info("Unmarshalling static data from {}", cachedXml.getPath());
			StaticDataProgressListener progressListener = new StaticDataProgressListener(progressReporter, totalSections, sectionEntryCounts);
			Unmarshaller un = createStaticDataUnmarshaller(progressListener);
			try (FileReader reader = new FileReader(cachedXml)) {
				StaticData data = (StaticData) un.unmarshal(reader);
				long elapsed = System.currentTimeMillis() - unmarshalStart;
				progressReporter.finish(totalSections, elapsed);
				logSlowSectionTimings(progressListener.sectionElapsedTimes());
				log.info("Unmarshalled static data in {} ms", elapsed);
				return data;
			}
		}
		/*
		 * catch (IllegalAnnotationsException e) {
		 * log.error("Error while loading static data", e); throw new
		 * Error("Error while loading static data", e); } catch (FileNotFoundException
		 * e) { log.error("Error while loading static data", e); throw new
		 * Error("Error while loading static data", e); } catch (JAXBException e) {
		 * log.error("Error while loading static data", e); throw new
		 * Error("Error while loading static data", e); }
		 */
		catch (Exception e) {
			progressReporter.failed();
			log.error("Error while loading static data", e);
		}
		return null;
	}

	public ItemData loadItemData() {
		return loadItemData(Config.cacheFile(ITEM_CACHE_XML_FILE), Config.dataFile(ITEM_DATA_DIR));
	}

	ItemData loadItemData(File cachedXml, File itemDataDir) {
		makeCacheDirectory(cachedXml.getParentFile());
		File sourceXml = itemDataSourceXml(cachedXml.getParentFile());
		prepareItemDataSource(sourceXml);
		long cacheStart = System.currentTimeMillis();
		log.info("Preparing item data cache: {}", cachedXml.getPath());
		mergeXmlFiles(cachedXml, sourceXml, itemDataDir);
		log.info("Prepared item data cache in {} ms", System.currentTimeMillis() - cacheStart);

		long unmarshalStart = System.currentTimeMillis();
		log.info("Unmarshalling item data from {}", cachedXml.getPath());
		try (FileReader reader = new FileReader(cachedXml)) {
			JAXBContext jc = JAXBContext.newInstance(ItemData.class);
			Unmarshaller un = jc.createUnmarshaller();
			un.setEventHandler(new XmlValidationHandler());
			ItemData data = (ItemData) un.unmarshal(reader);
			log.info("Unmarshalled item data in {} ms", System.currentTimeMillis() - unmarshalStart);
			return data;
		} catch (Exception e) {
			log.error("Error while loading item data", e);
			throw new Error("Error while loading item data", e);
		}
	}

	private File itemDataSourceXml(File cacheDir) {
		return new File(cacheDir, "item_templates.source.xml");
	}

	private void prepareItemDataSource(File sourceXml) {
		if (sourceXml.exists()) {
			return;
		}
		try (FileWriter writer = new FileWriter(sourceXml)) {
			writer.write(ITEM_SOURCE_XML);
		} catch (IOException e) {
			throw new Error("Error while preparing item data source", e);
		}
		sourceXml.setLastModified(0L);
	}

	Unmarshaller createStaticDataUnmarshaller(StaticDataProgressReporter progressReporter, int totalSections, Map<String, Integer> sectionEntryCounts)
			throws Exception {
		return createStaticDataUnmarshaller(new StaticDataProgressListener(progressReporter, totalSections, sectionEntryCounts));
	}

	private Unmarshaller createStaticDataUnmarshaller(StaticDataProgressListener progressListener) throws Exception {
		Future<JAXBContext> task = preloadedContext;
		JAXBContext jc = task != null ? task.get() : JAXBContext.newInstance(StaticData.class);
		Unmarshaller un = jc.createUnmarshaller();
		un.setEventHandler(new XmlValidationHandler());
		// Schema validation is intentionally not wired into JAXB unmarshalling; it is slow and is run only for rebuilt caches.
		un.setListener(progressListener);
		return un;
	}

	Future<?> validateCacheAsync(File cachedXml) {
		return submitValidationTask(() -> validateCache(cachedXml));
	}

	Future<?> submitValidationTask(Runnable task) {
		return ThreadPoolManager.getInstance().submitLongRunning(task);
	}

	private void validateCache(File cachedXml) {
		long validationStart = System.currentTimeMillis();
		log.info("Validating static data cache in background: {}", cachedXml.getPath());
		try (Reader reader = new FileReader(cachedXml)) {
			getSchema().newValidator().validate(new SAXSource(new InputSource(reader)));
			log.info("Validated static data cache in {} ms", System.currentTimeMillis() - validationStart);
		} catch (Throwable t) {
			cachedXml.setLastModified(0);
			log.error("Error validating static data cache: {}", cachedXml.getPath(), t);
			throw new Error("Error validating static data cache", t);
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
		Map<String, String> sectionNamesByXmlElement = staticDataSectionNamesByXmlElement();
		Map<String, Integer> counts = new HashMap<>();
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		try (FileInputStream stream = new FileInputStream(staticDataXml)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
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
		}
		counts.replaceAll((sectionName, count) -> Math.max(1, count));
		return counts;
	}

	/**
	 * Returns section entry counts, cached in a sidecar file to avoid re-scanning the 239MB XML on every warm start.
	 * Cache is rebuilt only when the XML cache file is newer than the counts file.
	 */
	Map<String, Integer> loadSectionEntryCounts(File cachedXml) throws Exception {
		if (!GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE) {
			return defaultSectionEntryCounts();
		}
		File countsFile = new File(cachedXml.getParentFile(), "static_data.counts");
		if (countsFile.exists() && countsFile.lastModified() >= cachedXml.lastModified()) {
			return loadCountsFile(countsFile);
		}
		Map<String, Integer> counts = staticDataSectionEntryCounts(cachedXml);
		saveCountsFile(countsFile, counts);
		return counts;
	}

	private static Map<String, Integer> loadCountsFile(File countsFile) throws IOException {
		Properties props = new Properties();
		try (FileReader reader = new FileReader(countsFile)) {
			props.load(reader);
		}
		Map<String, Integer> counts = new HashMap<>();
		for (String name : props.stringPropertyNames()) {
			counts.put(name, Integer.valueOf(props.getProperty(name)));
		}
		return counts;
	}

	private static void saveCountsFile(File countsFile, Map<String, Integer> counts) {
		Properties props = new Properties();
		counts.forEach((name, count) -> props.setProperty(name, count.toString()));
		try (FileWriter writer = new FileWriter(countsFile)) {
			props.store(writer, "static_data section entry counts (avoids re-scanning XML on warm start)");
		} catch (IOException e) {
			log.warn("Could not save section counts cache: {}", countsFile.getPath(), e);
		}
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
		List<Map.Entry<String, Long>> slowest = slowestSectionTimings(timings, 5);
		if (slowest.isEmpty()) {
			return;
		}
		log.info("Static data section timings (ms, slowest first):");
		for (Map.Entry<String, Long> timing : slowest) {
			log.info("  {} {}", String.format("%-32s", timing.getKey()), timing.getValue());
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

	/**
	 * Creates and returns {@link Schema} object representing xml schema of xml
	 * files
	 * 
	 * @return a Schema object.
	 */
	private Schema getSchema() {
		Schema schema = null;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try {
			schema = sf.newSchema(Config.dataFile(XML_SCHEMA_FILE));
		} catch (SAXException saxe) {
			log.error("Error while getting schema", saxe);
			throw new Error("Error while getting schema", saxe);
		}
		return schema;
	}

	/** Creates directory for cache files if it doesn't already exist */
	private void makeCacheDirectory(File cacheDir) {
		if (cacheDir != null && !cacheDir.exists()) {
			cacheDir.mkdirs();
		}
	}

	/**
	 * Merges xml files(if are newer than cache file) and puts output to cache file.
	 * 
	 * @see XmlMerger
	 * @param cachedXml
	 * @param cleanMainXml
	 * @throws Error is thrown if some problem occured.
	 */
	private boolean mergeXmlFiles(File cachedXml, File cleanMainXml) throws Error {
		return mergeXmlFiles(cachedXml, cleanMainXml, cleanMainXml.getParentFile());
	}

	private boolean mergeXmlFiles(File cachedXml, File cleanMainXml, File baseDir) throws Error {
		XmlMerger merger = new XmlMerger(cleanMainXml, cachedXml, baseDir);
		try {
			return merger.process();
		} catch (Exception e) {
			log.error("Error while merging xml files", e);
			throw new Error("Error while merging xml files", e);
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final XmlDataLoader instance = new XmlDataLoader();
	}
}
