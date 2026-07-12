package com.aionemu.gameserver.dataholders;


import com.aionemu.boot.i18n.I18n;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemMask;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.restriction.ItemCleanupTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 物品模板数据容器，持有并索引全部 {@link ItemTemplate}，支持热重载与限制清理。
 * Item template data holder, indexing all {@link ItemTemplate} instances with reload and restriction cleanup support.
 *
 * @author Luno
 */
@XmlRootElement(name = "item_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemData extends ReloadableData {

	@XmlElement(name = "item_template")
	private List<ItemTemplate> its;

	@XmlTransient
	private IntObjectHashMap<ItemTemplate> items;

	@XmlTransient
	private IntObjectHashMap<ItemTemplate> petEggs = new IntObjectHashMap<ItemTemplate>();

	@XmlTransient
	Map<Integer, List<ItemTemplate>> manastones = new HashMap<Integer, List<ItemTemplate>>();

	@XmlTransient
	Map<Integer, ItemTemplate> allItems;

	/**
	 * JAXB 反序列化完成后，按模板 ID 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes templates by id and clears the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		items = new IntObjectHashMap<ItemTemplate>();
		allItems = new HashMap<Integer, ItemTemplate>();
		for (ItemTemplate it : its) {
			items.put(it.getTemplateId(), it);
			allItems.put(it.getTemplateId(), it);
			// if (it.getCategory().equals(ItemCategory.MANASTONE)) {
			// int level = it.getLevel();
			// if (!manastones.containsKey(level)) {
			// manastones.put(level, new ArrayList<ItemTemplate>());
			// }
			// manastones.get(level).add(it);
			// }
			if (it.getActions() == null) {
				continue;
			}
		}
		its = null;
	}

	/**
	 * 根据清理规则覆盖物品的交易 / 出售 / 仓库存取掩码。
	 * sell / warehouse storage masks.
	 */
	public void cleanup() {
		for (ItemCleanupTemplate ict : DataManager.ITEM_CLEAN_UP.getList()) {
			ItemTemplate template = items.get(ict.getId());
			applyCleanup(template, ict.resultTrade(), ItemMask.TRADEABLE);
			applyCleanup(template, ict.resultSell(), ItemMask.SELLABLE);
			applyCleanup(template, ict.resultWH(), ItemMask.STORABLE_IN_WH);
			applyCleanup(template, ict.resultAccountWH(), ItemMask.STORABLE_IN_AWH);
			applyCleanup(template, ict.resultLegionWH(), ItemMask.STORABLE_IN_LWH);
		}
	}

	private void applyCleanup(ItemTemplate item, byte result, int mask) {
		if (result != -1) {
			switch (result) {
			case 1:
				item.modifyMask(true, mask);
				break;
			case 0:
				item.modifyMask(false, mask);
				break;
			}
		}
	}

	/**
	 * 按物品 ID 获取物品模板。
	 * Returns the item template for the given item id.
	 *
	 * item id
	 *
	 * @param itemId
	 * @return 物品模板或 null / item template or null
	 */
	public ItemTemplate getItemTemplate(int itemId) {
		return items.get(itemId);
	}

	/**
	 * 返回全部物品模板映射。
	 * Returns the full item template map.
	 *
	 * @return ID 到物品模板的映射 / map of id to item template
	 */
	public Map<Integer, ItemTemplate> getAllItems() {
		return allItems;
	}

	/**
	 * 返回已加载的物品模板数量。
	 * Returns the number of loaded item templates.
	 *
	 * template count
	 */
	public int size() {
		return items.size();
	}

	/**
	 * 返回按等级分组的魔石模板映射。
	 * Returns manastone templates grouped by level.
	 *
	 * @return 等级到魔石列表的映射 / map of level to manastone list
	 */
	public Map<Integer, List<ItemTemplate>> getManastones() {
		return manastones;
	}

	/**
	 * 按宠物 ID 获取宠物蛋模板。
	 * Returns the pet-egg template for the given pet id.
	 *
	 * pet id
	 *
	 * @param petId
	 * @return 宠物蛋模板或 null / pet-egg template or null
	 */
	public ItemTemplate getPetEggTemplate(int petId) {
		return petEggs.get(petId);
	}

	/**
	 * 热重载物品模板 XML 并通知管理员。
	 * Hot-reloads item templates from XML and notifies the admin.
	 *
	 * @param admin 触发重载的管理员 / admin who triggered the reload
	 */
	@Override
	public void reload(Player admin) {
		try {
			JAXBContext jc = JAXBContext.newInstance(StaticData.class);
			Unmarshaller un = jc.createUnmarshaller();
			un.setSchema(getSchema("./data/static_data/static_data.xsd"));
			List<ItemTemplate> newTemplates = new ArrayList<ItemTemplate>();
			ItemData data = (ItemData) un.unmarshal(Config.dataFile("./data/static_data/items/item_templates.xml"));
			if (data != null && data.getData() != null) {
				newTemplates.addAll(data.getData());
			}
			DataManager.ITEM_DATA.setData(newTemplates);
		} catch (Exception e) {
			PacketSendUtility.sendMessage(admin, "Item templates reload failed!");
			log.error(I18n.get("log.b18c257924d5", e));
		} finally {
			PacketSendUtility.sendMessage(admin,
					"Item templates reload Success! Total loaded: " + DataManager.ITEM_DATA.size());
		}
	}

	@Override
	protected List<ItemTemplate> getData() {
		return its;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setData(List<?> data) {
		this.its = (List<ItemTemplate>) data;
		this.afterUnmarshal(null, null);
	}

	/**
	 * 按描述字符串查找匹配的物品描述（忽略大小写）。
	 * Finds a matching item description by string (case-insensitive).
	 *
	 * @param descr 物品描述 / item description
	 * @return 匹配的描述，未找到则为空串 / matched description or empty string
	 */
	public String getItemDescr(String descr) {
		for (ItemTemplate it : items.values()) {
			if (descr.equalsIgnoreCase(it.getDescr())) {
				return it.getDescr();
			}
		}
		return "";
	}

	/**
	 * 按描述字符串返回对应物品 ID。
	 * Returns the item id matching the given description.
	 *
	 * @param descr 物品描述 / item description
	 * @return 物品 ID，未找到则为 0 / item id or 0
	 */
	public int giveItemIdOf(String descr) {
		for (ItemTemplate it : items.values()) {
			if (descr.equalsIgnoreCase(it.getDescr())) {
				return it.getTemplateId();
			}
		}
		return 0;
	}

	/**
	 * 返回内部物品模板哈希表。
	 * Returns the internal item template hash map.
	 *
	 * @return 物品模板映射 / item template map
	 */
	public IntObjectHashMap<ItemTemplate> getItemData() {
		return items;
	}
}
