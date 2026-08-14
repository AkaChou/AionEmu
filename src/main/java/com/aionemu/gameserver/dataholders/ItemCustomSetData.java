package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.item.ItemCustomSetTeamplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 物品自定义套装数据容器，按套装 ID 索引 {@link ItemCustomSetTeamplate}。
 * Item custom-set data holder, indexing {@link ItemCustomSetTeamplate} by set id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "item_custom_sets")
public class ItemCustomSetData {
	@XmlElement(name = "item_custom_set", required = true)
	protected List<ItemCustomSetTeamplate> customTemplates;

	@XmlTransient
	private IntObjectHashMap<ItemCustomSetTeamplate> custom = new IntObjectHashMap<ItemCustomSetTeamplate>();

	/**
	 * 按 ID 获取物品自定义套装模板。
	 * Returns the item custom-set template for the given id.
	 *
	 * @param id 套装 ID / set id
	 * @return 自定义套装模板或 null / custom-set template or null
	 */
	public ItemCustomSetTeamplate getCustomTemplate(int id) {
		return custom.get(id);
	}

	/**
	 * JAXB 反序列化完成后，按 ID 建立自定义套装索引。
	 * After JAXB unmarshalling, indexes custom-set templates by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (ItemCustomSetTeamplate it : customTemplates) {
			getCustomMap().put(it.getId(), it);
		}
	}

	private IntObjectHashMap<ItemCustomSetTeamplate> getCustomMap() {
		return custom;
	}

	/**
	 * 返回已加载的自定义套装数量。
	 * Returns the number of loaded custom sets.
	 *
	 * @return 已加载的自定义套装数量 / Returns the number of loaded custom sets.
	 */
	public int size() {
		return custom.size();
	}
}
