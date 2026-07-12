package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.WarehouseExpandTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 仓库扩容 NPC 静态数据容器，按 NPC ID 索引扩容模板。
 * Warehouse expander static-data holder, indexing expand templates by NPC id.
 *
 * @author spufy
 */
@XmlRootElement(name = "warehouse_expander")
@XmlAccessorType(XmlAccessType.FIELD)
public class WarehouseExpandData {

	@XmlElement(name = "warehouse_npc")
	private List<WarehouseExpandTemplate> clist;
	private IntObjectHashMap<WarehouseExpandTemplate> npctlistData = new IntObjectHashMap<WarehouseExpandTemplate>();

	/**
	 * JAXB 反序列化完成后，将扩容模板按 NPC ID 索引。
	 * After JAXB unmarshalling, indexes expand templates by NPC id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (WarehouseExpandTemplate npc : clist) {
			npctlistData.put(npc.getNpcId(), npc);
		}
	}

	/**
	 * 返回已加载的扩容模板数量。
	 * Returns the number of loaded expand templates.
	 *
	 * template count
	 */
	public int size() {
		return npctlistData.size();
	}

	/**
	 * 按 NPC ID 获取仓库扩容模板。
	 * Returns the warehouse expand template for the given NPC id.
	 *
	 * NPC id
	 *
	 * @param id
	 * @return 扩容模板，不存在则为 null / expand template or null
	 */
	public WarehouseExpandTemplate getWarehouseExpandListTemplate(int id) {
		return npctlistData.get(id);
	}
}
