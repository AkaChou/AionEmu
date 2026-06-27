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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.xml.sax.SAXException;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dataholders.StaticData;

/**
 * This class is responsible for loading xml files. It uses JAXB to do the
 * job.<br>
 * In addition, it uses @{link {@link XmlMerger} to create input file from all
 * xml files.
 * 
 * @author Luno
 */
public class XmlDataLoader {

	private static final Logger log = LoggerFactory.getLogger(XmlDataLoader.class);
	private static volatile ObjectProvider<XmlDataLoader> instanceProvider;
	/** File containing xml schema declaration */
	private final static String XML_SCHEMA_FILE = "./data/static_data/static_data.xsd";
	private static final String CACHE_XML_FILE = "./cache/static_data.xml";
	private static final String MAIN_XML_FILE = "./data/static_data/static_data.xml";

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
		mergeXmlFiles(cachedXml, cleanMainXml);
		log.info("Prepared static data cache in {} ms", System.currentTimeMillis() - cacheStart);

		try {
			long unmarshalStart = System.currentTimeMillis();
			Map<String, Integer> sectionEntryCounts = staticDataSectionEntryCounts(cachedXml);
			int totalSections = sectionEntryCounts.size();
			progressReporter.start(totalSections);
			log.info("Unmarshalling static data from {}", cachedXml.getPath());
			JAXBContext jc = JAXBContext.newInstance(StaticData.class);
			Unmarshaller un = jc.createUnmarshaller();
			un.setEventHandler(new XmlValidationHandler());
			un.setSchema(getSchema());
			un.setListener(new StaticDataProgressListener(progressReporter, totalSections, sectionEntryCounts));
			try (FileReader reader = new FileReader(cachedXml)) {
				StaticData data = (StaticData) un.unmarshal(reader);
				long elapsed = System.currentTimeMillis() - unmarshalStart;
				progressReporter.finish(totalSections, elapsed);
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

	private static final class StaticDataProgressListener extends Unmarshaller.Listener {

		private final StaticDataProgressReporter progressReporter;
		private final int totalSections;
		private final Map<String, Integer> sectionEntryCounts;
		private final Set<String> sectionNames;
		private final Map<String, Integer> sectionEntriesLoaded = new HashMap<>();
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
			progressReporter.sectionStarted(++sectionIndex, totalSections, sectionName, sectionEntryCounts.getOrDefault(sectionName, 1));
		}

		@Override
		public void afterUnmarshal(Object target, Object parent) {
			String sectionName = staticDataSectionName(target, parent);
			if (sectionName != null) {
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
	private void mergeXmlFiles(File cachedXml, File cleanMainXml) throws Error {
		XmlMerger merger = new XmlMerger(cleanMainXml, cachedXml);
		try {
			merger.process();
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
