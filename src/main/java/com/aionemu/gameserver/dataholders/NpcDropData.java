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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;

/**
 * @author MrPoke
 *
 */
@XmlRootElement(name = "npc_drops")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "npcDropData", propOrder = { "commonDropGroupDefinitions", "npcDrop" })
public class NpcDropData {

	private static final String COMMON_DROP_GROUPS_FILE = "common_drop_groups.xml";

	@XmlElement(name = "group")
	protected List<DropGroup> commonDropGroupDefinitions;
	@XmlElement(name = "npc_drop")
	protected List<NpcDrop> npcDrop;
	@XmlTransient
	private JAXBContext npcDropContext;
	@XmlTransient
	private Map<String, DropGroup> commonDropGroups;
	@XmlTransient
	private Map<Integer, NpcDrop> dropsByNpcId = Collections.emptyMap();

	public NpcDropData() {
	}

	public static NpcDropData loadEager(File npcDropsDirectory) {
		if (npcDropsDirectory == null || !npcDropsDirectory.isDirectory()) {
			throw new IllegalStateException("NPC drop directory not found: " + npcDropsDirectory);
		}
		NpcDropData data = new NpcDropData();
		data.npcDropContext = createNpcDropContext();
		data.commonDropGroups = loadCommonDropGroups(npcDropsDirectory);
		data.npcDrop = mergeDrops(listXmlFiles(npcDropsDirectory.toPath()).stream()
			.filter(file -> file.getName().startsWith("npc_drops_part_") && !file.getName().equals("npc_drops_part_old.xml"))
			.flatMap(file -> data.loadDropsFromFile(file).stream())
			.collect(Collectors.toList()));
		data.rebuildDropIndex();
		return data;
	}

	/**
	 * @return the npcDrop
	 */
	public List<NpcDrop> getNpcDrop() {
		return npcDrop == null ? Collections.emptyList() : npcDrop;
	}

	/**
	 * @param npcDrop the npcDrop to set
	 */
	public void setNpcDrop(List<NpcDrop> npcDrop) {
		this.npcDrop = npcDrop;
		rebuildDropIndex();
	}

	@SuppressWarnings("unused")
	private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		commonDropGroups = commonDropGroups(commonDropGroupDefinitions);
		if (!commonDropGroups.isEmpty()) {
			for (NpcDrop drop : getNpcDrop()) {
				expandCommonDropGroups(drop);
			}
		}
		npcDrop = mergeDrops(getNpcDrop());
		rebuildDropIndex();
	}

	public int size() {
		return getNpcDrop().size();
	}

	public synchronized NpcDrop getDrop(int npcId) {
		if (dropsByNpcId == null || dropsByNpcId.isEmpty() && !getNpcDrop().isEmpty()) {
			rebuildDropIndex();
		}
		return dropsByNpcId.get(npcId);
	}

	private List<NpcDrop> loadDropsFromFile(File file) {
		try (FileInputStream stream = new FileInputStream(file)) {
			NpcDropData data = (NpcDropData) npcDropContext.createUnmarshaller().unmarshal(stream);
			List<NpcDrop> drops = data.getNpcDrop();
			for (NpcDrop drop : drops) {
				expandCommonDropGroups(drop);
			}
			return drops;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to eager load npc_drops from " + file.getPath(), e);
		}
	}

	private void rebuildDropIndex() {
		Map<Integer, NpcDrop> index = new HashMap<>();
		for (NpcDrop drop : getNpcDrop()) {
			index.put(drop.getNpcId(), drop);
		}
		dropsByNpcId = Collections.unmodifiableMap(index);
	}

	private void expandCommonDropGroups(NpcDrop drop) {
		List<DropGroup> groups = drop.getCommonDropGroupNames().stream()
			.map(name -> {
				DropGroup group = commonDropGroups.get(name);
				if (group == null) {
					throw new IllegalStateException("Unknown common_drop_group: " + name);
				}
				return group.copy();
			})
			.toList();
		drop.addDropGroups(groups);
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
		Map<String, DropGroup> currentGroupsByName = new HashMap<>();
		for (DropGroup currentGroup : currentGroups) {
			currentGroupsByName.put(currentGroup.getGroupName(), currentGroup);
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
			DropGroup currentGroup = currentGroupsByName.get(newGroup.getGroupName());
			if (currentGroup == null) {
				groupsToAdd.add(newGroup);
				continue;
			}
			List<Drop> currentDrops = currentGroup.getDrop();
			List<Drop> newDrops = newGroup.getDrop();
			if (currentDrops != null && newDrops != null) {
				currentDrops.addAll(newDrops);
			}
		}
		if (!groupsToAdd.isEmpty()) {
			currentGroups.addAll(groupsToAdd);
		}
		return currentDrop;
	}

	private static List<NpcDrop> mergeDrops(List<NpcDrop> drops) {
		if (drops.isEmpty()) {
			return drops;
		}
		Map<Integer, NpcDrop> mergedDrops = new LinkedHashMap<>();
		for (NpcDrop drop : drops) {
			mergedDrops.merge(drop.getNpcId(), drop, NpcDropData::mergeDrop);
		}
		return new ArrayList<>(mergedDrops.values());
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

	private static JAXBContext createNpcDropContext() {
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		ClassLoader npcDropClassLoader = NpcDrop.class.getClassLoader();
		try {
			if (npcDropClassLoader != null) {
				thread.setContextClassLoader(npcDropClassLoader);
			}
			return JAXBContext.newInstance(NpcDropData.class);
		} catch (JAXBException e) {
			throw new IllegalStateException("Failed to create NPC drop JAXB context", e);
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}

	private static Map<String, DropGroup> loadCommonDropGroups(File npcDropsDirectory) {
		File file = new File(npcDropsDirectory, COMMON_DROP_GROUPS_FILE);
		if (!file.isFile()) {
			return Collections.emptyMap();
		}
		try (FileInputStream stream = new FileInputStream(file)) {
			CommonDropGroups data = (CommonDropGroups) JAXBContext.newInstance(CommonDropGroups.class)
				.createUnmarshaller()
				.unmarshal(stream);
			return commonDropGroups(data.getGroups());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load common drop groups from " + file.getPath(), e);
		}
	}

	private static Map<String, DropGroup> commonDropGroups(List<DropGroup> definitions) {
		if (definitions == null || definitions.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, DropGroup> groups = new HashMap<>();
		for (DropGroup group : definitions) {
			groups.put(group.getGroupName(), group);
		}
		return groups;
	}

	@XmlRootElement(name = "common_drop_groups")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class CommonDropGroups {
		@XmlElement(name = "group")
		private List<DropGroup> groups;

		private List<DropGroup> getGroups() {
			return groups == null ? Collections.emptyList() : groups;
		}
	}
}
