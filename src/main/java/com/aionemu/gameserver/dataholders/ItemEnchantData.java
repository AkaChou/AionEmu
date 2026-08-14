package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.item.EnchantType;
import com.aionemu.gameserver.model.templates.item.ItemEnchantTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 物品强化 / 授权模板数据容器，按类型与 ID 索引 {@link ItemEnchantTemplate}。
 * authorize template data holder, indexing {@link ItemEnchantTemplate} by type and id.
 *
 * @author Ranastic (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "enchant_templates")
public class ItemEnchantData {

	@XmlElement(name = "enchant_template", required = true)
	protected List<ItemEnchantTemplate> enchantTemplates;

	@XmlTransient
	private IntObjectHashMap<ItemEnchantTemplate> enchants = new IntObjectHashMap<ItemEnchantTemplate>();

	@XmlTransient
	private IntObjectHashMap<ItemEnchantTemplate> authorizes = new IntObjectHashMap<ItemEnchantTemplate>();

	/**
	 * JAXB 反序列化完成后，按强化类型将模板写入对应映射。
	 * After JAXB unmarshalling, stores templates into the map matching their enchant type.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (ItemEnchantTemplate it : enchantTemplates) {
			getEnchantMap(it.getEnchantType()).put(it.getId(), it);
		}
	}

	private IntObjectHashMap<ItemEnchantTemplate> getEnchantMap(EnchantType type) {
		if (type == EnchantType.ENCHANT) {
			return enchants;
		}
		return authorizes;
	}

	/**
	 * 按强化类型与 ID 获取强化模板。
	 * Returns the enchant template for the given type and id.
	 *
	 * @param type 强化类型 / enchant type
	 * @param id 模板 ID / template id
	 * @return 强化模板或 null / enchant template or null
	 */
	public ItemEnchantTemplate getEnchantTemplate(EnchantType type, int id) {
		if (type == EnchantType.ENCHANT) {
			return enchants.get(id);
		}
		return authorizes.get(id);
	}

	/**
	 * 返回强化与授权模板的总数量。
	 * Returns the total number of enchant and authorize templates.
	 *
	 * @return 模板总数 / total template count
	 */
	public int size() {
		return enchants.size() + authorizes.size();
	}
}
