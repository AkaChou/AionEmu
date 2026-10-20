package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import lombok.Getter;

/**
 * NPC 模板数据容器，持有并索引全部 {@link NpcTemplate}。
 * 每个 {@link Npc} 实例对应某一 NPC 类别，同类 NPC 共享同一 id、名称、物品与属性，
 * 其数据定义于 {@link NpcTemplate} 并以 NPC ID 唯一标识。
 * Container holding and serving all {@link NpcTemplate} instances.
 * Every {@link Npc} instance represents a class of NPCs sharing the same id, name,
 * items and statistics. That class data is defined in {@link NpcTemplate} and uniquely
 * identified by npc id.
 * @author Luno
 */
@Getter
@XmlRootElement(name = "npc_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class NpcData {

	@XmlElement(name = "npc_template")
	private List<NpcTemplate> npcs;

	/** 全部模板的稳定快照；保留名称重复定义供任务索引复用。 / Stable template snapshot; keeps duplicate ids for quest indexes. */
	private List<NpcTemplate> templates = List.of();

	/** 全部 NPC 模板映射 / map containing all npc templates */
	private IntObjectHashMap<NpcTemplate> npcData = new IntObjectHashMap<>();

	/**
	 * 以预期模板数量创建容器，避免合并分片时反复扩容。
	 * Creates the container with the expected template count to avoid repeated growth while
	 * merging shards.
	 * @param expectedSize 预期模板数量 / expected template count
	 */
	public NpcData(int expectedSize) {
		npcData = new IntObjectHashMap<>(expectedSize);
	}

	/**
	 * 创建默认容量容器。
	 * Creates a container with the default capacity.
	 */
	public NpcData() {
	}

	/**
	 * JAXB 反序列化完成后，按模板 ID 建立索引并释放列表引用。
	 * After JAXB unmarshalling, indexes templates by id and releases the list reference.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		List<NpcTemplate> templates = npcs == null ? List.of() : npcs;
		this.templates = List.copyOf(templates);
		npcData = new IntObjectHashMap<>(templates.size());
		for (NpcTemplate npc : templates) {
			npcData.put(npc.getTemplateId(), npc);
		}
		npcs = null;
	}

	/**
	 * 保留合并分片中的全部模板定义，供任务名称索引复用而不重复扫描 XML。
	 * Retains every merged shard template so quest name indexes can reuse it without rescanning XML.
	 *
	 * @param templates 模板定义 / template definitions
	 */
	public void retainTemplates(List<NpcTemplate> templates) {
		this.templates = List.copyOf(templates);
	}

	/**
	 * 返回已加载的 NPC 模板数量。
	 * Returns the number of loaded NPC templates.
	 * @return 已加载的NPC 模板数量 / Returns the number of loaded NPC templates.
	 */
	public int size() {
		return npcData.size();
	}

	/**
	 * 按 ID 返回 NPC 模板。
	 * Returns the {@link NpcTemplate} for the given id.
	 * @param id NPC 模板 ID / npc template id
	 * @return 模板或 null / template or null
	 */
	public NpcTemplate getNpcTemplate(int id) {
		return npcData.get(id);
	}
}
