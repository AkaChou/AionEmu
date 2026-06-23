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
import java.io.FileReader;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	/** File containing xml schema declaration */
	private final static String XML_SCHEMA_FILE = "./data/static_data/static_data.xsd";
	private static final String CACHE_XML_FILE = "./cache/static_data.xml";
	private static final String MAIN_XML_FILE = "./data/static_data/static_data.xml";

	public static final XmlDataLoader getInstance() {
		return SingletonHolder.instance;
	}

	private XmlDataLoader() {

	}

	/**
	 * Creates {@link StaticData} object based on xml files, starting from
	 * static_data.xml
	 * 
	 * @return StaticData object, containing all game data defined in xml files
	 */
	public StaticData loadStaticData() {
		File cachedXml = Config.cacheFile(CACHE_XML_FILE);
		makeCacheDirectory(cachedXml.getParentFile());
		File cleanMainXml = Config.dataFile(MAIN_XML_FILE);
		long cacheStart = System.currentTimeMillis();
		log.info("Preparing static data cache: {}", cachedXml.getPath());
		mergeXmlFiles(cachedXml, cleanMainXml);
		log.info("Prepared static data cache in {} ms", System.currentTimeMillis() - cacheStart);

		try {
			long unmarshalStart = System.currentTimeMillis();
			log.info("Unmarshalling static data from {}", cachedXml.getPath());
			JAXBContext jc = JAXBContext.newInstance(StaticData.class);
			Unmarshaller un = jc.createUnmarshaller();
			un.setEventHandler(new XmlValidationHandler());
			un.setSchema(getSchema());
			un.setListener(new StaticDataProgressListener());
			try (FileReader reader = new FileReader(cachedXml)) {
				StaticData data = (StaticData) un.unmarshal(reader);
				log.info("Unmarshalled static data in {} ms", System.currentTimeMillis() - unmarshalStart);
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

	private static final class StaticDataProgressListener extends Unmarshaller.Listener {

		private final Map<Object, Long> sectionStartTimes = new IdentityHashMap<>();

		@Override
		public void beforeUnmarshal(Object target, Object parent) {
			String sectionName = staticDataSectionName(target, parent);
			if (sectionName == null) {
				return;
			}
			sectionStartTimes.put(target, System.currentTimeMillis());
			log.info("Loading static data section: {}", sectionName);
		}

		@Override
		public void afterUnmarshal(Object target, Object parent) {
			String sectionName = staticDataSectionName(target, parent);
			if (sectionName == null) {
				return;
			}
			Long startedAt = sectionStartTimes.remove(target);
			if (startedAt == null) {
				log.info("Loaded static data section: {}", sectionName);
				return;
			}
			log.info("Loaded static data section: {} in {} ms", sectionName, System.currentTimeMillis() - startedAt);
		}
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
