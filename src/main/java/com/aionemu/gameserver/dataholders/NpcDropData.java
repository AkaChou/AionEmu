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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.NpcDrop;

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
	private JAXBContext npcDropContext;
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
		data.npcDropContext = createJaxbContext(NpcDropShard.class);
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
		try (FileInputStream stream = new FileInputStream(file)) {
			// 分片只绑定根下的 npc_drop，避免 NpcDropData.afterUnmarshal 对每个分片重复
			// mergeDrops/rebuildDropIndex；最终索引由 loadEager 在全部分片合并后建立一次。
			// Bind only the npc_drop entries from each shard so NpcDropData.afterUnmarshal does not run
			// per shard; loadEager builds the final merged index exactly once.
			NpcDropShard shard = (NpcDropShard) npcDropContext.createUnmarshaller().unmarshal(stream);
			List<NpcDrop> drops = shard.getDrops();
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
		List<DropGroup> groups = drop.getCommonDropGroups().stream()
			.map(this::expandedCommonDropGroup)
			.toList();
		drop.addDropGroups(groups);
	}

	private DropGroup expandedCommonDropGroup(NpcDrop.CommonDropGroup reference) {
		DropGroup group = commonDropGroups.get(reference.getName());
		if (group == null) {
			throw new IllegalStateException("Unknown common_drop_group: " + reference.getName());
		}
		CommonDropKey key = new CommonDropKey(reference.getName(), reference.getCommonDropAdjustment());
		if (expandedCommonDropGroups == null) {
			return copyWithMultiplier(group, reference.getCommonDropAdjustment());
		}
		return expandedCommonDropGroups.computeIfAbsent(key,
			ignored -> copyWithMultiplier(group, reference.getCommonDropAdjustment()));
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

	private static JAXBContext createJaxbContext(Class<?> type) {
		try {
			return XmlDataLoader.createJaxbContext(type);
		} catch (JAXBException e) {
			throw new IllegalStateException("Failed to create NPC drop JAXB context for " + type.getName(), e);
		}
	}

	private static Map<String, DropGroup> loadCommonDropGroups(File npcDropsDirectory) {
		File file = new File(npcDropsDirectory, COMMON_DROP_GROUPS_FILE);
		if (!file.isFile()) {
			return Collections.emptyMap();
		}
		try (FileInputStream stream = new FileInputStream(file)) {
			// 必须经由 createJaxbContext：本方法可能运行在 commonPool 线程，裸调用会因线程上下文
			// 类加载器找不到 JAXB runtime 而失败。Must go through createJaxbContext: this can run on
			// commonPool threads where a raw call fails because the thread CCL cannot see the JAXB runtime.
			CommonDropGroups data = (CommonDropGroups) XmlDataLoader.createJaxbContext(CommonDropGroups.class)
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

	/**
	 * 单个掉落分片的 JAXB 包装：只收集根下的 {@code npc_drop}，不参与局部合并与索引。
	 * JAXB wrapper for one drop shard: collects only root-level {@code npc_drop} entries without
	 * per-shard merge or indexing.
	 */
	@XmlRootElement(name = "npc_drops")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class NpcDropShard {

		@XmlElement(name = "npc_drop")
		private List<NpcDrop> drops;

		private List<NpcDrop> getDrops() {
			return drops == null ? Collections.emptyList() : drops;
		}
	}

	private record CommonDropKey(String name, int adjustment) {
	}
}
