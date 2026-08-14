package com.aionemu.gameserver.dataholders;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.cosmeticitems.CosmeticItemTemplate;

import java.util.LinkedHashMap;

/**
 * 外观物品数据容器，按外观名称索引模板。
 * Cosmetic item data holder, indexing templates by cosmetic name.
 *
 * @author xTz
 */
@XmlRootElement(name = "cosmetic_items")
@XmlAccessorType(XmlAccessType.FIELD)
public class CosmeticItemsData {
	@XmlElement(name = "cosmetic_item", type = CosmeticItemTemplate.class)
	private List<CosmeticItemTemplate> templates;
	private final Map<String, CosmeticItemTemplate> cosmeticItemTemplates = new LinkedHashMap<String, CosmeticItemTemplate>();

	/**
	 * JAXB 反序列化完成后，按外观名称建立索引并释放列表。
	 * After JAXB unmarshalling, indexes templates by cosmetic name and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (CosmeticItemTemplate template : templates) {
			cosmeticItemTemplates.put(template.getCosmeticName(), template);
		}
		templates.clear();
		templates = null;
	}

	/**
	 * 返回已加载的外观物品数量。
	 * Returns the number of loaded cosmetic items.
	 *
	 * @return 已加载的外观物品数量 / Returns the number of loaded cosmetic items.
	 */
	public int size() {
		return cosmeticItemTemplates.size();
	}

	/**
	 * 按外观名称获取模板。
	 * Returns the cosmetic item template for the given name.
	 *
	 * @param str 外观名称 / cosmetic name
	 * @return 模板，不存在则为 null / template or null
	 */
	public CosmeticItemTemplate getCosmeticItemsTemplate(String str) {
		return cosmeticItemTemplates.get(str);
	}
}
