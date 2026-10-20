package com.aionemu.gameserver.dataholders;

import java.io.BufferedInputStream;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlTransient;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;
import com.aionemu.gameserver.model.Race;

/**
 * NPC 掉落数据容器，支持公共掉落组展开、分片合并与按 NPC ID 索引。
 * NPC drop data holder supporting common drop-group expansion, multi-file merge and indexing by npc id.
 * @author MrPoke
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
	private Map<String, DropGroup> commonDropGroups;
	@XmlTransient
	private Map<CommonDropKey, DropGroup> expandedCommonDropGroups;
	@XmlTransient
	private Map<Integer, NpcDrop> dropsByNpcId = Collections.emptyMap();

	public NpcDropData() {
	}

	/**
	 * 从目录急切加载全部 NPC 掉落分片与公共掉落组。
	 * Eagerly loads all NPC drop part files and common drop groups from the given directory.
	 * @param npcDropsDirectory NPC 掉落数据目录 / npc drops directory
	 * @return 已加载的掉落数据 / loaded drop data
	 */
	public static NpcDropData loadEager(File npcDropsDirectory) {
		if (npcDropsDirectory == null || !npcDropsDirectory.isDirectory()) {
			throw new IllegalStateException("NPC drop directory not found: " + npcDropsDirectory);
		}
		NpcDropData data = new NpcDropData();
		data.commonDropGroups = loadCommonDropGroups(npcDropsDirectory);
		data.expandedCommonDropGroups = new ConcurrentHashMap<>();
		List<File> partFiles = listXmlFiles(npcDropsDirectory.toPath()).stream()
			.filter(file -> file.getName().startsWith("npc_drops_part_"))
			.toList();
		// 分片解析互不依赖，提交分片专用解析池并行执行；mergeDrops/rebuildDropIndex
		// 依赖全部分片结果，保持 join 后在调用线程串行。
		// Shard parses are independent; submit them to the dedicated shard parse pool. mergeDrops and
		// rebuildDropIndex depend on all shard results and stay serial after joining.
		List<List<NpcDrop>> loadedParts = XmlDataLoader.submitLargestFirst(partFiles, data::loadDropsFromFile);
		List<NpcDrop> allDrops = new ArrayList<>();
		for (List<NpcDrop> loadedPart : loadedParts) {
			allDrops.addAll(loadedPart);
		}
		data.npcDrop = mergeDrops(allDrops);
		data.rebuildDropIndex();
		return data;
	}

	/**
	 * 返回全部 NPC 掉落列表。
	 * Returns the full NPC drop list.
	 * @return NPC 掉落列表 / npc drop list
	 */
	public List<NpcDrop> getNpcDrop() {
		return npcDrop == null ? Collections.emptyList() : npcDrop;
	}

	/**
	 * 设置 NPC 掉落列表并重建索引。
	 * Sets the NPC drop list and rebuilds the index.
	 * @param npcDrop NPC 掉落列表 / npc drop list
	 */
	public void setNpcDrop(List<NpcDrop> npcDrop) {
		this.npcDrop = npcDrop;
		rebuildDropIndex();
	}

	/**
	 * JAXB 反序列化完成后，展开公共掉落组、合并重复 NPC 并重建索引。
	 * After JAXB unmarshalling, expands common drop groups, merges duplicate NPCs and rebuilds the index.
	 */
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

	/**
	 * 返回已加载的 NPC 掉落数量。
	 * Returns the number of loaded NPC drops.
	 * @return 掉落条目数量 / drop entry count
	 */
	public int size() {
		return getNpcDrop().size();
	}

	/**
	 * 按 NPC ID 获取掉落配置。
	 * Returns the drop configuration for the given npc id.
	 * @param npcId NPC ID / npc id
	 * @return 掉落配置或 null / drop config or null
	 */
	public synchronized NpcDrop getDrop(int npcId) {
		if (dropsByNpcId == null || dropsByNpcId.isEmpty() && !getNpcDrop().isEmpty()) {
			rebuildDropIndex();
		}
		return dropsByNpcId.get(npcId);
	}

	private List<NpcDrop> loadDropsFromFile(File file) {
		XMLInputFactory factory = createXmlInputFactory();
		try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			List<NpcDrop> drops = new ArrayList<>();
			NpcDrop currentDrop = null;
			DropGroup currentGroup = null;
			List<Drop> currentGroupDrops = null;
			List<DropGroup> currentCommonGroups = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "npc_drop" -> {
							currentDrop = new NpcDrop();
							currentDrop.setNpcId(requiredInt(reader, "npc_id", "npc_drop"));
							currentCommonGroups = new ArrayList<>();
							drops.add(currentDrop);
						}
						case "drop_group" -> {
							if (currentDrop == null) {
								throw new IllegalStateException("drop_group outside npc_drop: " + reader.getLocation());
							}
							currentGroup = new DropGroup();
							applyDropGroupAttributes(reader, currentGroup);
							currentGroupDrops = new ArrayList<>();
						}
						case "drop" -> {
							if (currentGroup == null) {
								throw new IllegalStateException("drop outside drop_group: " + reader.getLocation());
							}
							currentGroupDrops.add(parseDrop(reader));
						}
						case "common_drop_group" -> {
							if (currentDrop == null) {
								throw new IllegalStateException("common_drop_group outside npc_drop: "
									+ reader.getLocation());
							}
							currentCommonGroups.add(expandedCommonDropGroup(
								requiredAttribute(reader, "name", "common_drop_group"),
								attributeInt(reader, "common_drop_adjustment", 100)));
						}
						default -> {
						}
					}
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					switch (reader.getLocalName()) {
						case "drop_group" -> {
							if (currentDrop != null && currentGroup != null) {
								currentGroup.setDrop(currentGroupDrops.isEmpty() ? null : currentGroupDrops);
								currentDrop.addDropGroups(List.of(currentGroup));
							}
							currentGroup = null;
							currentGroupDrops = null;
						}
						case "npc_drop" -> {
							if (currentDrop != null && currentCommonGroups != null) {
								currentDrop.addDropGroups(currentCommonGroups);
							}
							currentDrop = null;
							currentCommonGroups = null;
						}
						default -> {
						}
					}
				}
			}
			reader.close();
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
		List<DropGroup> groups = drop.getCommonDropGroups().stream()
			.map(this::expandedCommonDropGroup)
			.toList();
		drop.addDropGroups(groups);
	}

	private DropGroup expandedCommonDropGroup(NpcDrop.CommonDropGroup reference) {
		return expandedCommonDropGroup(reference.getName(), reference.getCommonDropAdjustment());
	}

	private DropGroup expandedCommonDropGroup(String name, int adjustment) {
		DropGroup group = commonDropGroups.get(name);
		if (group == null) {
			throw new IllegalStateException("Unknown common_drop_group: " + name);
		}
		CommonDropKey key = new CommonDropKey(name, adjustment);
		if (expandedCommonDropGroups == null) {
			return copyWithMultiplier(group, adjustment);
		}
		return expandedCommonDropGroups.computeIfAbsent(key,
			ignored -> copyWithMultiplier(group, adjustment));
	}

	private static DropGroup copyWithMultiplier(DropGroup group, int commonDropAdjustment) {
		DropGroup copy = group.copy();
		copy.setChanceMultiplier(commonDropAdjustment / 100f);
		return copy;
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
		// 公共掉落组按 (名称, 倍率) 在多个 NPC 间共享；合并重复 NPC 前先复制当前组，
		// 否则 removeIf/addAll 会改动其他 NPC 引用的组，甚至对同一列表 addAll 自身。
		// Common groups are shared by (name, multiplier) across NPCs. Copy current groups before
		// merging duplicate NPCs; otherwise removeIf/addAll mutates groups owned by other NPCs,
		// or even adds a list to itself.
		List<DropGroup> mergedGroups = new ArrayList<>(currentGroups.size());
		Map<String, DropGroup> currentGroupsByName = new HashMap<>();
		for (DropGroup currentGroup : currentGroups) {
			DropGroup detachedGroup = currentGroup.copy();
			mergedGroups.add(detachedGroup);
			currentGroupsByName.put(detachedGroup.getGroupName(), detachedGroup);
		}
		Set<Integer> newItemIds = newGroups.stream()
			.map(DropGroup::getDrop)
			.filter(Objects::nonNull)
			.flatMap(List::stream)
			.map(Drop::getItemId)
			.collect(Collectors.toSet());
		for (DropGroup currentGroup : mergedGroups) {
			List<Drop> currentDrops = currentGroup.getDrop();
			if (currentDrops != null) {
				currentDrops.removeIf(existingDrop -> newItemIds.contains(existingDrop.getItemId()));
			}
		}
		for (DropGroup newGroup : newGroups) {
			DropGroup currentGroup = currentGroupsByName.get(newGroup.getGroupName());
			if (currentGroup == null) {
				mergedGroups.add(newGroup.copy());
				continue;
			}
			List<Drop> currentDrops = currentGroup.getDrop();
			List<Drop> newDrops = newGroup.getDrop();
			if (currentDrops != null && newDrops != null) {
				currentDrops.addAll(newDrops);
			}
		}
		if (!mergedGroups.isEmpty()) {
			currentDrop.replaceDropGroups(mergedGroups);
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

	private static Map<String, DropGroup> loadCommonDropGroups(File npcDropsDirectory) {
		File file = new File(npcDropsDirectory, COMMON_DROP_GROUPS_FILE);
		if (!file.isFile()) {
			return Collections.emptyMap();
		}
		XMLInputFactory factory = createXmlInputFactory();
		try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			Map<String, DropGroup> groups = new HashMap<>();
			DropGroup currentGroup = null;
			List<Drop> currentDrops = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "group" -> {
							currentGroup = new DropGroup();
							applyDropGroupAttributes(reader, currentGroup);
							currentDrops = new ArrayList<>();
						}
						case "drop" -> {
							if (currentGroup == null) {
								throw new IllegalStateException("drop outside group: " + reader.getLocation());
							}
							currentDrops.add(parseDrop(reader));
						}
						default -> {
						}
					}
				} else if (event == XMLStreamConstants.END_ELEMENT && "group".equals(reader.getLocalName())) {
					if (currentGroup != null && currentDrops != null) {
						currentGroup.setDrop(currentDrops.isEmpty() ? null : currentDrops);
						groups.put(currentGroup.getGroupName(), currentGroup);
					}
					currentGroup = null;
					currentDrops = null;
				}
			}
			reader.close();
			return groups;
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

	private record CommonDropKey(String name, int adjustment) {
	}

	private static XMLInputFactory createXmlInputFactory() {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		return factory;
	}

	private static void applyDropGroupAttributes(XMLStreamReader reader, DropGroup group) {
		String name = reader.getAttributeValue(null, "name");
		if (name != null) {
			group.setGroup_name(name);
		}
		String race = reader.getAttributeValue(null, "race");
		if (race != null) {
			group.setRace(Race.valueOf(race));
		}
		String useCategory = reader.getAttributeValue(null, "use_category");
		if (useCategory != null) {
			group.setLegacyUseCategory(Boolean.valueOf(useCategory));
		}
		String levelBased = reader.getAttributeValue(null, "level_based_chance_reduction");
		if (levelBased != null) {
			group.setUseLevelBasedChanceReduction(Boolean.valueOf(levelBased));
		}
		String maxItems = reader.getAttributeValue(null, "max_items");
		if (maxItems != null) {
			group.setMaxItems(Integer.parseInt(maxItems));
		}
		String adjustment = reader.getAttributeValue(null, "drop_group_adjustment");
		if (adjustment != null) {
			group.setDropGroupAdjustment(Integer.parseInt(adjustment));
		}
	}

	private static Drop parseDrop(XMLStreamReader reader) {
		int itemId = requiredInt(reader, "item_id", "drop");
		int minAmount = attributeInt(reader, "min_amount", 1);
		int maxAmount = attributeInt(reader, "max_amount", 0);
		Drop drop = new Drop();
		drop.setItemId(itemId);
		drop.setMinAmount(minAmount);
		drop.setMaxAmount(maxAmount == 0 ? minAmount : maxAmount);
		drop.setChance(attributeFloat(reader, "chance", 100));
		drop.setNoReduce(attributeBoolean(reader, "no_reduce", false));
		drop.setEachMember(attributeBoolean(reader, "eachmember", false));
		String eachMember = reader.getAttributeValue(null, "each_member");
		if (eachMember != null) {
			drop.setAionServerEachMember(Boolean.valueOf(eachMember));
		}
		return drop;
	}

	private static int requiredInt(XMLStreamReader reader, String name, String element) {
		String value = requiredAttribute(reader, name, element);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Invalid " + name + " on " + element + ": " + value, e);
		}
	}

	private static String requiredAttribute(XMLStreamReader reader, String name, String element) {
		String value = reader.getAttributeValue(null, name);
		if (value == null || value.isEmpty()) {
			throw new IllegalStateException("Missing " + name + " on " + element + ": " + reader.getLocation());
		}
		return value;
	}

	private static int attributeInt(XMLStreamReader reader, String name, int defaultValue) {
		String value = reader.getAttributeValue(null, name);
		return value == null || value.isEmpty() ? defaultValue : Integer.parseInt(value);
	}

	private static float attributeFloat(XMLStreamReader reader, String name, float defaultValue) {
		String value = reader.getAttributeValue(null, name);
		return value == null || value.isEmpty() ? defaultValue : Float.parseFloat(value);
	}

	private static boolean attributeBoolean(XMLStreamReader reader, String name, boolean defaultValue) {
		String value = reader.getAttributeValue(null, name);
		return value == null || value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
	}
}
