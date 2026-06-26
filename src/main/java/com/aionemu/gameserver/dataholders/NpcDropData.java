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
package com.aionemu.gameserver.dataholders;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;

/**
 * @author MrPoke
 *
 */
@XmlRootElement(name = "npc_drops")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "npcDropData", propOrder = { "npcDrop" })
public class NpcDropData {

	private static final Logger log = LoggerFactory.getLogger(NpcDropData.class);
	private static final int DEFAULT_CACHE_MAX_ENTRIES = 2000;
	private static final long DEFAULT_CACHE_EXPIRE_AFTER_ACCESS_MILLIS = TimeUnit.MINUTES.toMillis(60);

	@XmlElement(name = "npc_drop")
	protected List<NpcDrop> npcDrop;
	@XmlTransient
	private File npcDropsDirectory;
	@XmlTransient
	private int maxCachedDrops;
	@XmlTransient
	private long expireAfterAccessMillis;
	@XmlTransient
	private LongSupplier currentTimeMillis;
	@XmlTransient
	private JAXBContext npcDropContext;
	@XmlTransient
	private Map<Integer, List<File>> indexedDropFiles;
	@XmlTransient
	private LinkedHashMap<Integer, CacheEntry> cachedDrops;

	public NpcDropData() {
	}

	public static NpcDropData loadLazy(File npcDropsDirectory) {
		return loadLazy(npcDropsDirectory, DEFAULT_CACHE_MAX_ENTRIES, DEFAULT_CACHE_EXPIRE_AFTER_ACCESS_MILLIS, System::currentTimeMillis);
	}

	public static NpcDropData loadLazy(File npcDropsDirectory, int maxCachedDrops, long expireAfterAccessMillis) {
		return loadLazy(npcDropsDirectory, maxCachedDrops, expireAfterAccessMillis, System::currentTimeMillis);
	}

	public static NpcDropData loadLazy(File npcDropsDirectory, int maxCachedDrops, long expireAfterAccessMillis, LongSupplier currentTimeMillis) {
		return new NpcDropData(npcDropsDirectory, maxCachedDrops, expireAfterAccessMillis, currentTimeMillis);
	}

	private NpcDropData(File npcDropsDirectory, int maxCachedDrops, long expireAfterAccessMillis, LongSupplier currentTimeMillis) {
		this.npcDropsDirectory = npcDropsDirectory;
		this.maxCachedDrops = Math.max(0, maxCachedDrops);
		this.expireAfterAccessMillis = expireAfterAccessMillis;
		this.currentTimeMillis = currentTimeMillis;
		this.npcDropContext = createNpcDropContext();
		this.cachedDrops = new LinkedHashMap<>(64, 0.75f, true);
		this.indexedDropFiles = indexDropFiles(npcDropsDirectory);
		log.info("Indexed {} lazy NPC drop templates from {}", indexedDropFiles.size(), npcDropsDirectory.getPath());
	}

	/**
	 * @return the npcDrop
	 */
	public List<NpcDrop> getNpcDrop() {
		if (isLazy()) {
			return Collections.emptyList();
		}
		return npcDrop == null ? Collections.emptyList() : npcDrop;
	}

	/**
	 * @param npcDrop the npcDrop to set
	 */
	public void setNpcDrop(List<NpcDrop> npcDrop) {
		this.npcDrop = npcDrop;
	}

	public int size() {
		return isLazy() ? indexedDropFiles.size() : getNpcDrop().size();
	}

	public synchronized NpcDrop getDrop(int npcId) {
		if (!isLazy()) {
			return getNpcDrop().stream()
				.filter(drop -> drop.getNpcId() == npcId)
				.findFirst()
				.orElse(null);
		}
		long now = currentTimeMillis.getAsLong();
		cleanupExpiredDrops(now);
		CacheEntry cached = cachedDrops.get(npcId);
		if (cached != null) {
			cached.lastAccessMillis = now;
			return cached.drop;
		}
		List<File> files = indexedDropFiles.get(npcId);
		if (files == null) {
			return null;
		}
		NpcDrop loaded = loadDrop(npcId, files);
		if (loaded != null && maxCachedDrops > 0) {
			cachedDrops.put(npcId, new CacheEntry(loaded, now));
			evictOversizedCache();
		}
		return loaded;
	}

	public synchronized void cleanupExpiredDrops() {
		if (isLazy()) {
			cleanupExpiredDrops(currentTimeMillis.getAsLong());
		}
	}

	public synchronized void reload() {
		if (!isLazy()) {
			return;
		}
		indexedDropFiles = indexDropFiles(npcDropsDirectory);
		cachedDrops.clear();
		log.info("Reloaded {} lazy NPC drop templates from {}", indexedDropFiles.size(), npcDropsDirectory.getPath());
	}

	public synchronized int cachedDropCount() {
		return isLazy() ? cachedDrops.size() : getNpcDrop().size();
	}

	public boolean isLazy() {
		return indexedDropFiles != null;
	}

	private void cleanupExpiredDrops(long now) {
		if (expireAfterAccessMillis < 0) {
			return;
		}
		for (Iterator<Map.Entry<Integer, CacheEntry>> iterator = cachedDrops.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<Integer, CacheEntry> entry = iterator.next();
			if (now - entry.getValue().lastAccessMillis >= expireAfterAccessMillis) {
				iterator.remove();
			}
		}
	}

	private void evictOversizedCache() {
		while (cachedDrops.size() > maxCachedDrops) {
			Iterator<Integer> iterator = cachedDrops.keySet().iterator();
			if (!iterator.hasNext()) {
				return;
			}
			iterator.next();
			iterator.remove();
		}
	}

	private NpcDrop loadDrop(int npcId, List<File> files) {
		NpcDrop mergedDrop = null;
		for (File file : files) {
			for (NpcDrop drop : loadDropsFromFile(npcId, file)) {
				mergedDrop = mergeDrop(mergedDrop, drop);
			}
		}
		return mergedDrop;
	}

	private List<NpcDrop> loadDropsFromFile(int npcId, File file) {
		List<NpcDrop> drops = new ArrayList<>();
		XMLInputFactory inputFactory = newXmlInputFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT && "npc_drop".equals(reader.getLocalName())
						&& npcId == Integer.parseInt(reader.getAttributeValue(null, "npc_id"))) {
						JAXBElement<NpcDrop> element = npcDropContext.createUnmarshaller().unmarshal(reader, NpcDrop.class);
						drops.add(element.getValue());
					}
				}
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to lazy load npc_drop " + npcId + " from " + file.getPath(), e);
		}
		return drops;
	}

	private static NpcDrop mergeDrop(NpcDrop currentDrop, NpcDrop drop) {
		if (currentDrop == null) {
			return drop;
		}
		List<DropGroup> currentGroups = currentDrop.getDropGroup();
		List<DropGroup> newGroups = drop.getDropGroup();
		if (currentGroups.isEmpty() && !newGroups.isEmpty()) {
			return drop;
		}
		Set<Integer> newItemIds = newGroups.stream()
			.map(DropGroup::getDrop)
			.filter(drops -> drops != null)
			.flatMap(List::stream)
			.map(Drop::getItemId)
			.collect(Collectors.toSet());
		for (DropGroup currentGroup : currentGroups) {
			List<Drop> currentDrops = currentGroup.getDrop();
			if (currentDrops != null) {
				currentDrops.removeIf(existingDrop -> newItemIds.contains(existingDrop.getItemId()));
			}
		}
		List<DropGroup> groupsToAdd = new ArrayList<>();
		for (DropGroup newGroup : newGroups) {
			boolean added = false;
			for (DropGroup currentGroup : currentGroups) {
				if (currentGroup.getGroupName().equals(newGroup.getGroupName())) {
					List<Drop> currentDrops = currentGroup.getDrop();
					List<Drop> newDrops = newGroup.getDrop();
					if (currentDrops != null && newDrops != null) {
						currentDrops.addAll(newDrops);
					}
					added = true;
				}
			}
			if (!added) {
				groupsToAdd.add(newGroup);
			}
		}
		if (!groupsToAdd.isEmpty()) {
			currentGroups.addAll(groupsToAdd);
		}
		return currentDrop;
	}

	private static Map<Integer, List<File>> indexDropFiles(File npcDropsDirectory) {
		if (npcDropsDirectory == null || !npcDropsDirectory.isDirectory()) {
			throw new IllegalStateException("NPC drop directory not found: " + npcDropsDirectory);
		}
		Map<Integer, List<File>> index = new HashMap<>();
		for (File file : listXmlFiles(npcDropsDirectory.toPath())) {
			indexFile(file, index);
		}
		Map<Integer, List<File>> immutableIndex = new HashMap<>();
		index.forEach((npcId, files) -> immutableIndex.put(npcId, Collections.unmodifiableList(files)));
		return Collections.unmodifiableMap(immutableIndex);
	}

	private static List<File> listXmlFiles(Path root) {
		try (Stream<Path> paths = Files.walk(root)) {
			return paths
				.filter(Files::isRegularFile)
				.filter(NpcDropData::isVisible)
				.filter(path -> path.getFileName().toString().endsWith(".xml"))
				.filter(path -> !path.getFileName().toString().startsWith("new"))
				.sorted()
				.map(Path::toFile)
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to list NPC drop files from " + root, e);
		}
	}

	private static boolean isVisible(Path path) {
		try {
			return !Files.isHidden(path);
		} catch (IOException e) {
			return true;
		}
	}

	private static void indexFile(File file, Map<Integer, List<File>> index) {
		XMLInputFactory inputFactory = newXmlInputFactory();
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT && "npc_drop".equals(reader.getLocalName())) {
						int npcId = Integer.parseInt(reader.getAttributeValue(null, "npc_id"));
						List<File> files = index.computeIfAbsent(npcId, ignored -> new ArrayList<>());
						if (files.isEmpty() || !files.get(files.size() - 1).equals(file)) {
							files.add(file);
						}
					}
				}
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to index NPC drops from " + file.getPath(), e);
		}
	}

	private static XMLInputFactory newXmlInputFactory() {
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		disableXmlInputProperty(inputFactory, XMLInputFactory.SUPPORT_DTD);
		disableXmlInputProperty(inputFactory, "javax.xml.stream.isSupportingExternalEntities");
		return inputFactory;
	}

	private static void disableXmlInputProperty(XMLInputFactory inputFactory, String property) {
		try {
			inputFactory.setProperty(property, false);
		} catch (IllegalArgumentException ignored) {
		}
	}

	private static JAXBContext createNpcDropContext() {
		try {
			return JAXBContext.newInstance(NpcDrop.class);
		} catch (JAXBException e) {
			throw new IllegalStateException("Failed to create NPC drop JAXB context", e);
		}
	}

	private static final class CacheEntry {
		private final NpcDrop drop;
		private long lastAccessMillis;

		private CacheEntry(NpcDrop drop, long lastAccessMillis) {
			this.drop = drop;
			this.lastAccessMillis = lastAccessMillis;
		}
	}
}
